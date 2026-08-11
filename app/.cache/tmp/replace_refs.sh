#!/bin/bash
# Single-pass perl replacement of renamed resource/id references.
set -e
cd /data/data/com.termux/files/home/su/app
T=.cache/tmp

: > $T/repl_xml.txt
: > $T/repl_code.txt

# id references
while read -r old new; do
  [ "$old" = "$new" ] && continue
  echo "@+id/${old}|@+id/${new}" >> $T/repl_xml.txt
  echo "@id/${old}|@id/${new}" >> $T/repl_xml.txt
  echo "<item name=\"${old}\" type=\"id\"|<item name=\"${new}\" type=\"id\"" >> $T/repl_xml.txt
  echo "R.id.${old}|R.id.${new}" >> $T/repl_code.txt
done < $T/ids_map.txt

# resource references per type
while read -r old new t; do
  [ "$old" = "$new" ] && continue
  echo "@${t}/${old}|@${t}/${new}" >> $T/repl_xml.txt
  echo "R.${t}.${old}|R.${t}.${new}" >> $T/repl_code.txt
done < $T/files_uniq.txt

# value-name aliases
echo '<drawable name="ic_launcher">|<drawable name="LauncherIcon">' >> $T/repl_xml.txt
echo '<color name="ic_launcher_background">|<color name="LauncherIconBackground">' >> $T/repl_xml.txt

build_perl() {
  local infile="$1"
  perl -e '
    my %m;
    while (<>) { chomp; my ($k,$v) = split(/\|/,$_,2); $m{$k} = $v; }
    my @keys = sort { length($b) <=> length($a) } keys %m;
    my $re = join("|", map { quotemeta($_) } @keys);
    # emit perl script to STDOUT
    print "my \$re = qr/^" . $re . "$/;\n";
  ' "$infile" > /dev/null
  # simpler: write the alternation into a file
}

# Generate a perl program that does all replacements with a single alternation.
gen_prog() {
  local infile="$1" out="$2"
  {
    echo "use strict; use warnings;"
    echo "my %m = ("
    while IFS='|' read -r k v; do
      printf "  %s => %s,\n" "$(printf '%s' "$k" | sed 's/\\/\\\\/g; s/'"'"'/\\'"'"'/g' | awk '{printf "\047%s\047", $0}')" "$(printf '%s' "$v" | sed 's/\\/\\\\/g; s/'"'"'/\\'"'"'/g' | awk '{printf "\047%s\047", $0}')"
    done < "$infile"
    echo ");"
    echo "my \$re = join('|', map { quotemeta(\$_) } sort { length(\$b) <=> length(\$a) } keys %m);"
    echo 'while (<>) { s/($re)/$m{$1}/g; print; }'
  } > "$out"
}

RESXML=$(find apk/src/main/res core/src/main/res materials/src/main/res stub/src/main/res -name "*.xml" 2>/dev/null)
for f in apk/src/main/AndroidManifest.xml core/src/main/AndroidManifest.xml stub/src/main/AndroidManifest.xml materials/src/main/AndroidManifest.xml; do
  [ -f "$f" ] && RESXML="$RESXML $f"
done
CODE=$(find apk/src/main core/src/main stub/src/main -name "*.kt" -o -name "*.java" 2>/dev/null | grep -v "/build/")

gen_prog $T/repl_xml.txt $T/repl_xml.pl
gen_prog $T/repl_code.txt $T/repl_code.pl

echo "applying xml replacements to $(echo $RESXML | wc -w) files..."
for f in $RESXML; do perl $T/repl_xml.pl "$f" > "$f.tmp" && mv "$f.tmp" "$f"; done

echo "applying code replacements to $(echo $CODE | wc -w) files..."
for f in $CODE; do perl $T/repl_code.pl "$f" > "$f.tmp" && mv "$f.tmp" "$f"; done

echo "constraint_referenced_ids..."
perl -i -pe '
  if (/tools:constraint_referenced_ids="([^"]*)"/) {
    my $v = $1;
    my %m = qw( home_system_icon HomeSystemIcon home_system_title HomeSystemTitle home_system_button HomeSystemButton
                home_manager_icon HomeManagerIcon home_manager_title HomeManagerTitle home_manager_button HomeManagerButton
                module_update ModuleUpdate module_remove ModuleRemove );
    $v =~ s/\b(\Q$_\E)\b/$m{$_}/g for keys %m;
    s/(tools:constraint_referenced_ids=")[^"]*/$1$v/;
  }
' apk/src/main/res/layout/IncludeHomeSystem.xml apk/src/main/res/layout/IncludeHomeManager.xml apk/src/main/res/layout/ItemModule.xml

echo "DONE"

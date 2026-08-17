package android.sum

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import android.widget.TextView

class DebugActivity : Activity() {

    private val exceptionMap = mapOf(
        "StringIndexOutOfBoundsException" to "Invalid string operation\n",
        "IndexOutOfBoundsException" to "Invalid list operation\n",
        "ArithmeticException" to "Invalid arithmetical operation\n",
        "NumberFormatException" to "Invalid toNumber block operation\n",
        "ActivityNotFoundException" to "Invalid intent operation\n"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val formattedMessage = SpannableStringBuilder()
        val errorMessage = intent?.getStringExtra("error") ?: ""

        if (errorMessage.isNotEmpty()) {
            val split = errorMessage.split("\n")
            val exceptionType = if (split.isNotEmpty()) split[0] else ""
            val message = exceptionMap[exceptionType] ?: ""

            if (message.isNotEmpty()) {
                formattedMessage.append(message)
            }

            for (i in 1 until split.size) {
                formattedMessage.append(split[i])
                formattedMessage.append("\n")
            }
        } else {
            formattedMessage.append("No error message available.")
        }

        title = "$title Crashed"

        val errorView = TextView(this).apply {
            text = formattedMessage
            setTextIsSelectable(true)
        }

        val hscroll = HorizontalScrollView(this)
        val vscroll = ScrollView(this)

        hscroll.addView(vscroll)
        vscroll.addView(errorView)

        setContentView(hscroll)
    }
}

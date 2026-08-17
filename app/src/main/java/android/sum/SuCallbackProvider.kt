package android.sum

import android.os.Bundle
import android.sum.BaseProvider
import android.sum.CallbackHandler

class SuCallbackProvider : BaseProvider() {

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return when (method) {
            CallbackHandler.LOG, CallbackHandler.NOTIFY -> {
                CallbackHandler.run(context!!, method, extras)
                Bundle.EMPTY
            }
            else -> Bundle.EMPTY
        }
    }
}

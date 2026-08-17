package android.sum

import android.os.Bundle
import android.sum.base.BaseProvider
import android.sum.handler.CallbackHandler

class Provider : BaseProvider() {

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

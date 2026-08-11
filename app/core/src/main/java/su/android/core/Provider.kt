package su.android.core

import android.os.Bundle
import su.android.core.base.BaseProvider
import su.android.core.handler.CallbackHandler

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

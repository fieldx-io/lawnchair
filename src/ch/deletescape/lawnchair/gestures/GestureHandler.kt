package ch.deletescape.lawnchair.gestures

import android.content.Context
import android.content.Intent
import com.android.launcher3.R
import org.json.JSONObject

abstract class GestureHandler(val context: Context, val config: JSONObject?) {

    abstract val displayName: String
    open val hasConfig = false
    open val configIntent: Intent? = null

    abstract fun onGestureTrigger(controller: GestureController)

    protected open fun saveConfig(config: JSONObject) {

    }

    open fun onConfigResult(data: Intent?) {

    }

    open fun onDestroy() {

    }

    override fun toString(): String {
        return JSONObject().apply {
            put("class", this@GestureHandler::class.java.name)
            if (hasConfig) {
                val config = JSONObject()
                saveConfig(config)
                put("config", config)
            }
        }.toString()
    }
}

class BlankGestureHandler(context: Context, config: JSONObject?) : GestureHandler(context, config) {

    override val displayName = context.getString(R.string.action_none)!!

    override fun onGestureTrigger(controller: GestureController) {

    }
}
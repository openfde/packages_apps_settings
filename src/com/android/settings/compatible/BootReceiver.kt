package com.android.settings.compatible;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.util.Log;
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.InputStream
import com.android.settings.R;
import com.android.settings.location.fde.LocationUtils;

class BootReceiver : BroadcastReceiver() {
    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.w(TAG, "onReceive()------ action: $action")
        GlobalScope.launch {
            val inputStream: InputStream = context.getResources().openRawResource(R.raw.comp_config_list)
            CompUtils.parseList(context, inputStream)
            LocationUtils.parseGpsData(context)
        }
    }
}

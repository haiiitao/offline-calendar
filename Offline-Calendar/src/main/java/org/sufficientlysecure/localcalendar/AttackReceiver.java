package org.sufficientlysecure.localcalendar;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

public class AttackReceiver extends BroadcastReceiver {
    public static Intent in;

    public void onReceive(Context context, Intent intent) {
        Log.i("Attacker", "received intent action: " + intent.getAction() + "\ncontext: " + context.getPackageCodePath() + "\nOver");
        in = intent;
    }
}
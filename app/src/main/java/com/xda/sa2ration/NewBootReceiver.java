package com.xda.sa2ration;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java8.util.Optional;

public class NewBootReceiver extends BroadcastReceiver {

    /**
     * Receives new boot event. Applies persisted saturation and cm values, if present.
     * @param context context passed
     * @param intent intent passed
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Establecer saturacion guardada
            Optional<String> saturation = PersistenceController.getInstance(context).restoreFromProperties(MainActivity.keys.SATURATION.name());
            Optional<String> cm = PersistenceController.getInstance(context).restoreFromProperties( MainActivity.keys.CM.name());
            saturation.ifPresent(s -> CommandController.execCommand("setprop " + MainActivity.PERSISTENT_COLOR_SATURATION
                    + " " + s, "service call SurfaceFlinger 1022 f " + s));
            cm.ifPresent(c ->  CommandController.execCommand("service call SurfaceFlinger 1023 i32 " + c ));
        }
    }

}

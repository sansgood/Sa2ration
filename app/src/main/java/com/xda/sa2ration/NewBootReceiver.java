package com.xda.sa2ration;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java8.util.Optional;

public class NewBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Establecer saturacion guardada
            Optional<String> saturation = PersistenceController.getInstance(context).restoreFromProperties(MainActivity.keys.SATURATION.name());
            Optional<String> cm = PersistenceController.getInstance(context).restoreFromProperties( MainActivity.keys.CM.name());
            if (saturation.isPresent()) {
                CommandController.execSudo("service call SurfaceFlinger 1022 f " + saturation.get());
                CommandController.execSudo("setprop persist.sys.sf.color_saturation " + saturation.get());
            }
            if (cm.isPresent()) {
                CommandController.execSudo("service call SurfaceFlinger 1023 i32 " + cm);
            }
        }
    }

}

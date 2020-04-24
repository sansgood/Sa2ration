package com.xda.sa2ration;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.xda.sa2ration.databinding.ActivityMainBinding;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import java8.util.Optional;

public class MainActivity extends AppCompatActivity {

    public enum keys {
        SATURATION, CM
    }

    public static final int DEFAULT_PROGRESS = 100;
    public static final String PERSISTENT_COLOR_SATURATION = "persist.sys.sf.color_saturation";
    public static final String PERSISTENT_NATIVE_MODE = "persist.sys.sf.native_mode";
    private static final float STEP_SB = 10f;

    private ActivityMainBinding binding;
    private String saturation = "1.00";
    private String cm = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!CommandController.testSudo()) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.warning_no_root)
                    .setCancelable(false)
                    .setPositiveButton(R.string.accept, (v, a) -> finish())
                    .show();

        }
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        initSaturationBar();
        initImageView();
        initCm();
        initButtons();
    }


    // Values are persisted on pause, in order to restore them when system is rebooted
    // anyways, you can force persist them with the save button
    @Override
    protected void onPause() {
        super.onPause();
        PersistenceController.getInstance(this).storeToProperties(keys.SATURATION.name(), saturation);
        PersistenceController.getInstance(this).storeToProperties(keys.CM.name(), cm);
        PersistenceController.getInstance(this).persist();
    }

    /**
     * Initialized bottom buttons, for force save and default values.
     */
    private void initButtons() {
        binding.content.reset.setOnClickListener(v -> reset());
        binding.content.apply.setOnClickListener(v -> {
            super.onPause();
            PersistenceController.getInstance(this).storeToProperties(keys.SATURATION.name(), saturation);
            PersistenceController.getInstance(this).storeToProperties(keys.CM.name(), cm);
            PersistenceController.getInstance(this).persist();
            Toast.makeText(this, R.string.values_are_saved, Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Initializes color management control.
     */
    private void initCm() {
        Switch dci = binding.content.dci;
        boolean enabled;
        Optional<String> nativeMode = PersistenceController.getInstance(this)
                .restoreFromProperties(keys.CM.name());
        if (nativeMode.isPresent()) {
            cm = nativeMode.get();
            enabled = cm.equals("0");
            binding.content.dci.setChecked(enabled);
        }
        dci.setOnCheckedChangeListener((buttonView, isChecked) -> {
            cm = isChecked ? "0" : "1";
            CommandController.execCommand("service call SurfaceFlinger 1023 i32 " + cm);
            CommandController.setProp(PERSISTENT_NATIVE_MODE, cm);
        });
    }

    /**
     * Initializes saturation SeekBar control.
     */
    private void initSaturationBar() {
        saturation = retrieveCurrentSaturationLevel();
        float fakeProgress = Float.parseFloat(saturation) * 100;
        binding.content.seekBar.setProgress((int) fakeProgress);
        binding.content.seekBar.incrementProgressBy((int)STEP_SB);
        binding.content.textView.setText(format(Float.parseFloat(saturation)));
        binding.content.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progress = progress / (int)STEP_SB;
                progress = progress * (int)STEP_SB;
                saturation = format(progress / 100F);
                binding.content.textView.setText(saturation);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float progress = seekBar.getProgress() / 100F;
                float rounded = ((int)(progress * STEP_SB)) / STEP_SB;
                saturation = format(rounded);
                CommandController.execCommand("setprop " + PERSISTENT_COLOR_SATURATION + " " + saturation,
                        "service call SurfaceFlinger 1022 f " + saturation);
            }
        });

    }

    /**
     * Initialized ImageView with its onClick Listener.
     */
    private void initImageView() {
        ImageView preview = findViewById(R.id.imageView);
        preview.setOnClickListener(v -> {
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.photo_by)
                    .setMessage(Html.fromHtml(getResources().getString(R.string.photo_by_desc), 0))
                    .show();

            TextView link = alertDialog.findViewById(android.R.id.message);
            link.setLinksClickable(true);
            link.setMovementMethod(LinkMovementMethod.getInstance());
        });
    }

    /**
     * Get current persisted saturation level.
     * @return current saturation level.
     */
    private String retrieveCurrentSaturationLevel() {
        //Optional<String> optCurrent = CommandController.getProp(PERSISTENT_COLOR_SATURATION);
        Optional<String> optCurrent = PersistenceController.getInstance(this).restoreFromProperties(keys.SATURATION.name());
        if (optCurrent.isPresent()) {
            saturation = optCurrent.get();
        }
        Log.d(getClass().getName(), "Saturation: " + saturation);
        return saturation;
    }

    /**
     * Reset values to default ones.
     */
    private void reset() {
        binding.content.seekBar.setProgress(DEFAULT_PROGRESS);
        CommandController.execCommand("setprop " + PERSISTENT_COLOR_SATURATION + " " + saturation,
                "service call SurfaceFlinger 1022 f " + saturation);
        binding.content.dci.setChecked(false);
        PersistenceController.getInstance(this).storeToProperties(keys.CM.name(), cm);
        PersistenceController.getInstance(this).storeToProperties(keys.SATURATION.name(), saturation);
        PersistenceController.getInstance(this).persist();
    }

    /**
     * Formats current progress for its representation in TextView.
     * @param progress current progress.
     * @return formatted string that represents progress.
     */
    private String format(float progress) {
        return String.format(Locale.US, "%.2f", progress);
    }


}

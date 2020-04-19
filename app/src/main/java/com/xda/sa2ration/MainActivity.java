package com.xda.sa2ration;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.xda.sa2ration.databinding.ActivityMainBinding;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.Properties;

import androidx.appcompat.app.AppCompatActivity;
import java8.util.Optional;

public class MainActivity extends AppCompatActivity {

    public enum keys {
        SATURATION, HUE, CONTRAST, CM
    }

    private static final String PERSISTENT_COLOR_SATURATION = "persist.sys.sf.color_saturation";
    private static final String PERSISTENT_NATIVE_MODE = "persist.sys.sf.native_mode";
    private static final float STEP_SB = 10f;

    private ActivityMainBinding binding;
    private String saturation = "1.00";
    private String hue = "1.00";
    private String contrast = "1.00";
    private String cm = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!CommandController.testSudo()) {
            finish();
        }
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        initSaturationBar();
        initImageView();
        initCm();
        initButtons();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PersistenceController.getInstance(this).storeToProperties(keys.SATURATION.name(), saturation);
        PersistenceController.getInstance(this).storeToProperties(keys.CM.name(), cm);
        PersistenceController.getInstance(this).persist();
    }

    private void initButtons() {
        binding.content.reset.setOnClickListener(v -> reset());
    }

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
            CommandController.execSudo("service call SurfaceFlinger 1023 i32 " + cm);
            CommandController.setProp(PERSISTENT_NATIVE_MODE, cm);
        });
    }

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
                CommandController.execSudo("setprop persist.sys.sf.color_saturation " + saturation,
                        "service call SurfaceFlinger 1022 f " + saturation);
            }
        });
    }

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

    private String retrieveCurrentSaturationLevel() {
        //Optional<String> optCurrent = CommandController.getProp(PERSISTENT_COLOR_SATURATION);
        Optional<String> optCurrent = PersistenceController.getInstance(this).restoreFromProperties(keys.SATURATION.name());
        if (optCurrent.isPresent()) {
            saturation = optCurrent.get();
        }
        Log.d(getClass().getName(), "Saturation: " + saturation);
        return saturation;
    }

    private void reset() {
        binding.content.seekBar.setProgress(100);
        binding.content.dci.setChecked(false);
        PersistenceController.getInstance(this).storeToProperties(keys.CM.name(), cm);
        PersistenceController.getInstance(this).storeToProperties(keys.SATURATION.name(), saturation);
        PersistenceController.getInstance(this).persist();
    }

    private String format(float progress) {
        return String.format(Locale.US, "%.2f", progress);
    }


}

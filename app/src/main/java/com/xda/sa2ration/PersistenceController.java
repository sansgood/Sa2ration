package com.xda.sa2ration;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import java8.util.Optional;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

import static android.content.Context.MODE_PRIVATE;

public class PersistenceController {

    private Map<String, String> values;
    private Context context;
    private static PersistenceController instance;

    private PersistenceController(Context context) {
        this.context = context;
        values = new HashMap<>();
        Properties properties = new Properties();
        try (FileInputStream fis  = context.openFileInput("info.properties")) {
            properties.load(fis);
            values = StreamSupport.stream(properties.keySet())
                    .collect(Collectors.toMap(String::valueOf, k -> properties.getProperty(k + "")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static PersistenceController getInstance(Context context) {
        if (instance == null) {
            instance = new PersistenceController(context);
        }
        return instance;
    }

    public Optional<String> restoreFromProperties(String propName) {
        return Optional.ofNullable(values.get(propName));
    }

    public void storeToProperties(String propName, String value) {
        values.put(propName, value);
    }

    public void persist() {
        Properties properties = new Properties();
        try (FileOutputStream fos  = context.openFileOutput("info.properties", MODE_PRIVATE)) {
            StreamSupport.stream(values.entrySet())
                    .forEach(entry -> properties.put(entry.getKey(), entry.getValue()));
            properties.store(fos, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

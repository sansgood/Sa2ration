package com.xda.sa2ration;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java8.util.Optional;

public class CommandController {

    public static Optional<String> getProp(String systemProperty) {
        return execSudo("getprop " + systemProperty);
    }

    public static Optional<String> setProp(String systemProperty, String value) {
        return execSudo("setprop " + systemProperty + " " + value);
    }

    public static Optional<String> execSudo(String command) {
        String result = null;
        StringBuilder sb = new StringBuilder();
        try {
            Process su = Runtime.getRuntime().exec("su");
            try (DataOutputStream outputStream = new DataOutputStream(su.getOutputStream())) {
                outputStream.writeBytes(command + "\n");
                outputStream.flush();
                outputStream.writeBytes("exit\n");
                outputStream.flush();
                su.waitFor();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(su.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e("No Root?", e.getMessage());
            }
        } catch(IOException e){
            e.printStackTrace();
        }
        if (sb.length() != 0) {
            result = sb.toString();
        }
        return Optional.ofNullable(result);
    }

    public static boolean testSudo() {
        StackTraceElement st = null;
        try {
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            DataInputStream inputStream = new DataInputStream(su.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            while (bufferedReader.readLine() != null) {
                bufferedReader.readLine();
            }
            su.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            for (StackTraceElement s : e.getStackTrace()) {
                st = s;
                if (st != null) break;
            }
        }
        return st == null;
    }

}

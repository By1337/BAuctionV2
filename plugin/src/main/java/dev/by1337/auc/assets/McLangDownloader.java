package dev.by1337.auc.assets;

import dev.by1337.core.ServerVersion;
import dev.by1337.sync.common.packet.Packet;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class McLangDownloader {
    public static final String SITE = "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets";
    public static final String LANG_PATH = "assets/minecraft/lang";

    public static File downloadLang(String locale){
        locale = locale.toLowerCase();
        File out = new File("./.cache/auction/" + ServerVersion.CURRENT_ID + "+" + locale + ".json");
        if (out.exists()) return out;
        out.getParentFile().mkdirs();
        var url = SITE + "/" + ServerVersion.CURRENT_ID + "/" + LANG_PATH + "/" + locale + ".json";
        return downloadFrom(url, out);
    }

    public static File downloadFrom(String fullUrl, File saveTo) {
        if (saveTo.exists()) return saveTo;
        String data = parsePage(fullUrl);
        try {
            Files.writeString(saveTo.toPath(), data, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return saveTo;
    }

    public static String parsePage(String url) {
        HttpURLConnection connection = null;
        try {
            URL url0 = new URL(url);
            connection = (HttpURLConnection) url0.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(15000);
            connection.setRequestMethod("GET");

            int code = connection.getResponseCode();

            if (code == 200) {
                try (InputStream inputStream = connection.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    return String.join("\n", reader.lines().toList());
                }
            }
            throw new IOException("code: " + code + " Url: " + url);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}

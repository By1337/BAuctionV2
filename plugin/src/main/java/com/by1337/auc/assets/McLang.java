package com.by1337.auc.assets;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translator;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class McLang {
    private static final Gson GSON = new Gson();
    private final Map<String, String> lang = new HashMap<>();
    private final Locale locale;

    public McLang(String locale) {
        this.locale = Translator.parseLocale(locale);
        bootstrap(locale);
    }

    private void bootstrap(String lang) {
        this.lang.clear();
        try (FileReader fr = new FileReader(McLangDownloader.downloadLang(lang))) {
            Map<String, String> map = GSON.fromJson(fr, new TypeToken<Map<String, String>>() {}.getType());
            this.lang.putAll(map);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getTranslation(String key) {
        return lang.getOrDefault(key, key);
    }

    public String getTranslation(String key, Object... args) {
        return String.format(lang.getOrDefault(key, key), args);
    }

    public String getTranslation(Component c) {
        if (c instanceof TranslatableComponent t) {
            if (!lang.containsKey(t.key())) {
                return PlainTextComponentSerializer.plainText().serialize(GlobalTranslator.render(c, locale));
            }
            var args = t.arguments();
            Object[] arr = new Object[args.size()];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = args.get(i).value();
            }
            return getTranslation(t.key(), arr);
        } else {
            return PlainTextComponentSerializer.plainText().serialize(GlobalTranslator.render(c, locale));
        }
    }
}

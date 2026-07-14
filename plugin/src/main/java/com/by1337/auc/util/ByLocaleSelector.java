package com.by1337.auc.util;

import net.kyori.adventure.translation.Translator;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ByLocaleSelector<T> {
    private final Map<String, T> byLanguageTag = new HashMap<>();
    private final Map<String, T> byLC = new HashMap<>();
    private final Map<String, T> byLanguage = new HashMap<>();
    private final Locale def;
    private T def0;

    public ByLocaleSelector(String def, Map<Locale, T> map) {
        this(def);
        for (Map.Entry<Locale, T> e : map.entrySet()) {
            add(e.getKey(), e.getValue());
        }
    }

    public ByLocaleSelector(String def) {
        this.def = Translator.parseLocale(def);
    }

    public T select(Locale locale) {
        T t = select0(locale);
        if (t != null) return t;
        if (def0 != null) return def0;
        return def0 = select0(def);
    }

    private T select0(Locale locale) {
        T t = byLanguageTag.get(locale.toLanguageTag());
        if (t != null) return t;
        var lang = locale.getLanguage();
        t = byLC.get(lang + "-" + locale.getCountry());
        if (t != null) return t;
        var v = byLanguage.get(lang);
        return v;
    }

    public void add(Locale locale, T value) {
        byLanguageTag.put(locale.toLanguageTag(), value);
        byLC.put(locale.getLanguage() + "-" + locale.getCountry(), value);
        byLanguage.put(locale.getLanguage(), value);
    }
}

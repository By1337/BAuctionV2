package com.by1337.auc.config;

import com.by1337.auc.auc.category.Category;
import com.by1337.auc.util.CyclicListIterator;
import dev.by1337.yaml.decoder.RecordYamlDecoder;
import dev.by1337.yaml.decoder.YamlDecoder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Categories {
    public static final YamlDecoder<Categories> DECODER = RecordYamlDecoder.mapOf(
            Categories::new,
            YamlDecoder.mapOf(YamlDecoder.STRING, Category.DECODER)
                    .fieldOf("categories")
    );
    private final Map<String, Category> categories;
    private final List<Category> list;

    public Categories(Map<String, Category> categories) {
        this.categories = categories;
        list = List.copyOf(categories.values());
    }

    public @Nullable Category getById(String id) {
        return categories.get(id);
    }

    public CyclicListIterator<Category> cycle() {
        return new CyclicListIterator<>(list);
    }

    public List<Category> list() {
        return list;
    }
}

package dev.by1337.auc.config;

import dev.by1337.auc.auc.category.Category;
import dev.by1337.auc.handler.SimpleAuction;
import dev.by1337.auc.search.filter.EmptySearchFilter;
import dev.by1337.auc.util.CyclicListIterator;
import dev.by1337.yaml.decoder.RecordYamlDecoder;
import dev.by1337.yaml.decoder.YamlDecoder;
import org.jetbrains.annotations.Nullable;

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
        if (categories.isEmpty()) {
            categories = Map.of("any", new Category("any", EmptySearchFilter.INSTANCE));
        }
        this.categories = categories;
        list = List.copyOf(categories.values());
    }

    public void boot(SimpleAuction auction) {
        for (Category category : list) {
            auction.registries().category.register(category.id(), category);
        }
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

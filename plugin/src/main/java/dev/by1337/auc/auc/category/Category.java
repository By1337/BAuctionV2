package dev.by1337.auc.auc.category;

import dev.by1337.auc.search.filter.EmptySearchFilter;
import dev.by1337.auc.search.filter.SearchFilter;
import dev.by1337.auc.search.filter.SearchFilterParser;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.decoder.YamlDecoder;

public record Category(String id, SearchFilter filter) {
    public static final YamlDecoder<Category> DECODER = (ctx, yaml) -> {
        var v = SearchFilterParser.DECODER.decode(yaml);
        if (!v.hasResult())
            return DataResult.success(new Category(ctx.getFromKey(), EmptySearchFilter.INSTANCE)).withError(v.error());
        return v.map(f -> new Category(ctx.getFromKey(), f));
    };

    public String getTitle() {
        return "<lang:bauctionv2.categories.%s.title>".formatted(id);
    }

    public String getName() {
        return "<lang:bauctionv2.categories.%s.name>".formatted(id);
    }
}

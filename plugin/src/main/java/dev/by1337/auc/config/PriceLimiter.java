package dev.by1337.auc.config;

import dev.by1337.auc.tag.TagsExtractor;
import dev.by1337.auc.util.WildcardMatcher;
import dev.by1337.core.util.math.FastExpressionParser;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.decoder.RecordYamlDecoder;
import dev.by1337.yaml.decoder.YamlDecoder;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;

import java.util.Map;

public class PriceLimiter {
    private static final YamlDecoder<Double> PRICE = YamlDecoder.STRING.flatMap(str -> {
        str = str
                .replace(",", "")
                .replace(".", "")
                .replace("к", "k")
        ;
        try {
            return DataResult.success(FastExpressionParser.parse(str));
        } catch (FastExpressionParser.MathFormatException e) {
            return DataResult.error(e.getMessage());
        }
    });
    public static final YamlDecoder<PriceLimiter> DECODER = RecordYamlDecoder.mapOf(
            PriceLimiter::new,
            YamlDecoder.BOOL.fieldOf("enabled", false),
            PRICE.fieldOf("max", 30_000_000D),
            YamlDecoder.mapOf(YamlDecoder.STRING, PRICE)
                    .fieldOf("by_tag", Map.of())
    );
    private final boolean enabled;
    private final double max;
    private final Object2DoubleOpenHashMap<String> by_tag;
    private TagsExtractor tags;

    public PriceLimiter(boolean enabled, double max, Map<String, Double> byTag) {
        this.enabled = enabled;
        this.max = max;
        by_tag = new Object2DoubleOpenHashMap<>(byTag);
    }

    public void setTags(TagsExtractor tags) {
        this.tags = tags;
    }

    public boolean enabled() {
        return enabled;
    }

    public double max() {
        return max;
    }

    public double getMaxPrice(ItemStack itemStack) {
        var base = tags.extractTags(itemStack);
        double max = -1;
        for (var e : by_tag.object2DoubleEntrySet()) {
            for (String s : base) {
                if (WildcardMatcher.match(e.getKey(), s)) {
                    max = Math.max(max, e.getDoubleValue() * itemStack.getAmount());
                }
            }
        }
        var meta = itemStack.hasItemMeta() ? itemStack.getItemMeta() : null;
        if (meta instanceof BlockStateMeta bsm && bsm.getBlockState() instanceof Container c) {
            for (ItemStack stack : c.getInventory()) {
                if (stack == null || stack.isEmpty()) continue;
                max = Math.max(max, getMaxPrice(stack));
            }
        }
        if (meta instanceof BundleMeta bm && bm.hasItems()){
            for (ItemStack stack : bm.getItems()) {
                if (stack == null || stack.isEmpty()) continue;
                max = Math.max(max, getMaxPrice(stack));
            }
        }
        return max == -1 ? this.max : max;
    }
}

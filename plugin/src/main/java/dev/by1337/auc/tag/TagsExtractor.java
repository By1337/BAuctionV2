package dev.by1337.auc.tag;

import dev.by1337.core.bridge.registry.LegacyRegistryBridge;
import dev.by1337.core.util.reflect.LambdaMetafactoryUtil;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.potion.PotionEffect;
import org.bukkit.spawner.Spawner;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.Function;

public class TagsExtractor {
    private static final Set<String>[] TAGS;
    private final TagsConfig config;

    public TagsExtractor(TagsConfig config) {
        this.config = config;
    }

    public Set<String> extractTags(ItemStack item) {
        Set<String> result = new HashSet<>();
        Material material = item.getType();
        result.add(material.getKey().getKey());

        var tags = TAGS[material.ordinal()];
        if (tags != null){
            result.addAll(tags);
        }
        if (material.isFlammable()) result.add("flammable");
        if (material.isBurnable()) result.add("burnable");
        if (material.isFuel()) result.add("fuel");
        if (material.hasGravity()) result.add("has_gravity");
        if (material.isSolid()) result.add("solid");
        if (material.isRecord()) result.add("record");
        if (material.isEdible()) result.add("edible");
        if (material.isBlock()) result.add("block");

        var im = item.getItemMeta();
        if (im == null) {
            result.add("has_no_componets");
            return config.apply(result);
        }
        if (im instanceof Damageable damageable) {
            var tag = config.damageTag(damageable.getDamage(), material.getMaxDurability());
            if (tag != null) {
                result.add(tag);
            }
        }
        if (im.hasEnchants()) {
            im.getEnchants().forEach((key, value) -> {
                result.add(key.getKey().getKey());
                result.add(key.getKey().getKey() + ":" + value);
            });
        }
        if (im instanceof PotionMeta pm) {
            var base = pm.getBasePotionType();
            if (base != null){
                result.add(base.getKey().value());
                for (PotionEffect potionEffect : base.getPotionEffects()) {
                    result.add(potionEffect.getType().getKey().value());
                }
            }
            if (pm.hasCustomEffects()) {
                for (PotionEffect effect : pm.getCustomEffects()) {
                    String key = LegacyRegistryBridge.MOB_EFFECT.getKey(effect.getType()).value();
                    result.add(key);
                    result.add(key + ":" + effect.getAmplifier());
                }
            }
        }
        if (im.hasCustomModelData()) {
            result.add("model_data:" + im.getCustomModelData());
        }
        if (im instanceof BlockStateMeta bsm) {
            var state = bsm.getBlockState();
            if (state instanceof Container c) {
                if (c.getInventory().isEmpty()) {
                    result.add("empty_container");
                }
            } else if (state instanceof Spawner sp) {
                var type = sp.getSpawnedType();
                if (type == null) {
                    result.add("empty_spawner");
                } else {
                    result.add(type.getKey().value());
                }
            }
        }
        if (im instanceof BundleMeta bm){
            if (bm.hasItems()){
                result.add("empty_bundle");
            }
        }
        if (im instanceof EnchantmentStorageMeta enchantmentStorageMeta) {
            enchantmentStorageMeta.getStoredEnchants().forEach((key, value) -> {
                result.add(key.getKey().getKey());
                result.add(key.getKey().getKey() + ":" + value);
            });
        }

        var map = getPdc(im.getPersistentDataContainer());
        map.forEach((key, value) -> {
            result.add(key);
            result.add(key + ":" + value);
        });

        return config.apply(result);
    }


    private static Function<PersistentDataContainer, Map<String, Object>> GET_RAW;

    private Map<String, Object> getPdc(PersistentDataContainer pdc) {
        if (GET_RAW != null) {
            return GET_RAW.apply(pdc);
        } else {
            try {
                var mn = pdc.getClass().getDeclaredField("customDataTags");
                mn.setAccessible(true);
                GET_RAW = LambdaMetafactoryUtil.getterOf(mn);
                return GET_RAW.apply(pdc);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    static {
        List<Tag<Material>> tags = new ArrayList<>();
        try {
            for (Field field : Tag.class.getFields()) {
                if (field.getType() != Tag.class) continue;
                if (!Modifier.isStatic(field.getModifiers())) continue;
                if (!(field.getGenericType() instanceof ParameterizedType pt)) continue;
                var types = pt.getActualTypeArguments();
                if (types.length != 1 || types[0] != Material.class) {
                    continue;
                }
                Tag<Material> tag = (Tag<Material>) field.get(null);
                tags.add(tag);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        var materials = Material.values();
        TAGS = new Set[materials.length];
        for (Material material : materials) {
            for (Tag<Material> tag : tags) {
                if (tag.isTagged(material)){
                    var set = TAGS[material.ordinal()];
                    if (set == null)
                        TAGS[material.ordinal()] = set = new HashSet<>();
                    set.add(tag.getKey().value());
                }
            }
        }
    }
}

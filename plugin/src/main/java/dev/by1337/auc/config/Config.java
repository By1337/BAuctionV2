package dev.by1337.auc.config;

import dev.by1337.auc.auc.sort.SortingBoot;
import dev.by1337.auc.handler.SimpleAuction;
import dev.by1337.auc.hook.PostJoinUseDelay;
import dev.by1337.auc.lifecycle.AucLifecycle;
import dev.by1337.auc.tag.TagsConfig;
import dev.by1337.auc.tag.TagsExtractor;
import dev.by1337.auc.util.number.DurationParser;
import dev.by1337.bmenu.BMenu;
import dev.by1337.bmenu.slot.SlotFactory;
import dev.by1337.cmd.Command;
import dev.by1337.cmd.argument.ArgumentStrings;
import dev.by1337.core.util.io.ResourceUtil;
import dev.by1337.edsl.MessageManager;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.decoder.RecordYamlDecoder;
import dev.by1337.yaml.decoder.YamlDecoder;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class Config {
    public static final YamlDecoder<Config> DECODER = RecordYamlDecoder.mapOf(
            Config::new,
            YamlDecoder.fromContext(AucLifecycle.class).fieldOf(),
            MessageManager.decoder("messages.yml", "lang.yml").fieldOf(),
            readFile("tags.yml").and(TagsConfig.DECODER).fieldOf(),
            YamlDecoder.mapOf(YamlDecoder.STRING, SlotFactory.CODEC.asDecoder())
                    .fieldOf("visual", Map.of()),
            readFile("categories.yml").and(Categories.DECODER).fieldOf(null, new Categories(Map.of())),
            YamlDecoder.STRING.listOf().fieldOf("sorting"),
            readFile("database.yml").and(DbConfig.DECODER).fieldOf(),
            CommandsConf.DECODER.fieldOf("commands"),
            SlotsConf.DECODER.fieldOf("slots"),
            YamlDecoder.STRING.fieldOf("ah_search_max_price_perm"),
            DurationParser.DECODER.fieldOf("resell_cooldown"),
            DurationParser.DECODER.fieldOf("selling_duration"),
            PostJoinUseDelay.DECODER.fieldOf("post_join_use_delay", new PostJoinUseDelay(false, 10_000)),
            PriceLimiter.DECODER.fieldOf("prices", new PriceLimiter(false, 10_000_000, Map.of()))
    );
    private static final Logger log = LoggerFactory.getLogger(Config.class);
    public final MessageManager eventCtx;
    public final TagsConfig tags;
    public final TagsExtractor tagsExtractor;
    private final Map<String, SlotFactory> visual;
    public final Categories categories;
    public final List<String> sorting;
    public final DbConfig dbConfig;
    public final CommandsConf commands;
    public final SlotsConf slots;
    public final String ah_search_max_price_perm;
    public final long resell_cooldown;
    public final long selling_duration;
    public final PostJoinUseDelay post_join_use_delay;
    public final PriceLimiter priceLimiter;

    public Config(AucLifecycle lifecycle, MessageManager eventCtx, TagsConfig tags, Map<String, SlotFactory> visual, Categories categories, List<String> sorting, DbConfig dbConfig, CommandsConf commands, SlotsConf slots, String ahSearchMaxPricePerm, long resellCooldown, long sellingDuration, PostJoinUseDelay postJoinUseDelay, PriceLimiter priceLimiter) {
        this.eventCtx = eventCtx;
        this.tags = tags;
        tagsExtractor = new TagsExtractor(tags);
        this.visual = visual;
        this.categories = categories;
        this.sorting = sorting;
        this.dbConfig = dbConfig;
        this.commands = commands;
        this.slots = slots;
        ah_search_max_price_perm = ahSearchMaxPricePerm;
        resell_cooldown = resellCooldown;
        selling_duration = sellingDuration;
        post_join_use_delay = postJoinUseDelay;
        this.priceLimiter = priceLimiter;
        if (priceLimiter != null){
            priceLimiter.setTags(tagsExtractor);
        }
        var cmd = lifecycle.bootMessagesCommand(eventCtx.commands());
        cmd.sub(new Command<EventContext>("[visual]").executor(
                new ArgumentStrings<>("visual"),
                (s, v) -> {
                    var f = this.visual.get(v);
                    if (f == null) {
                        log.error("unknown visual {}", v);
                        return;
                    }
                    var menu = BMenu.menuLoader().getOpenedMenu(s.target());
                    if (menu != null) {
                        menu.layers().getMatrix(3)[menu.lastClickedSlot()] = f.build();
                    }
                }
        ));
        eventCtx.setCommands(cmd);
        lifecycle.configBooted(this);
    }

    public void boot(SimpleAuction auction) {
        categories.boot(auction);
        SortingBoot.boot(auction.registries().sorting, sorting);
    }

    private static YamlDecoder<YamlMap> readFile(String name) {
        return YamlDecoder.fromContext(Plugin.class)
                .map(pl -> ResourceUtil.saveIfNotExist(name, pl))
                .and(YamlDecoder.READ_YAML_FROM_FILE);
    }
}

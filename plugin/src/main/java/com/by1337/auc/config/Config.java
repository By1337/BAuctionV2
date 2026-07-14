package com.by1337.auc.config;

import com.by1337.auc.tag.TagsConfig;
import com.by1337.auc.tag.TagsExtractor;
import dev.by1337.bmenu.BMenu;
import dev.by1337.bmenu.slot.SlotFactory;
import dev.by1337.cmd.Command;
import dev.by1337.cmd.argument.ArgumentStrings;
import dev.by1337.core.util.io.ResourceUtil;
import dev.by1337.edsl.EventContextFactory;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.decoder.RecordYamlDecoder;
import dev.by1337.yaml.decoder.YamlDecoder;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Config {
    public static final YamlDecoder<Config> DECODER = RecordYamlDecoder.mapOf(
            Config::new,
            EventContextFactory.decoder("messages.yml").fieldOf(),
            readFile("tags.yml").and(TagsConfig.DECODER).fieldOf(),
            YamlDecoder.mapOf(YamlDecoder.STRING, SlotFactory.CODEC.asDecoder())
                    .fieldOf("visual")
    );
    private static final Logger log = LoggerFactory.getLogger(Config.class);
    public final EventContextFactory eventCtx;
    public final TagsConfig tags;
    public final TagsExtractor tagsExtractor;
    private final Map<String, SlotFactory> visual;

    public Config(EventContextFactory eventCtx, TagsConfig tags, Map<String, SlotFactory> visual) {
        this.eventCtx = eventCtx;
        this.tags = tags;
        tagsExtractor = new TagsExtractor(tags);
        this.visual = visual;
        eventCtx.commands().sub(new Command<EventContext>("[visual]").executor(
                new ArgumentStrings<>("visual"),
                (s, v) -> {
                    var f = this.visual.get(v);
                    if (f == null) {
                        log.error("unknown visual {}", v);
                        return;
                    }
                    var menu = BMenu.menuLoader().getOpenedMenu(s.target());
                    if (menu != null){
                        menu.layers().getMatrix(3)[menu.lastClickedSlot()] = f.build();
                    }
                }
        ));
    }

    private static YamlDecoder<YamlMap> readFile(String name){
        return YamlDecoder.fromContext(Plugin.class)
                .map(pl -> ResourceUtil.saveIfNotExist(name, pl))
                .and(YamlDecoder.READ_YAML_FROM_FILE);
    }
}

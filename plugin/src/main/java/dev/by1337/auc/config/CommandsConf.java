package dev.by1337.auc.config;

import dev.by1337.yaml.decoder.RecordYamlDecoder;
import dev.by1337.yaml.decoder.YamlDecoder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CommandsConf {
    public static final YamlDecoder<CommandsConf> DECODER = RecordYamlDecoder.mapOf(
            CommandsConf::new,
            YamlDecoder.STRING.fieldOf("ah_menu"),
            YamlDecoder.STRINGS.fieldOf("disabled_commands"),
            YamlDecoder.mapOf(YamlDecoder.STRING, YamlDecoder.STRING)
                    .fieldOf("commands_renamer")
    );
    public final String ah_menu;
    public final List<String> disabled_commands;
    public final Map<String, String> renamer;
    public CommandsConf(String ahMenu, List<String> disabled_commands, Map<String, String> renamer) {
        ah_menu = ahMenu;
        this.disabled_commands = disabled_commands;
        this.renamer = Collections.unmodifiableMap(renamer);
    }
    public String rename(String s){
        return renamer.getOrDefault(s, s);
    }
}

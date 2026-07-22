package dev.by1337.auc.config;

import dev.by1337.yaml.decoder.InlineYamlDecoder;
import dev.by1337.yaml.decoder.RecordYamlDecoder;
import dev.by1337.yaml.decoder.YamlDecoder;
import org.bukkit.entity.Player;

import java.util.List;

public class SlotsConf {
    public static final YamlDecoder<SlotsConf> DECODER = RecordYamlDecoder.mapOf(
            SlotsConf::new,
            SlotsPermPair.DECODER.listOf().fieldOf("select_max"),
            SlotsPermPair.DECODER.listOf().fieldOf("add_slots")
    );
    public final List<SlotsPermPair> select_max;
    public final List<SlotsPermPair> add_slots;

    public SlotsConf(List<SlotsPermPair> selectMax, List<SlotsPermPair> addSlots) {
        select_max = selectMax;
        add_slots = addSlots;
    }

    public int collectSlots(Player player) {
        int res = 0;
        for (SlotsPermPair pair : select_max) {
            if (player.hasPermission(pair.perm)) {
                res = Math.max(res, pair.slots);
            }
        }
        for (SlotsPermPair pair : add_slots) {
            if (player.hasPermission(pair.perm)) {
                res += pair.slots;
            }
        }
        return res;
    }

    public record SlotsPermPair(int slots, String perm) {
        public static final YamlDecoder<SlotsPermPair> DECODER = InlineYamlDecoder.inline(
                " ",
                "3 your.permission",
                SlotsPermPair::new,
                YamlDecoder.INT.fieldOf(),
                YamlDecoder.STRING.fieldOf()
        );
    }
}

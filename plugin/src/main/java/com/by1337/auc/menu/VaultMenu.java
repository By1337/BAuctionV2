package com.by1337.auc.menu;

import com.by1337.auc.auc.ClientVaultLot;
import com.by1337.auc.auc.LotData;
import com.by1337.auc.auc.sort.Sorting;
import com.by1337.auc.search.LotsResult;
import dev.by1337.bmenu.command.ExecuteContext;
import dev.by1337.bmenu.menu.Menu;
import dev.by1337.cmd.Command;
import dev.by1337.yaml.codec.PipelineYamlCodecBuilder;
import dev.by1337.yaml.codec.YamlCodec;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class VaultMenu extends LotsMenu {

    private final UUID player;
    private final VaultMenuConfig cfg;
    private final Sorting sorting;

    public VaultMenu(VaultMenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
        cfg = config;
        player = viewer.getUniqueId();
        sorting = auction.registries().sorting.iterator().next();
    }

    @Override
    protected LotsResult search() {
        return switch (cfg.type) {
            case ONLY_VAULT -> auction.playerVaultLots(player);
            case ONLY_ACTIVE -> auction.search(player, null, sorting);
            case VAULT_AND_ACTIVE -> auction.playerVaultLots(player).and(auction.search(player, null, sorting));
        };
    }

    @Override
    protected LotData getByUid(int uid, LotData old) {
        if (old == null) return null;
        if (old.getClass() == ClientVaultLot.class)
            return auction.getVaultLot(uid);
        return auction.getLot(uid);
    }

    static void bootCommands(Command<ExecuteContext> base) {
    }

    public static class VaultMenuConfig extends LotsMenuConfig {
        public static final YamlCodec<VaultMenuConfig> CODEC = new PipelineYamlCodecBuilder<>(VaultMenuConfig::new)
                .and(LotsMenuConfig.RAW_CODEC)
                .field(YamlCodec.fromEnum(Type.class), "vault_type", v -> v.type, (m, v) -> m.type = v)
                .build();
        public Type type;

        @Override
        public Menu create(Player viewer, @Nullable Menu previousMenu) {
            return new VaultMenu(this, viewer, previousMenu);
        }
    }

    public enum Type {
        ONLY_VAULT,
        ONLY_ACTIVE,
        VAULT_AND_ACTIVE,
    }
}

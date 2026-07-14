package com.by1337.auc.menu;

import com.by1337.auc.auc.ClientVaultLot;
import com.by1337.auc.auc.LotData;
import com.by1337.auc.auc.sort.SortingRegistry;
import com.by1337.auc.handler.index.search.LotsResult;
import dev.by1337.bmenu.menu.Menu;
import dev.by1337.yaml.codec.PipelineYamlCodecBuilder;
import dev.by1337.yaml.codec.YamlCodec;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class VaultMenu extends LotsMenu {

    private final UUID player;


    public VaultMenu(VaultMenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
        player = viewer.getUniqueId();
    }

    @Override
    protected LotsResult search() {
        return auction.playerVaultLots(player).and(auction.search(player, null, SortingRegistry.NEWEST));
    }

    @Override
    protected LotData getByUid(int uid, LotData old) {
        if (old == null) return null;
        if (old.getClass() == ClientVaultLot.class)
            return auction.getVaultLot(uid);
        return auction.getLot(uid);
    }

    public static class VaultMenuConfig extends LotsMenuConfig {
        public static final YamlCodec<VaultMenuConfig> CODEC = new PipelineYamlCodecBuilder<>(VaultMenuConfig::new)
                .and(LotsMenuConfig.RAW_CODEC)
                .build();

        @Override
        public Menu create(Player viewer, @Nullable Menu previousMenu) {
            return new VaultMenu(this, viewer, previousMenu);
        }
    }
}

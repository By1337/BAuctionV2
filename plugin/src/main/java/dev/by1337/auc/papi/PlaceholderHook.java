package dev.by1337.auc.papi;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.util.DurationFormatter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderHook extends PlaceholderExpansion {
    @Override
    public @NotNull String getIdentifier() {
        return "BAuctionV2";
    }

    @Override
    public @NotNull String getAuthor() {
        return "By1337";
    }

    @Override
    public @NotNull String getVersion() {
        return BAuction.plugin().getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }


    @Override
    @Nullable
    public String onPlaceholderRequest(final Player player, @NotNull final String params) {
        if (params.startsWith("ms_format")) {
            String s = params.substring("ms_format_".length());
            long ms = Long.parseLong(s);
            return DurationFormatter.getFormat(System.currentTimeMillis() + ms);
        } else if (params.startsWith("lp_ttl")) {
            String s = params.substring("lp_ttl_".length());
            if (player == null) return "0";
            var luckPermsUtil = BAuction.plugin().luckPermsUtil();
            if (luckPermsUtil == null) return "0";
            var v = luckPermsUtil.getPermissionTimeLeft(player.getUniqueId(), s);
            return Long.toString(v);
        }
        return "";
    }

}

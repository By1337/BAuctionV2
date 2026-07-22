package dev.by1337.auc.util.luckperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class LuckPermsUtil {
    private LuckPerms luckPerms;

    public LuckPermsUtil() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        luckPerms = provider.getProvider();
    }

    public long getPermissionTimeLeft(UUID user, String permission) {
        return getPermissionTimeLeft(luckPerms.getUserManager().getUser(user), permission);
    }
    public static long getPermissionTimeLeft(@Nullable User user, String permission) {
        if (user == null) return 0;
        long now = System.currentTimeMillis();

        return user.getNodes().stream()
                .filter(NodeType.PERMISSION::matches)
                .map(n -> (PermissionNode) n)
                .filter(n -> n.getPermission().equalsIgnoreCase(permission))
                .findFirst()
                .map(n -> {
                    if (!n.hasExpiry()) return Long.MAX_VALUE;
                    return Math.max(0,
                            (n.getExpiry().toEpochMilli() - now)
                    );
                })
                .orElse(0L);
    }

    public LuckPerms luckPerms() {
        return luckPerms;
    }
}

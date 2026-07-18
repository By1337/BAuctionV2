package dev.by1337.auc.util.mc;

import dev.by1337.auc.BAuction;
import org.bukkit.Bukkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCUtil {

    private static final Logger log = LoggerFactory.getLogger(MCUtil.class);

    public static void assertMain(){
        if (!Bukkit.isStopping() && !Bukkit.isPrimaryThread())
            throw new IllegalStateException("async trap");
    }

    public static void ensureMain(Runnable r) {
        if (Bukkit.isPrimaryThread()) {
            r.run();
            return;
        }
        if (Bukkit.isStopping() || !BAuction.plugin().isEnabled()) {
            log.warn("MCUtil#ensureMain but server is stopping or plugin is disabled!", new Throwable());
            r.run();
            return;
        }
        Bukkit.getServer().getScheduler().runTask(BAuction.plugin(), r);
    }
}

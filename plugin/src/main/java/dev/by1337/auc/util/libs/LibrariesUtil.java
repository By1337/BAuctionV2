package dev.by1337.auc.util.libs;

import dev.by1337.auc.BAuction;
import dev.by1337.core.util.RepositoryUtil;
import dev.by1337.core.util.io.ResourceUtil;
import dev.by1337.core.util.reflect.ClasspathUtil;

import java.nio.file.Path;

public class LibrariesUtil {
    private static final String BSYNC_JAR = "bsync-client.jar";

    public static void bootMariaDbDriver() {
        Path libraries = BAuction.plugin().getDataFolder().toPath().resolve(".libraries");
        if (!hasClass("org.mariadb.jdbc.Driver")) {
            ClasspathUtil.addUrl(BAuction.plugin(), RepositoryUtil.download("org.mariadb.jdbc:mariadb-java-client:3.5.2", libraries));
        }
    }

    public static void bootH2Driver() {
        Path libraries = BAuction.plugin().getDataFolder().toPath().resolve(".libraries");
        if (!hasClass("org.h2.Driver")) {
            ClasspathUtil.addUrl(BAuction.plugin(), RepositoryUtil.download("com.h2database:h2:2.4.240", libraries));
        }
    }

    public static void boot() {
        Path libraries = BAuction.plugin().getDataFolder().toPath().resolve(".libraries");
        if (!hasClass("dev.by1337.sync.common.channel.ChannelMessage")
                || !hasClass("dev.by1337.sync.client.channel.ChannelMaker")) {
            ResourceUtil.resourceStream(BAuction.plugin(), "libraries/" + BSYNC_JAR)
                    .to(libraries.toFile())
                    .stripPrefix("libraries")
                    .storeIfNotExist();
            ClasspathUtil.addUrl(BAuction.plugin(), libraries.resolve(BSYNC_JAR));
        }
    }

    private static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}

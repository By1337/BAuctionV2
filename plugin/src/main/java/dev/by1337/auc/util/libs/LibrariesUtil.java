package dev.by1337.auc.util.libs;

import dev.by1337.auc.BAuction;
import dev.by1337.core.util.reflect.ClasspathUtil;

import java.nio.file.Path;

public class LibrariesUtil {
    public static void bootMariaDbDriver() {
        Path libraries = BAuction.plugin().getDataFolder().toPath().resolve(".libraries");
        if (!hasClass("org.mariadb.jdbc.Driver")) {
            ClasspathUtil.addUrl(BAuction.plugin(), AucRepositoryUtil.download(AucRepositoryUtil.MAVEN_REPO, "org.mariadb.jdbc:mariadb-java-client:3.5.2", libraries));
        }
    }

    public static void bootH2Driver() {
        Path libraries = BAuction.plugin().getDataFolder().toPath().resolve(".libraries");
        if (!hasClass("org.h2.Driver")) {
            ClasspathUtil.addUrl(BAuction.plugin(), AucRepositoryUtil.download(AucRepositoryUtil.MAVEN_REPO, "com.h2database:h2:2.4.240", libraries));
        }
    }

    public static void boot() {
        Path libraries = BAuction.plugin().getDataFolder().toPath().resolve(".libraries");
        if (!hasClass("com.zaxxer.hikari.HikariConfig")) {
            ClasspathUtil.addUrl(BAuction.plugin(), AucRepositoryUtil.download(AucRepositoryUtil.MAVEN_REPO, "com.zaxxer:HikariCP:7.0.2", libraries));
        }
        if (!hasClass("com.github.benmanes.caffeine.cache.Caffeine")) {
            ClasspathUtil.addUrl(BAuction.plugin(), AucRepositoryUtil.download(AucRepositoryUtil.MAVEN_REPO, "com.github.ben-manes.caffeine:caffeine:3.2.3", libraries));
        }
        if (!hasClass("org.roaringbitmap.RoaringBitmap")) {
            ClasspathUtil.addUrl(BAuction.plugin(), AucRepositoryUtil.download(AucRepositoryUtil.MAVEN_REPO, "org.roaringbitmap:RoaringBitmap:1.6.14", libraries));
        }
        // bsync
        if (!hasClass("dev.by1337.sync.common.channel.ChannelMessage")) {
            ClasspathUtil.addUrl(BAuction.plugin(), AucRepositoryUtil.download(AucRepositoryUtil.BDEV_REPO,
                    "dev.by1337.sync:bsync-common:1.3",
                    libraries)
            );
            ClasspathUtil.addUrl(BAuction.plugin(), AucRepositoryUtil.download(AucRepositoryUtil.BDEV_REPO,
                    "dev.by1337.sync:bsync-client:1.3",
                    libraries)
            );
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

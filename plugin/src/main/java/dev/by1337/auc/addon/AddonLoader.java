package dev.by1337.auc.addon;

import dev.by1337.auc.BAuction;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class AddonLoader implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger("AddonLoader");
    private final Map<String, AbstractAddon> addons = new ConcurrentHashMap<>();
    private final List<AddonClassLoader> loaders = new CopyOnWriteArrayList<>();
    private final File folder;
    private final BAuction plugin;

    public AddonLoader(File folder, BAuction plugin) {
        this.folder = folder;
        this.plugin = plugin;
        folder.mkdirs();
    }

    public void findAddons() {
        for (File file : folder.listFiles()) {
            if (file.getName().endsWith(".jar")) {
                try {
                    AddonClassLoader addonClassLoader = new AddonClassLoader(this.getClass().getClassLoader(), file, this, plugin);
                    loaders.add(addonClassLoader);
                    addons.put(addonClassLoader.description().name(), addonClassLoader.addon());
                } catch (Throwable e) {
                    LOGGER.error("Failed to load addon {}", file.getName(), e);
                }
            }
        }
    }

    public void enableAll() {
        for (AbstractAddon value : addons.values()) {
            try {
                value.setEnabled(true);
            } catch (Throwable e) {
                LOGGER.error("Failed to enable addon {}", value.getDescription().name(), e);
            }
        }
    }

    public void disableAll() {
        for (AbstractAddon value : addons.values()) {
            try {
                value.setEnabled(false);
            } catch (Throwable e) {
                LOGGER.error("Failed to disable addon {}", value.getDescription().name(), e);
            }
        }
    }
    public Collection<AbstractAddon> addons(){
        return Collections.unmodifiableCollection(addons.values());
    }


    @Nullable
    public Class<?> getClassByName(String name, boolean resolve) {
        for (AddonClassLoader loader1 : loaders) {
            try {
                return loader1.loadClass0(name, resolve, false);
            } catch (ClassNotFoundException ignore) {
            }
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        addons.clear();
        for (AddonClassLoader loader : loaders) {
            loader.close();
        }
        loaders.clear();
    }
}

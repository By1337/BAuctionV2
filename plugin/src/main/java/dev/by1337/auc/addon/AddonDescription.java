package dev.by1337.auc.addon;

public class AddonDescription {
    private final String name;
    private final String mainClass;
    private final String version;
    private final String author;


    public AddonDescription(String name, String mainClass, String version, String author) {
        this.name = name;
        this.mainClass = mainClass;
        this.version = version;

        this.author = author;
    }

    public String name() {
        return name;
    }

    public String mainClass() {
        return mainClass;
    }

    public String version() {
        return version;
    }
}

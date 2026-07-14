package com.by1337.auc.handler.name;

import java.util.Objects;

public class PlayerName {
    private String name;

    public PlayerName(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public String name() {
        return name;
    }

    boolean setName(String name) {
        boolean changed = !Objects.equals(this.name, name);
        this.name = Objects.requireNonNull(name);
        return changed;
    }
}

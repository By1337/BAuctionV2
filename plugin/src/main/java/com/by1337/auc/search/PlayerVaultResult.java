package com.by1337.auc.search;

import com.by1337.auc.auc.ClientVaultLot;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

public record PlayerVaultResult(Iterator<ClientVaultLot> iterator, int size) implements LotsResult {

    public @Nullable ClientVaultLot next() {
        if (!iterator.hasNext()) return null;
        return iterator.next();
    }

    @Override
    public void release() {
    }
}

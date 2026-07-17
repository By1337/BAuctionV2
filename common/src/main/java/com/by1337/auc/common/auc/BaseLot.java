package com.by1337.auc.common.auc;

public interface BaseLot {
    BaseLot withUid(int uid);
    int uid();
    byte[] asBytes();
}

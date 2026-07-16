package com.by1337.auc.registry;

import com.by1337.auc.auc.category.Category;
import com.by1337.auc.auc.sort.Sorting;

public class AucRegistries {
    public AucRegistry<Sorting> sorting = new AucRegistry<>();
    public AucRegistry<Category> category = new AucRegistry<>();


    public void writeLock() {
        sorting.writeLock();
        category.writeLock();
    }
}

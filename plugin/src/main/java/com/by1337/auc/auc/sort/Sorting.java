package com.by1337.auc.auc.sort;

import com.by1337.auc.auc.ClientAucLot;

import java.util.Comparator;

public record Sorting(String id, Comparator<ClientAucLot> comparator) {
}

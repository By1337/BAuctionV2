package dev.by1337.auc.search;

import dev.by1337.auc.search.filter.SearchFilter;

import java.util.ArrayList;
import java.util.List;

public class TrieNode {
    char value;
    boolean isEnd;
    List<TrieNode> children;
    SearchFilter storedValue;

    public TrieNode(char value) {
        this.value = value;
        this.isEnd = false;
        this.children = new ArrayList<>();
        this.storedValue = null;
    }
}

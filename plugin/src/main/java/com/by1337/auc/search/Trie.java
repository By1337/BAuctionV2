package com.by1337.auc.search;

import com.by1337.auc.search.filter.SearchFilter;

import java.util.ArrayList;
import java.util.List;

public class Trie {
    private final TrieNode root;

    public Trie() {
        root = new TrieNode('\0');
    }

    public void insert(String key, SearchFilter value) {
        TrieNode node = root;
        for (char ch : key.toCharArray()) {
            TrieNode child = getChildWithChar(node, ch);
            if (child == null) {
                child = new TrieNode(ch);
                node.children.add(child);
            }
            node = child;
        }
        node.isEnd = true;
        node.storedValue = value;
    }

    public List<SearchFilter> getAllWithPrefix(String prefix) {
        List<SearchFilter> results = new ArrayList<>();
        TrieNode node = getNodeByPrefix(prefix);
        if (node == null) return List.of();
        getAllWithPrefixHelper(node, results);
        return results;
    }

    public List<String> getAllKeysWithPrefix(String prefix) {
        return getAllKeysWithPrefix(prefix, -1);
    }

    public List<String> getAllKeysWithPrefix(String prefix, int limit) {
        List<String> keys;
        if (limit > 0) {
            keys = new ArrayList<>(limit);
        } else {
            keys = new ArrayList<>();
        }
        TrieNode node = getNodeByPrefix(prefix);
        getAllKeysWithPrefixHelper(node, prefix, keys, limit);
        return keys;
    }

    private TrieNode getNodeByPrefix(String prefix) {
        TrieNode node = root;
        for (char ch : prefix.toCharArray()) {
            TrieNode child = getChildWithChar(node, ch);
            if (child == null) {
                return null;
            }
            node = child;
        }
        return node;
    }

    private TrieNode getChildWithChar(TrieNode node, char ch) {
        for (TrieNode child : node.children) {
            if (child.value == ch) {
                return child;
            }
        }
        return null;
    }

    private void getAllWithPrefixHelper(TrieNode node, List<SearchFilter> results) {
        if (node == null) {
            return;
        }

        if (node.isEnd) {
            results.add(node.storedValue);
        }

        for (TrieNode child : node.children) {
            getAllWithPrefixHelper(child, results);
        }
    }

    private void getAllKeysWithPrefixHelper(TrieNode node, String currentPrefix, List<String> keys) {
        getAllKeysWithPrefixHelper(node, currentPrefix, keys, -1);
    }

    private void getAllKeysWithPrefixHelper(TrieNode node, String currentPrefix, List<String> keys, int limit) {
        if (node == null) {
            return;
        }

        if (node.isEnd) {
            keys.add(currentPrefix);
        }
        for (TrieNode child : node.children) {
            if (limit != -1 && keys.size() >= limit) {
                return;
            }
            if (child != null) {
                getAllKeysWithPrefixHelper(child, currentPrefix + child.value, keys, limit);
            }
        }
    }
}

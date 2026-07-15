package com.by1337.auc.search;

import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Trie<T> {

    private final Node<T> root = new Node<>('\0', null);

    public T insert(CharSequence key, T value) {
        Node<T> node = root;
        var len = key.length();
        for (int i = 0; i < len; i++) {
            char c = key.charAt(i);
            var child = node.children.get(c);
            if (child == null) {
                child = new Node<>(c, null);
                node.children.put(c, child);
            }
            node = child;
        }
        var old = node.storedValue;
        node.storedValue = value;
        node.key = key.toString();
        return old;
    }

    public List<T> parse(CharSequence key) {
        List<T> result = new ArrayList<>();
        var len = key.length();
        int start = 0;

        while (start < len) {
            boolean found = false;
            for (int end = len; end > start; end--) {
                T t = get(key, start, end);
                if (t != null) {
                    result.add(t);
                    start = end;
                    found = true;
                    break;
                }
            }

            if (!found) {
                start++;
            }
        }

        return result;
    }

    public List<String> getSuggestions(CharSequence prefix, int limit) {
        List<String> suggestions = new ArrayList<>();
        int len = prefix.length();

        for (int start = 0; start < len; start++) {
            Node<T> node = root;
            boolean validPrefix = true;

            for (int i = start; i < len; i++) {
                char c = prefix.charAt(i);
                boolean validSpace = c == ' ' && node.key != null;
                node = node.children.get(prefix.charAt(i));
                if (node == null && validSpace){
                    start = i;
                    validPrefix = false;
                    break;
                }
                if (node == null) {
                    validPrefix = false;
                    break;
                }
            }

            if (validPrefix && node != null) {
                String before = prefix.subSequence(0, start).toString();
                collectSuggestions(node, before, suggestions, limit);
                return suggestions;
            }
        }

        Node<T> node = root;
        for (int i = 0; i < len; i++) {
            node = node.children.get(prefix.charAt(i));
            if (node == null) {
                return suggestions;
            }
        }
        collectSuggestions(node, prefix.toString(), suggestions, limit);
        return suggestions;
    }

    private void collectSuggestions(Node<T> node, String prefix, List<String> suggestions, int limit) {
        if (suggestions.size() >= limit) return;
        if (node.storedValue != null) {
            suggestions.add(prefix + node.key);
        }

        for (Node<T> child : node.children.values()) {
            collectSuggestions(child, prefix, suggestions, limit);
        }
    }

    private @Nullable T get(CharSequence key, int from, int to) {
        Node<T> node = root;
        for (int i = from; i < to; i++) {
            node = node.children.get(key.charAt(i));
            if (node == null) return null;
        }
        return node.storedValue;
    }

    private @Nullable T get(CharSequence key) {
        return get(key, 0, key.length());
    }

    private static class Node<T> {
        final char c;
        final Char2ObjectMap<Node<T>> children = new Char2ObjectOpenHashMap<>();
        T storedValue;
        String key;
        boolean end;

        private Node(char c, T storedValue) {
            this.c = c;
            this.storedValue = storedValue;
        }
    }
}

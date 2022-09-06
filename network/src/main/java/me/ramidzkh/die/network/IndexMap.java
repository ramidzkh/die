package me.ramidzkh.die.network;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.jetbrains.annotations.Nullable;

class IndexMap<N> {

    private final BiMap<N, Integer> nodeToIndex = HashBiMap.create();
    private int index;

    public int insert(N node) {
        var myIndex = nodeToIndex.get(node);

        if (myIndex != null) {
            return myIndex;
        } else {
            nodeToIndex.put(node, index);
            return index++;
        }
    }

    @Nullable
    public N get(int index) {
        return nodeToIndex.inverse().get(index);
    }

    public int size() {
        return nodeToIndex.size();
    }
}

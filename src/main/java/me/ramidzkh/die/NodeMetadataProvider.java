package me.ramidzkh.die;

import java.util.List;

/**
 * Provides metadata about the node, such as children and textual value
 *
 * @param <N> Node
 */
public interface NodeMetadataProvider<N> {

    /**
     * Lists direct children
     *
     * @param node Parent node
     * @return Children nodes
     */
    List<N> getChildren(N node);

    /**
     * @param node Node
     * @return Type
     */
    String getType(N node);

    /**
     * @param node Node
     * @return Textual value
     */
    String getLabel(N node);
}

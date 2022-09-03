/*
 * Copyright 2021 delexer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.ramidzkh.die;

import org.jetbrains.annotations.Nullable;

/**
 * Tree diffing algorithm
 */
public interface Differ {

    /**
     * Diffs two nodes against each other
     *
     * @param metadata Provides metadata about the node, such as children and textual value
     * @param original The original node
     * @param modified The modified node
     * @param provider The provider of a visitor which accepts the changes
     * @param <N>      Node, such as origin tree node
     */
    <N> void diff(NodeMetadataProvider<N> metadata, N original, N modified, DiffVisitorProvider<N> provider);

    /**
     * The provider of a visitor which accepts the changes, given context about matching nodes
     *
     * @param <N> Node
     */
    interface DiffVisitorProvider<N> {

        /**
         * Creates a diff visitor based on a {@link NodeMatches node match predicate}
         *
         * @param matches A node match predicate
         * @return A visitor which accepts changes
         */
        Visitor<N> createVisitor(NodeMatches<N> matches);
    }

    /**
     * Context of matching nodes
     *
     * @param <N> Node
     */
    interface NodeMatches<N> {

        /**
         * Checks if the given nodes are matched between both sources. Swapping the parameters will give the same
         * result. Node which have been totally replaces also match
         *
         * @param a The first node
         * @param b The second node
         * @return Do they match
         */
        boolean areMatched(N a, N b);

        /**
         * Find the node which has matched to this node
         *
         * @param node The node
         * @return The matching node
         */
        @Nullable
        N getMatchedNode(N node);
    }

    /**
     * A visitor which accepts changes
     *
     * @param <N> Node
     */
    interface Visitor<N> {

        /**
         * Marks the deletion of a node
         *
         * @param node The deleted node
         */
        void delete(N node);

        /**
         * Marks the insertion of a node within another node
         *
         * @param node   The added node
         * @param parent The parent node
         * @param pos    The position of the node
         */
        void insert(N node, N parent, int pos);

        /**
         * Marks the move of a node into another node
         *
         * @param node   The moved node
         * @param parent The node it was moved into
         * @param pos    The new position of the new
         */
        void move(N node, N parent, int pos);

        /**
         * Marks the replacement of a node
         *
         * @param node The original node
         * @param x    The replacement node
         */
        void replace(N node, N x);

        /**
         * Indicates that all actions have been visited
         */
        void finish();
    }
}

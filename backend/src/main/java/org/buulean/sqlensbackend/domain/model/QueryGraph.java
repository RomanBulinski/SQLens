package org.buulean.sqlensbackend.domain.model;

import java.util.ArrayList;
import java.util.List;

/** Aggregate root representing the full query structure as a graph. */
public class QueryGraph {

    private final List<TableNode> nodes;
    private final List<JoinEdge>  edges;

    private QueryGraph(Builder builder) {
        this.nodes = List.copyOf(builder.nodes);
        this.edges = List.copyOf(builder.edges);
    }

    public List<TableNode> getNodes() { return nodes; }
    public List<JoinEdge>  getEdges() { return edges; }

    public int complexity() { return nodes.size(); }

    public boolean hasCartesianProducts() {
        return nodes.stream().anyMatch(n ->
            edges.stream().noneMatch(e ->
                e.getFromNodeId().equals(n.getId()) || e.getToNodeId().equals(n.getId())
            )
        );
    }

    public static class Builder {
        private final List<TableNode> nodes = new ArrayList<>();
        private final List<JoinEdge>  edges = new ArrayList<>();

        public Builder addNode(TableNode node) { nodes.add(node); return this; }
        public Builder addEdge(JoinEdge  edge) { edges.add(edge); return this; }
        public QueryGraph build()              { return new QueryGraph(this); }
    }
}

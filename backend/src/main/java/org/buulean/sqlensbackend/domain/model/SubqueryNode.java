package org.buulean.sqlensbackend.domain.model;

import java.util.Optional;

public class SubqueryNode extends TableNode {

    private final QueryGraph innerGraph;

    public SubqueryNode(String alias, QueryGraph innerGraph) {
        super("subquery_" + alias, "(subquery)", alias);
        this.innerGraph = innerGraph;
    }

    @Override public String getNodeType()        { return "subquery"; }
    @Override public String getBackgroundColor() { return "#fff3cd"; }

    @Override
    public Optional<QueryGraph> getInnerGraph() {
        return Optional.of(innerGraph);
    }
}

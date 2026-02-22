package org.buulean.sqlensbackend.domain.model;

import java.util.Optional;

public abstract class TableNode {

    protected final String id;
    protected final String name;
    protected final String alias;

    protected TableNode(String id, String name, String alias) {
        this.id    = id;
        this.name  = name;
        this.alias = alias;
    }

    public String getId()    { return id; }
    public String getName()  { return name; }
    public String getAlias() { return alias; }

    /** Subclasses define their type label ("table" | "subquery" | "cte"). */
    public abstract String getNodeType();

    /** Subclasses define their background colour for the diagram. */
    public abstract String getBackgroundColor();

    public String getDisplayLabel() {
        return alias != null ? name + " (" + alias + ")" : name;
    }

    /** Composite pattern — only SubqueryNode overrides this. */
    public Optional<QueryGraph> getInnerGraph() {
        return Optional.empty();
    }
}

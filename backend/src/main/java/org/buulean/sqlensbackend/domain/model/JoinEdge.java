package org.buulean.sqlensbackend.domain.model;

import java.util.List;
import java.util.Objects;

/** Immutable value object representing a JOIN between two nodes. */
public final class JoinEdge {

    private final String       fromNodeId;
    private final String       toNodeId;
    private final JoinType     joinType;
    private final String       condition;
    private final List<String> columns;

    public JoinEdge(String fromNodeId, String toNodeId,
                    JoinType joinType, String condition,
                    List<String> columns) {
        this.fromNodeId = fromNodeId;
        this.toNodeId   = toNodeId;
        this.joinType   = joinType;
        this.condition  = condition;
        this.columns    = List.copyOf(columns != null ? columns : List.of());
    }

    public String       getFromNodeId() { return fromNodeId; }
    public String       getToNodeId()   { return toNodeId; }
    public JoinType     getJoinType()   { return joinType; }
    public String       getCondition()  { return condition; }
    public List<String> getColumns()    { return columns; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JoinEdge other)) return false;
        return Objects.equals(fromNodeId, other.fromNodeId)
            && Objects.equals(toNodeId,   other.toNodeId)
            && joinType == other.joinType
            && Objects.equals(condition,  other.condition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromNodeId, toNodeId, joinType, condition);
    }
}

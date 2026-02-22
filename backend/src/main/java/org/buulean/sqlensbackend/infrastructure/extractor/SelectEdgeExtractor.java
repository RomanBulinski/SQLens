package org.buulean.sqlensbackend.infrastructure.extractor;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.buulean.sqlensbackend.domain.model.JoinEdge;
import org.buulean.sqlensbackend.domain.model.JoinType;
import org.buulean.sqlensbackend.domain.port.EdgeExtractor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Extracts JoinEdge objects from the JOIN clauses of a plain SELECT statement.
 */
@Service
public class SelectEdgeExtractor implements EdgeExtractor {

    @Override
    public boolean canHandle(Statement statement) {
        return statement instanceof Select;
    }

    @Override
    public List<JoinEdge> extract(Statement statement) {
        List<JoinEdge> edges = new ArrayList<>();
        Select select = (Select) statement;

        if (!(select.getSelectBody() instanceof PlainSelect plainSelect)) {
            return edges;
        }

        List<Join> joins = plainSelect.getJoins();
        if (joins == null || joins.isEmpty()) {
            return edges;
        }

        String defaultFromId = nodeId(plainSelect.getFromItem());

        for (Join join : joins) {
            JoinType     type      = resolveJoinType(join);
            String       toId      = nodeId(join.getRightItem());
            String       condition = extractCondition(join);
            List<String> columns   = extractColumns(condition);
            String       fromId    = resolveFromId(defaultFromId, toId, condition);

            edges.add(new JoinEdge(fromId, toId, type, condition, columns));
        }

        return edges;
    }

    /**
     * Determines the source node of a JOIN from its ON condition.
     * For "ep.project_id = p.id" with toId="p", this returns "ep".
     * Falls back to defaultFromId when the condition cannot be parsed.
     */
    private String resolveFromId(String defaultFromId, String toId, String condition) {
        if (condition == null || condition.isBlank()) return defaultFromId;

        Set<String> refs = new LinkedHashSet<>();
        for (String token : condition.split("[=\\s()]+")) {
            int dot = token.lastIndexOf('.');
            if (dot > 0) {
                refs.add(token.substring(0, dot));
            }
        }
        refs.remove(toId);
        return refs.isEmpty() ? defaultFromId : refs.iterator().next();
    }

    private String nodeId(FromItem fromItem) {
        if (fromItem instanceof Table t) {
            return t.getAlias() != null ? t.getAlias().getName() : t.getName();
        }
        return fromItem.toString();
    }

    private JoinType resolveJoinType(Join join) {
        if (join.isLeft())  return JoinType.LEFT;
        if (join.isRight()) return JoinType.RIGHT;
        if (join.isFull())  return JoinType.FULL;
        if (join.isCross()) return JoinType.CROSS;
        return JoinType.INNER; // INNER JOIN or bare JOIN (simple)
    }

    private String extractCondition(Join join) {
        Collection<Expression> onExprs = join.getOnExpressions();
        if (onExprs != null && !onExprs.isEmpty()) {
            return onExprs.iterator().next().toString();
        }
        return "";
    }

    /** Simple column extraction from "t1.col1 = t2.col2" style conditions. */
    private List<String> extractColumns(String condition) {
        if (condition == null || condition.isBlank()) return List.of();
        List<String> columns = new ArrayList<>();
        for (String token : condition.split("[=\\s]+")) {
            token = token.trim();
            if (token.contains(".")) {
                String col = token.substring(token.lastIndexOf('.') + 1);
                if (!col.isBlank()) columns.add(col);
            }
        }
        return List.copyOf(columns);
    }
}

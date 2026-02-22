package org.buulean.sqlensbackend.infrastructure.extractor;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.buulean.sqlensbackend.domain.model.BaseTableNode;
import org.buulean.sqlensbackend.domain.model.TableNode;
import org.buulean.sqlensbackend.domain.port.NodeExtractor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts BaseTableNode objects from the FROM clause and JOIN clauses
 * of a plain SELECT statement.
 */
@Service
public class SelectNodeExtractor implements NodeExtractor {

    @Override
    public boolean canHandle(Statement statement) {
        return statement instanceof Select;
    }

    @Override
    public List<TableNode> extract(Statement statement) {
        List<TableNode> nodes = new ArrayList<>();
        Select select = (Select) statement;

        if (!(select.getSelectBody() instanceof PlainSelect plainSelect)) {
            return nodes;
        }

        // FROM item
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem instanceof Table t) {
            nodes.add(tableNode(t));
        }

        // JOIN items
        List<Join> joins = plainSelect.getJoins();
        if (joins != null) {
            for (Join join : joins) {
                if (join.getRightItem() instanceof Table t) {
                    nodes.add(tableNode(t));
                }
            }
        }

        return nodes;
    }

    private TableNode tableNode(Table t) {
        String alias = t.getAlias() != null ? t.getAlias().getName() : null;
        String id    = alias != null ? alias : t.getName();
        return new BaseTableNode(id, t.getName(), alias);
    }
}

package org.buulean.sqlensbackend.infrastructure.extractor;

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.WithItem;
import org.buulean.sqlensbackend.domain.model.CteNode;
import org.buulean.sqlensbackend.domain.model.TableNode;
import org.buulean.sqlensbackend.domain.port.NodeExtractor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts CteNode objects from WITH clauses (Common Table Expressions).
 */
@Service
public class CteNodeExtractor implements NodeExtractor {

    @Override
    public boolean canHandle(Statement statement) {
        if (!(statement instanceof Select select)) return false;
        List<WithItem> withItems = select.getWithItemsList();
        return withItems != null && !withItems.isEmpty();
    }

    @Override
    public List<TableNode> extract(Statement statement) {
        Select select = (Select) statement;
        List<TableNode> nodes = new ArrayList<>();

        for (WithItem withItem : select.getWithItemsList()) {
            String cteName = withItem.getAlias() != null
                    ? withItem.getAlias().getName()
                    : withItem.toString();
            nodes.add(new CteNode(cteName, false));
        }

        return nodes;
    }
}

package org.buulean.sqlensbackend.infrastructure.extractor;

import net.sf.jsqlparser.statement.Statement;
import org.buulean.sqlensbackend.domain.model.TableNode;
import org.buulean.sqlensbackend.domain.port.NodeExtractor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Extracts SubqueryNode objects from subqueries in the FROM clause.
 * Phase 2 implementation — disabled for Phase 1 MVP.
 */
@Service
public class SubqueryNodeExtractor implements NodeExtractor {

    @Override
    public boolean canHandle(Statement statement) {
        // Phase 2: detect ParenthesedSelect in FROM clause
        return false;
    }

    @Override
    public List<TableNode> extract(Statement statement) {
        return List.of();
    }
}

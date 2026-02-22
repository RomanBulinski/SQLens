package org.buulean.sqlensbackend.domain.port;

import net.sf.jsqlparser.statement.Statement;
import org.buulean.sqlensbackend.domain.model.TableNode;

import java.util.List;

/** Strategy for extracting TableNode objects from a parsed AST. */
public interface NodeExtractor {
    boolean canHandle(Statement statement);
    List<TableNode> extract(Statement statement);
}

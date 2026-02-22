package org.buulean.sqlensbackend.domain.port;

import net.sf.jsqlparser.statement.Statement;
import org.buulean.sqlensbackend.domain.model.JoinEdge;

import java.util.List;

/** Strategy for extracting JoinEdge objects from a parsed AST. */
public interface EdgeExtractor {
    boolean canHandle(Statement statement);
    List<JoinEdge> extract(Statement statement);
}

package org.buulean.sqlensbackend.application.service;

import net.sf.jsqlparser.statement.Statement;
import org.buulean.sqlensbackend.domain.model.QueryGraph;
import org.buulean.sqlensbackend.domain.port.EdgeExtractor;
import org.buulean.sqlensbackend.domain.port.NodeExtractor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Assembles a QueryGraph by delegating to all registered NodeExtractor
 * and EdgeExtractor implementations (Open/Closed Principle).
 */
@Service
public class DiagramModelBuilder {

    private final List<NodeExtractor> nodeExtractors;
    private final List<EdgeExtractor> edgeExtractors;

    public DiagramModelBuilder(List<NodeExtractor> nodeExtractors,
                               List<EdgeExtractor> edgeExtractors) {
        this.nodeExtractors = nodeExtractors;
        this.edgeExtractors = edgeExtractors;
    }

    public QueryGraph build(Statement statement) {
        QueryGraph.Builder graph = new QueryGraph.Builder();

        nodeExtractors.stream()
            .filter(e -> e.canHandle(statement))
            .flatMap(e -> e.extract(statement).stream())
            .forEach(graph::addNode);

        edgeExtractors.stream()
            .filter(e -> e.canHandle(statement))
            .flatMap(e -> e.extract(statement).stream())
            .forEach(graph::addEdge);

        return graph.build();
    }
}

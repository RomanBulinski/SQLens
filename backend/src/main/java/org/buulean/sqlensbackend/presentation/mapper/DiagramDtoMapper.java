package org.buulean.sqlensbackend.presentation.mapper;

import org.buulean.sqlensbackend.domain.model.JoinEdge;
import org.buulean.sqlensbackend.domain.model.QueryGraph;
import org.buulean.sqlensbackend.domain.model.TableNode;
import org.buulean.sqlensbackend.domain.result.ParseError;
import org.buulean.sqlensbackend.presentation.dto.DiagramResponseDto;
import org.buulean.sqlensbackend.presentation.dto.EdgeDto;
import org.buulean.sqlensbackend.presentation.dto.ErrorResponseDto;
import org.buulean.sqlensbackend.presentation.dto.NodeDto;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DiagramDtoMapper {

    public DiagramResponseDto toDto(QueryGraph graph) {
        Map<String, List<String>> nodeColumns = extractNodeColumns(graph.getEdges());

        List<NodeDto> nodes = graph.getNodes().stream()
                .map(node -> toNodeDto(node, nodeColumns))
                .toList();

        List<EdgeDto> edges = graph.getEdges().stream()
                .map(this::toEdgeDto)
                .toList();

        return new DiagramResponseDto(nodes, edges, null);
    }

    private NodeDto toNodeDto(TableNode node, Map<String, List<String>> nodeColumns) {
        List<String> columns = nodeColumns.getOrDefault(node.getId(), List.of());
        return new NodeDto(
                node.getId(),
                node.getNodeType(),
                node.getName(),
                node.getAlias(),
                columns
        );
    }

    private EdgeDto toEdgeDto(JoinEdge edge) {
        String id = edge.getFromNodeId() + "__" + edge.getToNodeId();
        return new EdgeDto(
                id,
                edge.getFromNodeId(),
                edge.getToNodeId(),
                edge.getJoinType().name(),
                edge.getCondition()
        );
    }

    public ErrorResponseDto toErrorDto(ParseError error) {
        return new ErrorResponseDto(
                error.getCode(),
                error.getMessage(),
                error.getLine(),
                error.getColumn(),
                error.getSuggestion()
        );
    }

    /**
     * Parses each edge's ON condition to map nodeId → columns used.
     * e.g. "o.customer_id = c.id" → { o: [customer_id], c: [id] }
     */
    private Map<String, List<String>> extractNodeColumns(List<JoinEdge> edges) {
        Map<String, LinkedHashSet<String>> sets = new LinkedHashMap<>();

        for (JoinEdge edge : edges) {
            String condition = edge.getCondition();
            if (condition == null || condition.isBlank()) continue;

            // Split compound conditions on AND / OR
            String[] clauses = condition.split("(?i)\\s+AND\\s+|\\s+OR\\s+");
            for (String clause : clauses) {
                // Split on = to get both sides
                String[] sides = clause.split("=", 2);
                for (String side : sides) {
                    side = side.replaceAll("[()\\s]", "");
                    int dot = side.lastIndexOf('.');
                    if (dot > 0 && dot < side.length() - 1) {
                        String tableRef = side.substring(0, dot);
                        String colName  = side.substring(dot + 1);
                        if (!tableRef.isBlank() && !colName.isBlank()) {
                            sets.computeIfAbsent(tableRef, k -> new LinkedHashSet<>())
                                .add(colName);
                        }
                    }
                }
            }
        }

        Map<String, List<String>> result = new LinkedHashMap<>();
        sets.forEach((k, v) -> result.put(k, new ArrayList<>(v)));
        return result;
    }
}

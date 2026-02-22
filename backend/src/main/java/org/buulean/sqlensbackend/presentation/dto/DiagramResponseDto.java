package org.buulean.sqlensbackend.presentation.dto;

import java.util.List;

public class DiagramResponseDto {

    private List<NodeDto> nodes;
    private List<EdgeDto> edges;
    private String        warning;

    public DiagramResponseDto() {}

    public DiagramResponseDto(List<NodeDto> nodes, List<EdgeDto> edges, String warning) {
        this.nodes   = nodes;
        this.edges   = edges;
        this.warning = warning;
    }

    public List<NodeDto> getNodes()   { return nodes; }
    public List<EdgeDto> getEdges()   { return edges; }
    public String        getWarning() { return warning; }
    public void          setWarning(String warning) { this.warning = warning; }
}

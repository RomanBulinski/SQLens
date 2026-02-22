package org.buulean.sqlensbackend.domain.model;

public class CteNode extends TableNode {

    private final boolean recursive;

    public CteNode(String name, boolean recursive) {
        super("cte_" + name, name, null);
        this.recursive = recursive;
    }

    @Override public String getNodeType()        { return "cte"; }
    @Override public String getBackgroundColor() { return "#cfe2ff"; }

    public boolean isRecursive() { return recursive; }
}

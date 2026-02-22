package org.buulean.sqlensbackend.domain.model;

public class BaseTableNode extends TableNode {

    public BaseTableNode(String id, String name, String alias) {
        super(id, name, alias);
    }

    @Override public String getNodeType()        { return "table"; }
    @Override public String getBackgroundColor() { return "#ffffff"; }

}

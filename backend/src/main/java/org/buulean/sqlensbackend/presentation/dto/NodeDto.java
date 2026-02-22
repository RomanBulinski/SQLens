package org.buulean.sqlensbackend.presentation.dto;

import java.util.List;

public class NodeDto {

    private String       id;
    private String       type;
    private String       tableName;
    private String       alias;
    private List<String> columns;

    public NodeDto() {}

    public NodeDto(String id, String type, String tableName, String alias, List<String> columns) {
        this.id        = id;
        this.type      = type;
        this.tableName = tableName;
        this.alias     = alias;
        this.columns   = columns;
    }

    public String       getId()        { return id; }
    public String       getType()      { return type; }
    public String       getTableName() { return tableName; }
    public String       getAlias()     { return alias; }
    public List<String> getColumns()   { return columns; }
}

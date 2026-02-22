package org.buulean.sqlensbackend.presentation.dto;

public class EdgeDto {

    private String id;
    private String sourceId;
    private String targetId;
    private String joinType;
    private String condition;

    public EdgeDto() {}

    public EdgeDto(String id, String sourceId, String targetId,
                   String joinType, String condition) {
        this.id        = id;
        this.sourceId  = sourceId;
        this.targetId  = targetId;
        this.joinType  = joinType;
        this.condition = condition;
    }

    public String getId()        { return id; }
    public String getSourceId()  { return sourceId; }
    public String getTargetId()  { return targetId; }
    public String getJoinType()  { return joinType; }
    public String getCondition() { return condition; }
}

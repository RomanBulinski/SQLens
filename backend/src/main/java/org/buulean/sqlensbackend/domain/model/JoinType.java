package org.buulean.sqlensbackend.domain.model;

public enum JoinType {

    INNER("#21808d", "solid",  "→"),
    LEFT ("#a84b2f", "solid",  "→"),
    RIGHT("#6b21a8", "solid",  "←"),
    FULL ("#c0152f", "solid",  "↔"),
    CROSS("#626c71", "dashed", "—");

    private final String color;
    private final String lineStyle;
    private final String arrowStyle;

    JoinType(String color, String lineStyle, String arrowStyle) {
        this.color      = color;
        this.lineStyle  = lineStyle;
        this.arrowStyle = arrowStyle;
    }

    public String  getColor()      { return color; }
    public String  getLineStyle()  { return lineStyle; }
    public String  getArrowStyle() { return arrowStyle; }

    public boolean isBidirectional() { return this == FULL; }
    public boolean hasCondition()    { return this != CROSS; }
}

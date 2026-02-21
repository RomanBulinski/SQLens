export type NodeType = 'table' | 'subquery' | 'cte';

export interface NodeDto {
  id: string;
  type: NodeType;
  tableName?: string;
  alias?: string;
  columns?: string[];
}

export interface EdgeDto {
  id: string;
  sourceId: string;
  targetId: string;
  joinType: string;
  condition?: string;
}

export interface DiagramResponse {
  nodes: NodeDto[];
  edges: EdgeDto[];
}

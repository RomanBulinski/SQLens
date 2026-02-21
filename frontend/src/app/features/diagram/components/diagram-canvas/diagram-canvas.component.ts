import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import * as joint from 'jointjs';
import { DiagramResponse, NodeDto } from '../../../../core/models/diagram-response.model';

const NODE_W = 160;
const NODE_H = 56;
const COL_GAP = 100;
const ROW_GAP = 80;
const COLS = 3;
const PADDING = 40;

const NODE_COLORS: Record<string, { fill: string; stroke: string }> = {
  table:    { fill: '#dbeafe', stroke: '#3b82f6' },
  subquery: { fill: '#ede9fe', stroke: '#7c3aed' },
  cte:      { fill: '#fef3c7', stroke: '#d97706' },
};

@Component({
  selector: 'app-diagram-canvas',
  standalone: true,
  templateUrl: './diagram-canvas.component.html',
  styleUrl: './diagram-canvas.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DiagramCanvasComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() diagram: DiagramResponse | null = null;
  @ViewChild('canvasEl') canvasRef!: ElementRef<HTMLDivElement>;

  private graph!: joint.dia.Graph;
  private paper!: joint.dia.Paper;
  private initialized = false;
  private isPanning = false;
  private panStart = { x: 0, y: 0 };

  constructor(private ngZone: NgZone) {}

  ngAfterViewInit(): void {
    this.ngZone.runOutsideAngular(() => this.initJoint());
    this.initialized = true;
    if (this.diagram) {
      this.render(this.diagram);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['diagram'] && this.initialized && this.diagram) {
      this.render(this.diagram);
    }
  }

  ngOnDestroy(): void {
    this.graph?.clear();
    (this.paper as any)?.remove();
  }

  private initJoint(): void {
    this.graph = new joint.dia.Graph({}, { cellNamespace: joint.shapes });

    this.paper = new joint.dia.Paper({
      el: this.canvasRef.nativeElement,
      model: this.graph,
      width: 3000,
      height: 2000,
      gridSize: 10,
      drawGrid: { name: 'dot', args: { color: '#cbd5e1' } },
      background: { color: '#f8fafc' },
      interactive: { vertexAdd: false },
      cellViewNamespace: joint.shapes,
    });

    // Pan on blank drag
    this.paper.on('blank:pointerdown', (evt: any) => {
      this.isPanning = true;
      this.panStart = { x: evt.clientX, y: evt.clientY };
      this.canvasRef.nativeElement.style.cursor = 'grabbing';
    });

    this.paper.on('blank:pointermove', (evt: any) => {
      if (!this.isPanning) return;
      const dx = evt.clientX - this.panStart.x;
      const dy = evt.clientY - this.panStart.y;
      const t = this.paper.translate();
      this.paper.translate(t.tx + dx, t.ty + dy);
      this.panStart = { x: evt.clientX, y: evt.clientY };
    });

    this.paper.on('blank:pointerup', () => {
      this.isPanning = false;
      this.canvasRef.nativeElement.style.cursor = 'grab';
    });

    // Zoom with scroll wheel
    const zoom = (delta: number) => {
      const s = this.paper.scale();
      const next = Math.min(3, Math.max(0.2, s.sx + delta * 0.1));
      this.paper.scale(next, next);
    };
    this.paper.on('blank:mousewheel', (_e: any, _x: number, _y: number, delta: number) => zoom(delta));
    this.paper.on('cell:mousewheel', (_cv: any, _e: any, _x: number, _y: number, delta: number) => zoom(delta));

    // Hover highlight
    this.paper.on('element:mouseenter', (cellView: any) => {
      const id = (cellView as joint.dia.CellView & { model: joint.dia.Cell }).model.id;
      this.graph.getLinks().forEach(link => {
        const highlight = link.getSourceCell()?.id === id || link.getTargetCell()?.id === id;
        link.attr('line/stroke', highlight ? '#3b82f6' : '#94a3b8');
        link.attr('line/strokeWidth', highlight ? 3 : 2);
      });
    });

    this.paper.on('element:mouseleave', (_cellView: any) => {
      this.graph.getLinks().forEach(link => {
        link.attr('line/stroke', '#94a3b8');
        link.attr('line/strokeWidth', 2);
      });
    });
  }

  private render(data: DiagramResponse): void {
    this.graph.clear();
    this.paper.translate(0, 0);
    this.paper.scale(1, 1);

    const cellMap: Record<string, joint.shapes.standard.Rectangle> = {};

    data.nodes.forEach((node: NodeDto, i: number) => {
      const col = i % COLS;
      const row = Math.floor(i / COLS);
      const x = PADDING + col * (NODE_W + COL_GAP);
      const y = PADDING + row * (NODE_H + ROW_GAP);
      const colors = NODE_COLORS[node.type] ?? NODE_COLORS['table'];
      const label = node.alias
        ? `${node.tableName ?? node.id}\n[${node.alias}]`
        : (node.tableName ?? node.id);

      const rect = new joint.shapes.standard.Rectangle({
        position: { x, y },
        size: { width: NODE_W, height: NODE_H },
        attrs: {
          body: {
            fill: colors.fill,
            stroke: colors.stroke,
            strokeWidth: 2,
            rx: 6,
            ry: 6,
          },
          label: {
            text: label,
            fill: '#1e293b',
            fontSize: 12,
            fontWeight: 'bold',
            fontFamily: "'Cascadia Code', 'Fira Code', Consolas, monospace",
          },
        },
      });

      rect.addTo(this.graph);
      cellMap[node.id] = rect;
    });

    data.edges.forEach(edge => {
      const src = cellMap[edge.sourceId];
      const tgt = cellMap[edge.targetId];
      if (!src || !tgt) return;

      const link = new joint.shapes.standard.Link({
        source: { id: src.id },
        target: { id: tgt.id },
        attrs: {
          line: {
            stroke: '#94a3b8',
            strokeWidth: 2,
            targetMarker: {
              type: 'path',
              d: 'M 10 -5 0 0 10 5 z',
              fill: '#94a3b8',
            },
          },
        },
        labels: [
          {
            position: 0.5,
            attrs: {
              text: {
                text: edge.joinType,
                fontSize: 10,
                fill: '#475569',
                fontWeight: '600',
                fontFamily: 'Inter, sans-serif',
              },
              rect: {
                fill: 'white',
                stroke: '#e2e8f0',
                strokeWidth: 1,
                rx: 3,
                ry: 3,
                refWidth: 8,
                refHeight: 4,
                refX: -4,
                refY: -2,
              },
            },
          },
        ],
      });

      link.addTo(this.graph);
    });

    // Center content in the viewport
    setTimeout(() => {
      this.paper.fitToContent({ padding: PADDING, allowNewOrigin: 'any' });
    }, 0);
  }
}

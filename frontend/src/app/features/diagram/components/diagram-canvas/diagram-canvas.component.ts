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

// ── Layout constants ────────────────────────────────────────────────────────
const NODE_W   = 190;
const HEADER_H = 32;
const COL_H    = 16;
const BODY_PAD = 8;
const H_GAP    = 80;
const V_GAP    = 60;
const COLS     = 3;
const PADDING  = 40;

const NODE_COLORS: Record<string, { fill: string; stroke: string }> = {
  table:    { fill: '#dbeafe', stroke: '#2563eb' },
  subquery: { fill: '#ede9fe', stroke: '#7c3aed' },
  cte:      { fill: '#fef3c7', stroke: '#d97706' },
};

/**
 * Custom JointJS shape: coloured header rect + body rect + two text elements.
 * Defined once at module level so joint.shapes.sqlens.TableNode is registered.
 */
const TableNodeShape = joint.dia.Element.define(
  'sqlens.TableNode',
  { attrs: { body: {}, header: {}, headerLabel: {}, bodyLabel: {} } },
  {
    markup: [
      { tagName: 'rect', selector: 'body' },
      { tagName: 'rect', selector: 'header' },
      { tagName: 'text', selector: 'headerLabel' },
      { tagName: 'text', selector: 'bodyLabel' },
    ],
  }
);

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
  private isPanning   = false;
  private panStart    = { x: 0, y: 0 };

  constructor(private ngZone: NgZone) {}

  ngAfterViewInit(): void {
    this.ngZone.runOutsideAngular(() => this.initJoint());
    this.initialized = true;
    if (this.diagram) this.render(this.diagram);
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

  // ── JointJS initialisation ────────────────────────────────────────────────

  private initJoint(): void {
    this.graph = new joint.dia.Graph({}, { cellNamespace: joint.shapes });

    this.paper = new joint.dia.Paper({
      el:                 this.canvasRef.nativeElement,
      model:              this.graph,
      width:              3000,
      height:             2000,
      gridSize:           10,
      drawGrid:           { name: 'dot', args: { color: '#cbd5e1' } },
      background:         { color: '#f8fafc' },
      interactive:        { vertexAdd: false },
      cellViewNamespace:  joint.shapes,
    });

    // Pan
    this.paper.on('blank:pointerdown', (evt: any) => {
      this.isPanning = true;
      this.panStart  = { x: evt.clientX, y: evt.clientY };
      this.canvasRef.nativeElement.style.cursor = 'grabbing';
    });
    this.paper.on('blank:pointermove', (evt: any) => {
      if (!this.isPanning) return;
      const t = this.paper.translate();
      this.paper.translate(t.tx + evt.clientX - this.panStart.x,
                           t.ty + evt.clientY - this.panStart.y);
      this.panStart = { x: evt.clientX, y: evt.clientY };
    });
    this.paper.on('blank:pointerup', () => {
      this.isPanning = false;
      this.canvasRef.nativeElement.style.cursor = 'grab';
    });

    // Zoom
    const zoom = (delta: number) => {
      const s    = this.paper.scale();
      const next = Math.min(3, Math.max(0.2, s.sx + delta * 0.1));
      this.paper.scale(next, next);
    };
    this.paper.on('blank:mousewheel', (_e: any, _x: number, _y: number, d: number) => zoom(d));
    this.paper.on('cell:mousewheel',  (_cv: any, _e: any, _x: number, _y: number, d: number) => zoom(d));

    // Hover highlight
    this.paper.on('element:mouseenter', (cv: any) => {
      const id = cv.model.id;
      this.graph.getLinks().forEach(lk => {
        const hit = lk.getSourceCell()?.id === id || lk.getTargetCell()?.id === id;
        lk.attr('line/stroke',      hit ? '#2563eb' : '#94a3b8');
        lk.attr('line/strokeWidth', hit ? 3 : 2);
      });
    });
    this.paper.on('element:mouseleave', () => {
      this.graph.getLinks().forEach(lk => {
        lk.attr('line/stroke',      '#94a3b8');
        lk.attr('line/strokeWidth', 2);
      });
    });
  }

  // ── Render ────────────────────────────────────────────────────────────────

  private render(data: DiagramResponse): void {
    this.graph.clear();
    this.paper.translate(0, 0);
    this.paper.scale(1, 1);

    const nodeH = (n: NodeDto) =>
      HEADER_H + (n.columns?.length ?? 0) * COL_H + BODY_PAD * 2;

    // Compute row-aware Y positions so taller nodes don't overlap the next row
    const rows: NodeDto[][] = [];
    for (let i = 0; i < data.nodes.length; i += COLS) {
      rows.push(data.nodes.slice(i, i + COLS));
    }
    const rowMaxH = rows.map(r => Math.max(...r.map(nodeH)));
    const rowY    = rowMaxH.reduce<number[]>((acc, _h, i) => {
      acc.push(i === 0 ? PADDING : acc[i - 1] + rowMaxH[i - 1] + V_GAP);
      return acc;
    }, []);

    const cellMap: Record<string, joint.dia.Element> = {};

    data.nodes.forEach((node: NodeDto, i: number) => {
      const col     = i % COLS;
      const row     = Math.floor(i / COLS);
      const x       = PADDING + col * (NODE_W + H_GAP);
      const y       = rowY[row];
      const h       = nodeH(node);
      const colors  = NODE_COLORS[node.type] ?? NODE_COLORS['table'];
      const columns = node.columns ?? [];
      const title   = node.alias
        ? `${node.tableName ?? node.id} [${node.alias}]`
        : (node.tableName ?? node.id);

      const el = new (TableNodeShape as any)({
        position: { x, y },
        size:     { width: NODE_W, height: h },
        attrs: {
          // Full-height card background
          body: {
            refWidth:    '100%',
            refHeight:   '100%',
            fill:        colors.fill,
            stroke:      colors.stroke,
            strokeWidth: 2,
            rx:          6,
            ry:          6,
          },
          // Coloured header strip at the top
          header: {
            refWidth: '100%',
            height:   HEADER_H,
            fill:     colors.stroke,
            stroke:   'none',
            rx:       6,
            ry:       6,
          },
          // Table name — white on coloured header
          headerLabel: {
            refX:       '50%',
            refY:       HEADER_H / 2,
            textAnchor: 'middle',
            yAlignment: 'middle',
            text:       title,
            fill:       '#ffffff',
            fontSize:   11,
            fontWeight: 'bold',
            fontFamily: 'Consolas, "Courier New", monospace',
          },
          // Column names — dark on light body
          bodyLabel: {
            refX:       8,
            refY:       HEADER_H + BODY_PAD,
            yAlignment: 'top',
            text:       columns.join('\n'),
            fill:       '#1e293b',
            fontSize:   11,
            fontFamily: 'Consolas, "Courier New", monospace',
          },
        },
      }) as joint.dia.Element;

      el.addTo(this.graph);
      cellMap[node.id] = el;
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
            stroke:       '#94a3b8',
            strokeWidth:  2,
            targetMarker: { type: 'path', d: 'M 10 -5 0 0 10 5 z', fill: '#94a3b8' },
          },
        },
        labels: [{
          position: 0.5,
          attrs: {
            text: {
              text:       edge.joinType,
              fontSize:   10,
              fill:       '#475569',
              fontWeight: '600',
              fontFamily: 'Inter, sans-serif',
            },
            rect: {
              fill:        'white',
              stroke:      '#e2e8f0',
              strokeWidth: 1,
              rx:          3,
              ry:          3,
              refWidth:    8,
              refHeight:   4,
              refX:        -4,
              refY:        -2,
            },
          },
        }],
      });

      link.addTo(this.graph);
    });

    setTimeout(() => {
      this.paper.fitToContent({ padding: PADDING, allowNewOrigin: 'any' });
    }, 0);
  }
}

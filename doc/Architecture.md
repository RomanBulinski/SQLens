# Architecture Design Document
## SQLens - SQL Query Structure Visualizer
### OOP · SOLID · Clean Architecture · Design Patterns

**Version:** 1.0
**Date:** February 21, 2026
**Author:** Roman Bulinski

---

## 1. Architecture Philosophy

SQLens has a sharp, well-defined domain: **parse SQL text → produce a graph model → render it visually**. This pipeline maps cleanly to layered architecture, and the natural variation points (different node types, different JOIN types, different export formats, different layout algorithms) are exactly where OOP and SOLID principles earn their keep.

The guiding rule for every decision below:

> **Complexity lives only where the domain is complex. Everything else stays simple.**

---

## 2. High-Level Architecture: Clean Architecture

The backend follows **Clean Architecture** (Robert C. Martin). Dependencies point inward — outer layers know about inner layers, never the reverse.

```
┌────────────────────────────────────────────────────┐
│              Presentation Layer                    │
│   REST Controllers · DTOs · Request/Response       │
├────────────────────────────────────────────────────┤
│              Application Layer                     │
│   Use Cases · Application Services · Port Interfaces│
├────────────────────────────────────────────────────┤
│              Domain Layer  (pure Java)             │
│   Entities · Value Objects · Domain Services       │
├────────────────────────────────────────────────────┤
│              Infrastructure Layer                  │
│   JSQLParser Adapter · Spring config · LocalStorage│
└────────────────────────────────────────────────────┘
```

**Why this matters for SQLens specifically:**
- The domain (graph of tables + joins) never needs to know about HTTP, JSQLParser, or JointJS
- Swapping JSQLParser for another parser = change one adapter class, zero domain changes
- Unit-testing the core parsing logic needs no Spring context, no mocks of HTTP

---

## 3. Domain Layer — Pure OOP Model

This is the heart of the application. No framework annotations, no library imports — just Java classes representing the problem domain.

### 3.1 Core Domain Objects

```
Domain
├── QueryGraph          ← Aggregate Root
├── TableNode           ← Abstract Entity (base class)
│   ├── BaseTableNode   ← concrete: regular FROM table
│   ├── SubqueryNode    ← concrete: (SELECT ...) alias
│   └── CteNode         ← concrete: WITH name AS (...)
├── JoinEdge            ← Value Object
├── JoinType            ← Enum
├── ParseResult<T,E>    ← Generic Result type
└── ParseError          ← Value Object
```

### 3.2 Class Designs

#### `JoinType` — Enum with behaviour (not just constants)

```java
public enum JoinType {
    INNER ("#21808d", "solid",  "→"),
    LEFT  ("#a84b2f", "solid",  "→"),
    RIGHT ("#6b21a8", "solid",  "←"),
    FULL  ("#c0152f", "solid",  "↔"),
    CROSS ("#626c71", "dashed", "—");

    private final String color;
    private final String lineStyle;
    private final String arrowStyle;

    JoinType(String color, String lineStyle, String arrowStyle) {
        this.color = color;
        this.lineStyle = lineStyle;
        this.arrowStyle = arrowStyle;
    }

    // Behaviour lives with data — OOP encapsulation
    public boolean isBidirectional() { return this == FULL; }
    public boolean hasCondition()    { return this != CROSS; }
}
```

**OOP principle applied:** Encapsulation — rendering metadata lives in the enum, not scattered across if-else chains in services.

#### `TableNode` — Abstract base with polymorphism

```java
public abstract class TableNode {
    protected final String id;       // unique within query graph
    protected final String name;     // display name
    protected final String alias;    // SQL alias (may be null)

    protected TableNode(String id, String name, String alias) {
        this.id = id;
        this.name = name;
        this.alias = alias;
    }

    // Template method — subclasses define their type label
    public abstract String getNodeType();

    // Template method — subclasses define display color
    public abstract String getBackgroundColor();

    public String getDisplayLabel() {
        return alias != null
            ? name + " (" + alias + ")"
            : name;
    }

    // Subclasses may override to add child graphs (SubqueryNode)
    public Optional<QueryGraph> getInnerGraph() {
        return Optional.empty();
    }
}
```

**OOP principles applied:**
- **Abstraction:** callers work with `TableNode`, not with concrete subtypes
- **Inheritance:** shared fields and `getDisplayLabel()` defined once
- **Polymorphism:** `getNodeType()` and `getBackgroundColor()` vary per subtype

#### Concrete `TableNode` subtypes

```java
public class BaseTableNode extends TableNode {
    public BaseTableNode(String id, String name, String alias) {
        super(id, name, alias);
    }

    @Override public String getNodeType()         { return "table"; }
    @Override public String getBackgroundColor()  { return "#ffffff"; }
}

public class SubqueryNode extends TableNode {
    private final QueryGraph innerGraph;  // Composite pattern

    public SubqueryNode(String alias, QueryGraph innerGraph) {
        super("subquery_" + alias, "(subquery)", alias);
        this.innerGraph = innerGraph;
    }

    @Override public String getNodeType()         { return "subquery"; }
    @Override public String getBackgroundColor()  { return "#fff3cd"; }
    @Override public Optional<QueryGraph> getInnerGraph() {
        return Optional.of(innerGraph);
    }
}

public class CteNode extends TableNode {
    private final boolean recursive;

    public CteNode(String name, boolean recursive) {
        super("cte_" + name, name, null);
        this.recursive = recursive;
    }

    @Override public String getNodeType()         { return "cte"; }
    @Override public String getBackgroundColor()  { return "#cfe2ff"; }
    public boolean isRecursive()                  { return recursive; }
}
```

#### `JoinEdge` — Immutable Value Object

```java
public final class JoinEdge {
    private final String fromNodeId;
    private final String toNodeId;
    private final JoinType joinType;
    private final String condition;       // "o.customer_id = c.id"
    private final List<String> columns;   // ["customer_id", "id"]

    // All-args constructor, no setters — immutability
    public JoinEdge(String fromNodeId, String toNodeId,
                    JoinType joinType, String condition,
                    List<String> columns) {
        this.fromNodeId = fromNodeId;
        this.toNodeId   = toNodeId;
        this.joinType   = joinType;
        this.condition  = condition;
        this.columns    = List.copyOf(columns);
    }

    // Value Object equality by content, not identity
    @Override
    public boolean equals(Object o) { ... }
    @Override
    public int hashCode() { ... }
}
```

#### `QueryGraph` — Aggregate Root with Builder

```java
public class QueryGraph {
    private final List<TableNode> nodes;
    private final List<JoinEdge>  edges;

    private QueryGraph(Builder builder) {
        this.nodes = List.copyOf(builder.nodes);
        this.edges = List.copyOf(builder.edges);
    }

    public List<TableNode> getNodes() { return nodes; }
    public List<JoinEdge>  getEdges() { return edges; }

    public int complexity() { return nodes.size(); }

    public boolean hasCartesianProducts() {
        // Business logic: any node with no edges?
        return nodes.stream()
            .anyMatch(n -> edges.stream()
                .noneMatch(e -> e.getFromNodeId().equals(n.getId())
                             || e.getToNodeId().equals(n.getId())));
    }

    // Builder Pattern — complex object construction is readable
    public static class Builder {
        private final List<TableNode> nodes = new ArrayList<>();
        private final List<JoinEdge>  edges = new ArrayList<>();

        public Builder addNode(TableNode node) {
            nodes.add(node); return this;
        }
        public Builder addEdge(JoinEdge edge) {
            edges.add(edge); return this;
        }
        public QueryGraph build() {
            return new QueryGraph(this);
        }
    }
}
```

**OOP principles applied:**
- **Encapsulation:** Builder controls construction, lists are defensively copied
- **Builder Pattern:** readable construction of complex aggregate

#### `ParseResult<T, E>` — Explicit Success/Failure

Avoid exceptions for control flow. Instead, return a typed result:

```java
public sealed interface ParseResult<T, E> permits ParseResult.Success, ParseResult.Failure {

    record Success<T, E>(T value) implements ParseResult<T, E> {}
    record Failure<T, E>(E error) implements ParseResult<T, E> {}

    static <T, E> ParseResult<T, E> success(T value) { return new Success<>(value); }
    static <T, E> ParseResult<T, E> failure(E error)  { return new Failure<>(error); }

    default boolean isSuccess() { return this instanceof Success<T, E>; }
}
```

Usage is explicit and forces the caller to handle both cases:

```java
ParseResult<QueryGraph, ParseError> result = analyzer.analyze(sql);
if (result instanceof ParseResult.Success<QueryGraph, ParseError> s) {
    return ResponseEntity.ok(mapper.toDto(s.value()));
} else if (result instanceof ParseResult.Failure<QueryGraph, ParseError> f) {
    return ResponseEntity.badRequest().body(f.error());
}
```

---

## 4. SOLID Principles — Applied

### S — Single Responsibility Principle

Each class has **one reason to change**.

| Class | Single Responsibility |
|-------|-----------------------|
| `SqlParserService` | Wrap JSQLParser; translate library exceptions to domain errors |
| `TableExtractor` | Extract `TableNode` objects from a parsed AST |
| `JoinExtractor` | Extract `JoinEdge` objects from a parsed AST |
| `DiagramModelBuilder` | Assemble a `QueryGraph` from extracted parts |
| `SqlAnalyzerService` | Orchestrate the parsing pipeline |
| `DiagramDtoMapper` | Convert domain `QueryGraph` to REST response DTO |
| `SqlAnalyzeController` | Handle HTTP request/response only |

**Anti-pattern to avoid:** A god-class `SqlService` that does all of this.
If parsing logic changes, only `SqlParserService` changes. If the DTO shape changes, only `DiagramDtoMapper` changes.

### O — Open/Closed Principle

**Open for extension, closed for modification.**

The core insight: adding a new node type (e.g., a `UnionNode` in Phase 4) must not require editing existing extractors.

Define **extractor interfaces** with a contract:

```java
// Port interface — lives in Application layer
public interface NodeExtractor {
    boolean canHandle(Statement statement);
    List<TableNode> extract(Statement statement);
}

public interface EdgeExtractor {
    boolean canHandle(Statement statement);
    List<JoinEdge> extract(Statement statement);
}
```

Register all implementations in Spring context:

```java
@Service
public class SelectNodeExtractor implements NodeExtractor {
    @Override
    public boolean canHandle(Statement s) { return s instanceof Select; }

    @Override
    public List<TableNode> extract(Statement s) {
        // extract FROM tables and aliases
    }
}

@Service
public class CteNodeExtractor implements NodeExtractor {
    @Override
    public boolean canHandle(Statement s) {
        return s instanceof Select sel && sel.getWithItemsList() != null;
    }

    @Override
    public List<TableNode> extract(Statement s) {
        // extract CTE definitions
    }
}
```

Adding Phase 4 `UnionExtractor`:

```java
@Service
public class UnionNodeExtractor implements NodeExtractor { ... }
// Zero changes to existing code
```

The `DiagramModelBuilder` collects all `NodeExtractor` implementations via Spring injection:

```java
@Service
public class DiagramModelBuilder {
    private final List<NodeExtractor> nodeExtractors;
    private final List<EdgeExtractor> edgeExtractors;

    public DiagramModelBuilder(List<NodeExtractor> nodeExtractors,
                               List<EdgeExtractor> edgeExtractors) {
        this.nodeExtractors = nodeExtractors;
        this.edgeExtractors = edgeExtractors;
    }

    public QueryGraph build(Statement statement) {
        QueryGraph.Builder graph = new QueryGraph.Builder();

        nodeExtractors.stream()
            .filter(e -> e.canHandle(statement))
            .flatMap(e -> e.extract(statement).stream())
            .forEach(graph::addNode);

        edgeExtractors.stream()
            .filter(e -> e.canHandle(statement))
            .flatMap(e -> e.extract(statement).stream())
            .forEach(graph::addEdge);

        return graph.build();
    }
}
```

### L — Liskov Substitution Principle

Any `TableNode` subtype must be usable wherever `TableNode` is expected, without breaking behaviour.

The `DiagramDtoMapper` maps any `TableNode` uniformly:

```java
public NodeDto toDto(TableNode node) {
    return new NodeDto(
        node.getId(),
        node.getDisplayLabel(),
        node.getNodeType(),
        node.getBackgroundColor(),
        node.getInnerGraph().map(this::toDto).orElse(null)
    );
}
```

This method works identically for `BaseTableNode`, `SubqueryNode`, `CteNode`, and any future node type — because all honour the `TableNode` contract.

**LSP violation to avoid:** A subclass that throws `UnsupportedOperationException` for an inherited method breaks substitutability.

### I — Interface Segregation Principle

**Clients should not be forced to depend on interfaces they don't use.**

Wrong: one fat interface:
```java
// BAD — forces every implementor to handle everything
public interface SqlProcessor {
    QueryGraph parse(String sql);
    byte[] exportToPng(QueryGraph graph);
    void saveToHistory(String sql);
    List<String> getHistory();
}
```

Right: small, focused port interfaces:

```java
// Application layer ports
public interface SqlParser {
    ParseResult<Statement, ParseError> parse(String sql);
}

public interface NodeExtractor {
    boolean canHandle(Statement s);
    List<TableNode> extract(Statement s);
}

public interface EdgeExtractor {
    boolean canHandle(Statement s);
    List<JoinEdge> extract(Statement s);
}

public interface DiagramSerializer {
    DiagramResponseDto serialize(QueryGraph graph);
}
```

Each service implements only what it needs. `SqlAnalyzerService` depends only on `SqlParser` and `DiagramModelBuilder`, not on export or history.

### D — Dependency Inversion Principle

**High-level modules depend on abstractions, not on concretions.**

```java
// Application layer — high-level policy
@Service
public class SqlAnalyzerService {
    private final SqlParser parser;              // ← interface, not JSQLParser
    private final DiagramModelBuilder builder;   // ← interface
    private final QueryValidator validator;      // ← interface

    // Spring injects the concrete implementations
    public SqlAnalyzerService(SqlParser parser,
                              DiagramModelBuilder builder,
                              QueryValidator validator) {
        this.parser    = parser;
        this.builder   = builder;
        this.validator = validator;
    }
}

// Infrastructure layer — concrete adapter
@Component
public class JSQLParserAdapter implements SqlParser {
    @Override
    public ParseResult<Statement, ParseError> parse(String sql) {
        try {
            return ParseResult.success(CCJSqlParserUtil.parse(sql));
        } catch (JSQLParserException e) {
            return ParseResult.failure(ParseError.fromException(e));
        }
    }
}
```

**Benefit:** `SqlAnalyzerService` is testable with a mock `SqlParser` — no JSQLParser dependency in tests.

---

## 5. Design Patterns

### 5.1 Factory Pattern — `TableNodeFactory`

Decides which `TableNode` subtype to create from a raw AST expression:

```java
@Component
public class TableNodeFactory {

    public TableNode create(FromItem fromItem) {
        if (fromItem instanceof Table t) {
            return new BaseTableNode(
                t.getName(),
                t.getName(),
                t.getAlias() != null ? t.getAlias().getName() : null
            );
        }
        if (fromItem instanceof SubSelect ss) {
            QueryGraph inner = buildInnerGraph(ss.getSelectBody());
            String alias = ss.getAlias().getName();
            return new SubqueryNode(alias, inner);
        }
        throw new UnsupportedNodeTypeException(fromItem.getClass().getName());
    }
}
```

**Why Factory:** the decision logic is in one place. If JSQLParser changes how it represents tables, only the factory changes.

### 5.2 Strategy Pattern — Layout Algorithms (Frontend)

Different layout algorithms for different query shapes:

```typescript
// TypeScript interface — Strategy contract
interface LayoutStrategy {
  apply(nodes: DiagramNode[], edges: DiagramEdge[]): NodePosition[];
}

// Concrete strategies
class LinearLayoutStrategy implements LayoutStrategy {
  apply(nodes, edges): NodePosition[] {
    // Left-to-right chain ordering
  }
}

class HierarchicalLayoutStrategy implements LayoutStrategy {
  apply(nodes, edges): NodePosition[] {
    // Top-down tree layout, star schemas
  }
}

class CircularLayoutStrategy implements LayoutStrategy {
  apply(nodes, edges): NodePosition[] {
    // Central table surrounded by others
  }
}

// Context — picks strategy based on graph shape
class LayoutEngine {
  private strategy: LayoutStrategy;

  setStrategy(strategy: LayoutStrategy) {
    this.strategy = strategy;
  }

  computeLayout(nodes: DiagramNode[], edges: DiagramEdge[]): NodePosition[] {
    return this.strategy.apply(nodes, edges);
  }

  // Auto-select best strategy
  autoSelect(graph: QueryGraph): void {
    if (graph.isStar())   this.setStrategy(new CircularLayoutStrategy());
    else if (graph.isChain()) this.setStrategy(new LinearLayoutStrategy());
    else                  this.setStrategy(new HierarchicalLayoutStrategy());
  }
}
```

**Why Strategy:** US 1.4 requires "Re-layout" button and multiple layout modes. Adding a new layout = new class, no changes to existing code. Aligns with OCP.

### 5.3 Chain of Responsibility — SQL Validation Pipeline

Before expensive parsing, validate cheaply first:

```java
public abstract class SqlValidator {
    private SqlValidator next;

    public SqlValidator setNext(SqlValidator next) {
        this.next = next;
        return next;
    }

    public final Optional<ParseError> validate(String sql) {
        Optional<ParseError> error = doValidate(sql);
        if (error.isPresent()) return error;
        return next != null ? next.validate(sql) : Optional.empty();
    }

    protected abstract Optional<ParseError> doValidate(String sql);
}

// Concrete validators
public class EmptyQueryValidator extends SqlValidator {
    @Override
    protected Optional<ParseError> doValidate(String sql) {
        if (sql == null || sql.isBlank())
            return Optional.of(ParseError.emptyInput());
        return Optional.empty();
    }
}

public class QueryLengthValidator extends SqlValidator {
    private static final int MAX_LENGTH = 100_000;

    @Override
    protected Optional<ParseError> doValidate(String sql) {
        if (sql.length() > MAX_LENGTH)
            return Optional.of(ParseError.tooLong(sql.length(), MAX_LENGTH));
        return Optional.empty();
    }
}

public class StatementTypeValidator extends SqlValidator {
    @Override
    protected Optional<ParseError> doValidate(String sql) {
        String upper = sql.stripLeading().toUpperCase();
        if (!upper.startsWith("SELECT") && !upper.startsWith("WITH"))
            return Optional.of(ParseError.unsupportedStatement());
        return Optional.empty();
    }
}
```

Wired together in Spring config:

```java
@Bean
public SqlValidator validationChain() {
    SqlValidator empty   = new EmptyQueryValidator();
    SqlValidator length  = new QueryLengthValidator();
    SqlValidator type    = new StatementTypeValidator();
    empty.setNext(length).setNext(type);
    return empty;
}
```

**Why Chain:** US 5.1 / FR-7 requires specific error messages for each failure type, checked in order. New validation rule = new link in chain, no existing code changes.

### 5.4 Visitor Pattern — AST Traversal

JSQLParser exposes the Visitor pattern natively. Lean into it:

```java
public class JoinExtractorVisitor extends StatementDeParser {
    private final List<JoinEdge> extractedEdges = new ArrayList<>();
    private final TableNodeFactory nodeFactory;

    @Override
    public void visit(PlainSelect plainSelect) {
        FromItem from = plainSelect.getFromItem();
        List<Join> joins = plainSelect.getJoins();

        if (joins == null) return;

        for (Join join : joins) {
            JoinType type = resolveJoinType(join);
            String condition = join.getOnExpression() != null
                ? join.getOnExpression().toString()
                : "";

            extractedEdges.add(new JoinEdge(
                getNodeId(from),
                getNodeId(join.getRightItem()),
                type,
                condition,
                extractColumns(join.getOnExpression())
            ));
        }
    }

    private JoinType resolveJoinType(Join join) {
        if (join.isInner())  return JoinType.INNER;
        if (join.isLeft())   return JoinType.LEFT;
        if (join.isRight())  return JoinType.RIGHT;
        if (join.isFull())   return JoinType.FULL;
        if (join.isCross())  return JoinType.CROSS;
        return JoinType.INNER; // default
    }
}
```

### 5.5 Composite Pattern — Nested Subqueries

`SubqueryNode` contains an inner `QueryGraph`, which can itself contain `SubqueryNode`s. This is the Composite pattern — tree structure where leaf and container share the same interface:

```java
// Both leaf and composite implement the same base
abstract class TableNode { ... }

// Leaf — no children
class BaseTableNode extends TableNode { }

// Composite — contains a full sub-graph
class SubqueryNode extends TableNode {
    private final QueryGraph innerGraph; // recursive structure

    @Override
    public Optional<QueryGraph> getInnerGraph() {
        return Optional.of(innerGraph);
    }
}
```

The `DiagramDtoMapper` recurses naturally:

```java
public DiagramDto toDto(QueryGraph graph) {
    List<NodeDto> nodes = graph.getNodes().stream()
        .map(node -> {
            NodeDto dto = toNodeDto(node);
            // Recurse into composite nodes
            node.getInnerGraph()
                .map(this::toDto)
                .ifPresent(dto::setInnerDiagram);
            return dto;
        })
        .toList();
    ...
}
```

### 5.6 Command Pattern — Frontend Diagram Actions

For UC 3 (drag, zoom, pan, re-layout), each user action is a command. This enables undo/redo in Phase 4 without redesigning:

```typescript
interface DiagramCommand {
  execute(state: DiagramState): DiagramState;
  undo(state: DiagramState): DiagramState;  // future Phase 4
}

class MoveNodeCommand implements DiagramCommand {
  constructor(
    private nodeId: string,
    private newPosition: Position,
    private previousPosition: Position
  ) {}

  execute(state: DiagramState): DiagramState {
    return { ...state, nodePositions: { ...state.nodePositions, [this.nodeId]: this.newPosition }};
  }

  undo(state: DiagramState): DiagramState {
    return { ...state, nodePositions: { ...state.nodePositions, [this.nodeId]: this.previousPosition }};
  }
}

class ZoomCommand implements DiagramCommand {
  constructor(private delta: number, private center: Position) {}

  execute(state: DiagramState): DiagramState {
    const newZoom = Math.max(0.5, Math.min(2.0, state.zoom + this.delta));
    return { ...state, zoom: newZoom };
  }

  undo(state: DiagramState): DiagramState {
    return { ...state, zoom: state.zoom - this.delta };
  }
}
```

### 5.7 Observer Pattern — State Management (Frontend)

Backed by RxJS `BehaviorSubject`:

```typescript
export interface DiagramState {
  queryGraph: QueryGraph | null;
  nodePositions: Record<string, Position>;
  selectedNodeId: string | null;
  zoom: number;
  panOffset: Position;
  error: ParseError | null;
  isLoading: boolean;
}

@Injectable({ providedIn: 'root' })
export class DiagramStateService {
  private readonly state$ = new BehaviorSubject<DiagramState>(initialState);

  // Selectors — components subscribe to slices
  readonly queryGraph$   = this.state$.pipe(map(s => s.queryGraph),   distinctUntilChanged());
  readonly error$        = this.state$.pipe(map(s => s.error),         distinctUntilChanged());
  readonly zoom$         = this.state$.pipe(map(s => s.zoom),          distinctUntilChanged());
  readonly selectedNode$ = this.state$.pipe(map(s => s.selectedNodeId),distinctUntilChanged());

  dispatch(command: DiagramCommand): void {
    const current = this.state$.getValue();
    const next = command.execute(current);
    this.state$.next(next);
  }
}
```

Components subscribe to only what they need:

```typescript
@Component({ ... })
export class DiagramCanvasComponent {
  readonly graph$ = this.stateService.queryGraph$;
  readonly zoom$  = this.stateService.zoom$;

  constructor(private stateService: DiagramStateService) {}

  onNodeDragged(nodeId: string, pos: Position) {
    // Component only dispatches commands — no direct state mutation
    this.stateService.dispatch(new MoveNodeCommand(nodeId, pos, previousPos));
  }
}
```

### 5.8 Adapter Pattern — JSQLParser Integration

JSQLParser's API is not shaped to match the domain model. The adapter translates it:

```java
// Infrastructure adapter — isolates JSQLParser from domain
@Component
public class JSQLParserAdapter implements SqlParser {

    @Override
    public ParseResult<Statement, ParseError> parse(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            return ParseResult.success(statement);
        } catch (JSQLParserException ex) {
            ParseError error = mapException(ex);
            return ParseResult.failure(error);
        }
    }

    private ParseError mapException(JSQLParserException ex) {
        // Extract line/column from exception message
        // Map library exception to domain error type
        return new ParseError(
            ex.getMessage(),
            extractLine(ex),
            extractColumn(ex)
        );
    }
}
```

---

## 6. Application Layer — Use Cases

Use cases orchestrate the domain, living between domain and presentation:

```java
@Service
public class AnalyzeQueryUseCase {
    private final SqlValidator validationChain;
    private final SqlParser parser;
    private final DiagramModelBuilder builder;

    public ParseResult<QueryGraph, ParseError> execute(String rawSql) {
        // 1. Validate (Chain of Responsibility)
        Optional<ParseError> validationError = validationChain.validate(rawSql);
        if (validationError.isPresent()) {
            return ParseResult.failure(validationError.get());
        }

        // 2. Parse (Adapter hides JSQLParser)
        ParseResult<Statement, ParseError> parsed = parser.parse(rawSql);
        if (!parsed.isSuccess()) {
            return ParseResult.failure(((ParseResult.Failure<Statement, ParseError>) parsed).error());
        }

        Statement statement = ((ParseResult.Success<Statement, ParseError>) parsed).value();

        // 3. Build graph (Open/Closed extensible extractors)
        QueryGraph graph = builder.build(statement);

        return ParseResult.success(graph);
    }
}
```

**The use case is the narrative of the system.** Reading `AnalyzeQueryUseCase.execute()` tells you exactly what the system does, in plain steps.

---

## 7. Presentation Layer — REST Controller

The controller is thin — it only handles HTTP concerns:

```java
@RestController
@RequestMapping("/api/sql")
public class SqlAnalyzeController {

    private final AnalyzeQueryUseCase analyzeQueryUseCase;
    private final DiagramDtoMapper mapper;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@RequestBody @Valid SqlAnalyzeRequest request) {
        ParseResult<QueryGraph, ParseError> result =
            analyzeQueryUseCase.execute(request.getSql());

        if (result instanceof ParseResult.Success<QueryGraph, ParseError> s) {
            DiagramResponseDto dto = mapper.toDto(s.value());

            // NFR: warn on high complexity
            if (s.value().complexity() > 20) {
                dto.setWarning("Very complex query detected. Performance may be impacted.");
            }

            return ResponseEntity.ok(dto);
        }

        ParseError error = ((ParseResult.Failure<QueryGraph, ParseError>) result).error();
        return ResponseEntity.badRequest().body(mapper.toErrorDto(error));
    }
}
```

---

## 8. Frontend Architecture (Angular 17+)

### 8.1 Component Hierarchy — Smart / Dumb Pattern

```
AppComponent (Smart — owns DiagramStateService)
├── SqlInputComponent       (Dumb — emits SqlSubmitEvent)
├── DiagramCanvasComponent  (Dumb — receives graph$, zoom$)
│   └── [JointJS paper host]
├── ControlPanelComponent   (Dumb — emits ZoomEvent, LayoutEvent, ExportEvent)
├── ErrorDisplayComponent   (Dumb — receives error$)
└── HistoryComponent        (Dumb — receives history$, emits HistorySelectEvent)
```

**Why Smart/Dumb:** Dumb components are pure — given same inputs, same output. They are trivially testable and reusable. Business logic is in services, not components.

### 8.2 Service Layer

```
DiagramStateService    — single source of truth (BehaviorSubject<DiagramState>)
SqlAnalyzerApiService  — HTTP POST /api/sql/analyze → maps to domain model
HistoryService         — localStorage reads/writes with FIFO eviction
ExportService          — PNG/SVG export via JointJS paper.toDataURL()
LayoutService          — wraps LayoutEngine, selects and applies strategy
```

### 8.3 Export — Strategy + Interface Segregation

```typescript
interface Exporter {
  export(paper: joint.dia.Paper, filename: string): Promise<void>;
}

class PngExporter implements Exporter {
  async export(paper, filename) {
    const dataUrl = paper.toDataURL({ type: 'image/png', width: 1920, height: 1080 });
    this.triggerDownload(dataUrl, filename + '.png');
  }
}

class SvgExporter implements Exporter {
  async export(paper, filename) {
    const svgDoc = paper.toSVG();
    const blob = new Blob([svgDoc], { type: 'image/svg+xml' });
    this.triggerDownload(URL.createObjectURL(blob), filename + '.svg');
  }
}

@Injectable({ providedIn: 'root' })
export class ExportService {
  private exporters = new Map<string, Exporter>([
    ['png', new PngExporter()],
    ['svg', new SvgExporter()],
  ]);

  export(format: 'png' | 'svg', paper: joint.dia.Paper): Promise<void> {
    const filename = `sqlens_diagram_${this.timestamp()}`;
    const exporter = this.exporters.get(format)!;
    return exporter.export(paper, filename);
  }
}
```

### 8.4 History Service — Encapsulation of localStorage

```typescript
interface QueryHistoryEntry {
  id: string;         // SHA-1 hash of sql
  sql: string;
  preview: string;    // first 100 chars
  timestamp: Date;
}

@Injectable({ providedIn: 'root' })
export class HistoryService {
  private readonly STORAGE_KEY = 'sqlens_history';
  private readonly MAX_ENTRIES = 10;

  save(sql: string): void {
    const existing = this.load();
    const entry: QueryHistoryEntry = {
      id: this.hash(sql),
      sql,
      preview: sql.substring(0, 100),
      timestamp: new Date()
    };

    // FIFO eviction: most recent first, trim to max
    const updated = [entry, ...existing.filter(e => e.id !== entry.id)]
      .slice(0, this.MAX_ENTRIES);

    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(updated));
  }

  load(): QueryHistoryEntry[] {
    const raw = localStorage.getItem(this.STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  }

  clear(): void {
    localStorage.removeItem(this.STORAGE_KEY);
  }

  private hash(sql: string): string {
    // Simple hash for deduplication
    return btoa(sql).substring(0, 16);
  }
}
```

---

## 9. Project & Package Structure

### Maven Multi-Module Layout

```
sqlens\                                  ← root (parent pom.xml)
├── pom.xml                              ← parent POM (packaging=pom)
│                                           inherits spring-boot-starter-parent
│                                           declares 2 modules: frontend, backend
│
├── frontend\                            ← Maven module: sqlens-frontend
│   ├── pom.xml                          ← frontend-maven-plugin builds Angular
│   ├── package.json                     ← Angular project (created by ng new)
│   ├── angular.json
│   ├── src\app\                         ← Angular source
│   └── dist\sqlens-frontend\browser\    ← ng build output (git-ignored)
│
├── backend\
│   └── sqlens-backend\                  ← Maven module: sqlens-backend
│       ├── pom.xml                      ← Spring Boot, depends on sqlens-frontend
│       └── src\main\java\
│           └── org\buulean\sqlensbackend\
│               ├── domain\
│               ├── application\
│               ├── infrastructure\
│               └── presentation\
│
└── doc\                                 ← All documentation
```

**How the single JAR is assembled:**
```
mvn clean package (from sqlens\ root)
  │
  ├─ 1. frontend module:
  │     frontend-maven-plugin installs Node locally → npm ci → ng build
  │     Angular dist/ copied to target/classes/META-INF/resources/
  │     → sqlens-frontend-0.0.1-SNAPSHOT.jar  (contains Angular files)
  │
  └─ 2. backend module:
        depends on sqlens-frontend JAR
        spring-boot-maven-plugin creates fat JAR:
        → sqlens-backend-0.0.1-SNAPSHOT.jar
              ├── Spring Boot classes
              ├── JSQLParser
              └── META-INF/resources/  ← Angular app (auto-served as static)
```

**Result:** One JAR. One `java -jar`. Frontend and backend served on the same port (8080).

---

### Backend Java Package Structure

```
org.buulean.sqlensbackend
├── domain
│   ├── model
│   │   ├── QueryGraph.java
│   │   ├── TableNode.java          (abstract)
│   │   ├── BaseTableNode.java
│   │   ├── SubqueryNode.java
│   │   ├── CteNode.java
│   │   ├── JoinEdge.java
│   │   └── JoinType.java           (enum)
│   ├── result
│   │   ├── ParseResult.java        (sealed interface)
│   │   └── ParseError.java
│   └── port                        (interfaces — Application → Domain)
│       ├── SqlParser.java
│       ├── NodeExtractor.java
│       └── EdgeExtractor.java
│
├── application
│   ├── usecase
│   │   └── AnalyzeQueryUseCase.java
│   ├── service
│   │   ├── SqlAnalyzerService.java
│   │   ├── DiagramModelBuilder.java
│   │   └── QueryValidator.java     (abstract — Chain of Responsibility)
│   └── validation
│       ├── EmptyQueryValidator.java
│       ├── QueryLengthValidator.java
│       └── StatementTypeValidator.java
│
├── infrastructure
│   ├── parser
│   │   ├── JSQLParserAdapter.java  (implements SqlParser)
│   │   └── JoinExtractorVisitor.java
│   ├── extractor
│   │   ├── SelectNodeExtractor.java
│   │   ├── CteNodeExtractor.java
│   │   └── SubqueryNodeExtractor.java
│   └── config
│       └── BeanConfig.java
│
└── presentation
    ├── controller
    │   ├── SqlAnalyzeController.java
    │   └── SpaController.java      (forwards unknown routes → index.html)
    ├── dto
    │   ├── SqlAnalyzeRequest.java
    │   ├── DiagramResponseDto.java
    │   ├── NodeDto.java
    │   ├── EdgeDto.java
    │   └── ErrorResponseDto.java
    └── mapper
        └── DiagramDtoMapper.java
```

### Frontend

```
src/app
├── core
│   ├── services
│   │   ├── diagram-state.service.ts
│   │   ├── sql-analyzer-api.service.ts
│   │   ├── history.service.ts
│   │   ├── export.service.ts
│   │   └── layout.service.ts
│   ├── models
│   │   ├── query-graph.model.ts
│   │   ├── diagram-node.model.ts   (abstract class)
│   │   ├── diagram-edge.model.ts
│   │   ├── join-type.enum.ts
│   │   └── parse-error.model.ts
│   └── commands
│       ├── diagram-command.interface.ts
│       ├── move-node.command.ts
│       ├── zoom.command.ts
│       └── relayout.command.ts
│
├── features
│   ├── sql-input
│   │   └── sql-input.component.ts  (dumb)
│   ├── diagram-canvas
│   │   └── diagram-canvas.component.ts (dumb)
│   ├── control-panel
│   │   └── control-panel.component.ts  (dumb)
│   ├── history
│   │   └── history.component.ts        (dumb)
│   └── error-display
│       └── error-display.component.ts  (dumb)
│
└── app.component.ts                (smart — orchestrates)
```

---

## 10. Key Design Decisions Summary

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Architecture | Clean Architecture (4 layers) | Domain stays pure; infrastructure is swappable |
| Node hierarchy | Abstract class `TableNode` + 3 subtypes | Polymorphism handles rendering uniformly (LSP, OCP) |
| JOIN types | Enum with colour/style fields | Behaviour lives with data; no switch-case sprawl |
| Extractor extensibility | `NodeExtractor` / `EdgeExtractor` interfaces | Adding Phase 4 UNION = new class only (OCP) |
| Error handling | `ParseResult<T,E>` sealed interface | No exception-driven flow; caller must handle both paths |
| Validation | Chain of Responsibility | Ordered, independent, extensible checks |
| Frontend state | BehaviorSubject + Command pattern | Predictable state, future undo/redo ready |
| Layout algorithms | Strategy pattern | Multiple algorithms, easily swappable |
| Export formats | Strategy pattern + Exporter interface | PNG/SVG today, PDF tomorrow = new class |
| Component design | Smart/Dumb split | Dumb components are pure and unit-testable |
| Parser integration | Adapter pattern | JSQLParser is an infrastructure detail, not a domain concern |

---

## 11. What NOT to Do (Anti-patterns to Avoid)

| Anti-pattern | Where it's tempting | Why to avoid |
|---|---|---|
| God service | `SqlService` doing parse + extract + map + validate | Violates SRP; hard to test each part |
| `instanceof` chains | `if (node instanceof CteNode) ...` spread in mapper | Replace with polymorphism (`node.getNodeType()`) |
| Leaking JSQLParser types into domain | Using `PlainSelect` as a domain field | Couples domain to library; blocks parser swap |
| Exception-driven control flow | `throw ParseException` from service to controller | Exceptions are for unexpected failures, not business outcomes |
| Fat Angular components | All logic in `AppComponent` | Untestable; breaks SRP |
| Mutable state in services | Direct property mutation instead of BehaviorSubject | Race conditions; no single source of truth |
| Hardcoded join colors in extractor | `if (type == "LEFT") return "#a84b2f"` | Put in `JoinType` enum; SRP |

---

## References

1. Martin, R. C. (2017). *Clean Architecture: A Craftsman's Guide to Software Structure and Design*. Prentice Hall.
2. Gamma, E. et al. (1994). *Design Patterns: Elements of Reusable Object-Oriented Software*. Addison-Wesley.
3. Martin, R. C. (2003). *Agile Software Development, Principles, Patterns, and Practices*. Prentice Hall.
4. Evans, E. (2003). *Domain-Driven Design: Tackling Complexity in the Heart of Software*. Addison-Wesley.

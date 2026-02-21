# Unit & Integration Test Strategy
## SQLens - SQL Query Structure Visualizer

**Version:** 1.0
**Date:** February 21, 2026
**Backend:** JUnit 5 · Mockito · Spring Boot Test · Testcontainers
**Frontend:** Jest · Angular Testing Library · HttpClientTestingModule

---

## 1. Testing Pyramid

```
         ▲
        /E2E\         52 tests — Playwright
       /──────\       Slow, full-stack, browser
      /  Integ \      ~30 tests — Spring @SpringBootTest / MockMvc
     /──────────\     Medium, API-level, real Spring context
    /    Unit    \    ~120 tests — JUnit 5 + Jest
   /──────────────\   Fast, isolated, no I/O
  ▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔
```

**Rule:** Unit tests run in < 1s each. Integration tests run in < 10s each. E2E tests are the safety net — not the primary coverage tool.

---

## 2. Backend Unit Tests (Java / JUnit 5)

### 2.1 Domain Model Tests

These tests have **zero dependencies** — pure Java, no Spring, no mocks.

#### `JoinTypeTest`

```java
@DisplayName("JoinType enum")
class JoinTypeTest {

    @Test
    @DisplayName("FULL OUTER JOIN is bidirectional")
    void fullJoinIsBidirectional() {
        assertTrue(JoinType.FULL.isBidirectional());
    }

    @ParameterizedTest
    @EnumSource(value = JoinType.class, names = {"INNER", "LEFT", "RIGHT", "CROSS"})
    @DisplayName("Other JOIN types are not bidirectional")
    void nonFullJoinsAreNotBidirectional(JoinType type) {
        assertFalse(type.isBidirectional());
    }

    @Test
    @DisplayName("CROSS JOIN has no condition")
    void crossJoinHasNoCondition() {
        assertFalse(JoinType.CROSS.hasCondition());
    }

    @ParameterizedTest
    @EnumSource(value = JoinType.class, names = {"INNER", "LEFT", "RIGHT", "FULL"})
    @DisplayName("Non-CROSS JOIN types have conditions")
    void nonCrossJoinsHaveConditions(JoinType type) {
        assertTrue(type.hasCondition());
    }

    @Test
    @DisplayName("Each JOIN type has a non-null hex color")
    void allJoinTypesHaveColors() {
        for (JoinType type : JoinType.values()) {
            assertNotNull(type.getColor());
            assertTrue(type.getColor().startsWith("#"));
        }
    }
}
```

#### `TableNodeTest`

```java
@DisplayName("TableNode hierarchy")
class TableNodeTest {

    @Test
    @DisplayName("BaseTableNode display label shows alias in parentheses")
    void baseTableDisplayLabelWithAlias() {
        TableNode node = new BaseTableNode("orders", "orders", "o");
        assertEquals("orders (o)", node.getDisplayLabel());
    }

    @Test
    @DisplayName("BaseTableNode display label shows name only when no alias")
    void baseTableDisplayLabelWithoutAlias() {
        TableNode node = new BaseTableNode("orders", "orders", null);
        assertEquals("orders", node.getDisplayLabel());
    }

    @Test
    @DisplayName("BaseTableNode has no inner graph")
    void baseTableHasNoInnerGraph() {
        TableNode node = new BaseTableNode("t", "t", null);
        assertTrue(node.getInnerGraph().isEmpty());
    }

    @Test
    @DisplayName("SubqueryNode has an inner graph")
    void subqueryNodeHasInnerGraph() {
        QueryGraph inner = new QueryGraph.Builder().build();
        TableNode node = new SubqueryNode("alias", inner);
        assertTrue(node.getInnerGraph().isPresent());
        assertSame(inner, node.getInnerGraph().get());
    }

    @Test
    @DisplayName("CteNode type is 'cte'")
    void cteNodeType() {
        CteNode node = new CteNode("active_orders", false);
        assertEquals("cte", node.getNodeType());
    }

    @Test
    @DisplayName("CteNode correctly reports recursive flag")
    void cteNodeRecursiveFlag() {
        CteNode recursive = new CteNode("hierarchy", true);
        CteNode nonRecursive = new CteNode("flat", false);
        assertTrue(recursive.isRecursive());
        assertFalse(nonRecursive.isRecursive());
    }
}
```

#### `QueryGraphTest`

```java
@DisplayName("QueryGraph aggregate")
class QueryGraphTest {

    @Test
    @DisplayName("Builder creates immutable node list")
    void builderCreatesImmutableNodes() {
        TableNode n1 = new BaseTableNode("orders", "orders", "o");
        TableNode n2 = new BaseTableNode("customers", "customers", "c");

        QueryGraph graph = new QueryGraph.Builder()
            .addNode(n1)
            .addNode(n2)
            .build();

        assertEquals(2, graph.getNodes().size());
        assertThrows(UnsupportedOperationException.class,
            () -> graph.getNodes().add(new BaseTableNode("x", "x", null)));
    }

    @Test
    @DisplayName("hasCartesianProducts detects isolated node")
    void detectsCartesianProduct() {
        TableNode orders    = new BaseTableNode("orders", "orders", "o");
        TableNode customers = new BaseTableNode("customers", "customers", "c");
        TableNode orphan    = new BaseTableNode("orphan", "orphan", null);

        JoinEdge edge = new JoinEdge("orders", "customers", JoinType.INNER, "o.cid = c.id", List.of());

        QueryGraph graph = new QueryGraph.Builder()
            .addNode(orders).addNode(customers).addNode(orphan)
            .addEdge(edge)
            .build();

        assertTrue(graph.hasCartesianProducts());
    }

    @Test
    @DisplayName("hasCartesianProducts returns false when all nodes are connected")
    void noCartesianProductWhenFullyConnected() {
        TableNode orders    = new BaseTableNode("orders", "orders", "o");
        TableNode customers = new BaseTableNode("customers", "customers", "c");

        JoinEdge edge = new JoinEdge("orders", "customers", JoinType.INNER, "o.cid = c.id", List.of());

        QueryGraph graph = new QueryGraph.Builder()
            .addNode(orders).addNode(customers)
            .addEdge(edge)
            .build();

        assertFalse(graph.hasCartesianProducts());
    }
}
```

---

### 2.2 Validation Chain Tests

```java
@DisplayName("SQL Validation Chain")
class SqlValidationChainTest {

    private SqlValidator chain;

    @BeforeEach
    void setup() {
        SqlValidator empty  = new EmptyQueryValidator();
        SqlValidator length = new QueryLengthValidator();
        SqlValidator type   = new StatementTypeValidator();
        empty.setNext(length).setNext(type);
        chain = empty;
    }

    @Test
    @DisplayName("null input returns empty input error")
    void nullInput() {
        var result = chain.validate(null);
        assertTrue(result.isPresent());
        assertEquals("EMPTY_INPUT", result.get().getCode());
    }

    @Test
    @DisplayName("blank input returns empty input error")
    void blankInput() {
        var result = chain.validate("   ");
        assertTrue(result.isPresent());
        assertEquals("EMPTY_INPUT", result.get().getCode());
    }

    @Test
    @DisplayName("query exceeding 100,000 chars returns length error")
    void queryTooLong() {
        String long_sql = "SELECT * FROM t -- " + "x".repeat(100_001);
        var result = chain.validate(long_sql);
        assertTrue(result.isPresent());
        assertEquals("QUERY_TOO_LONG", result.get().getCode());
    }

    @Test
    @DisplayName("INSERT statement returns unsupported type error")
    void insertStatement() {
        var result = chain.validate("INSERT INTO orders VALUES (1)");
        assertTrue(result.isPresent());
        assertEquals("UNSUPPORTED_STATEMENT", result.get().getCode());
    }

    @Test
    @DisplayName("valid SELECT query passes all validators")
    void validSelectQuery() {
        var result = chain.validate("SELECT * FROM orders");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("WITH clause (CTE) passes all validators")
    void validCteQuery() {
        var result = chain.validate("WITH cte AS (SELECT * FROM t) SELECT * FROM cte");
        assertTrue(result.isEmpty());
    }
}
```

---

### 2.3 Extractor Tests

```java
@DisplayName("SelectNodeExtractor")
class SelectNodeExtractorTest {

    private SelectNodeExtractor extractor;
    private TableNodeFactory factory;

    @BeforeEach
    void setup() {
        factory   = new TableNodeFactory();
        extractor = new SelectNodeExtractor(factory);
    }

    @Test
    @DisplayName("extracts two tables from simple JOIN query")
    void extractsTwoTables() throws Exception {
        Statement stmt = CCJSqlParserUtil.parse(
            "SELECT * FROM orders o INNER JOIN customers c ON o.id = c.id"
        );

        List<TableNode> nodes = extractor.extract(stmt);

        assertEquals(2, nodes.size());
        assertTrue(nodes.stream().anyMatch(n -> n.getName().equals("orders")));
        assertTrue(nodes.stream().anyMatch(n -> n.getName().equals("customers")));
    }

    @Test
    @DisplayName("extracts alias correctly")
    void extractsAlias() throws Exception {
        Statement stmt = CCJSqlParserUtil.parse(
            "SELECT * FROM orders o"
        );

        List<TableNode> nodes = extractor.extract(stmt);

        assertEquals(1, nodes.size());
        assertEquals("o", nodes.get(0).getAlias());
    }

    @Test
    @DisplayName("canHandle returns true for SELECT statement")
    void canHandleSelect() throws Exception {
        Statement stmt = CCJSqlParserUtil.parse("SELECT * FROM t");
        assertTrue(extractor.canHandle(stmt));
    }

    @Test
    @DisplayName("canHandle returns false for INSERT statement")
    void cannotHandleInsert() throws Exception {
        Statement stmt = CCJSqlParserUtil.parse("INSERT INTO t VALUES (1)");
        assertFalse(extractor.canHandle(stmt));
    }
}

@DisplayName("JoinExtractorVisitor")
class JoinExtractorVisitorTest {

    private JoinExtractorVisitor visitor;

    @BeforeEach
    void setup() {
        visitor = new JoinExtractorVisitor();
    }

    @Test
    @DisplayName("extracts INNER JOIN edge with correct type and condition")
    void extractsInnerJoin() throws Exception {
        Statement stmt = CCJSqlParserUtil.parse(
            "SELECT * FROM orders o INNER JOIN customers c ON o.customer_id = c.id"
        );
        stmt.accept(visitor);

        List<JoinEdge> edges = visitor.getExtractedEdges();
        assertEquals(1, edges.size());
        assertEquals(JoinType.INNER, edges.get(0).getJoinType());
        assertEquals("o.customer_id = c.id", edges.get(0).getCondition());
    }

    @Test
    @DisplayName("extracts multiple JOIN edges preserving types")
    void extractsMultipleJoins() throws Exception {
        Statement stmt = CCJSqlParserUtil.parse("""
            SELECT * FROM orders o
            INNER JOIN customers c ON o.customer_id = c.id
            LEFT  JOIN payments  p ON p.order_id    = o.id
        """);
        stmt.accept(visitor);

        List<JoinEdge> edges = visitor.getExtractedEdges();
        assertEquals(2, edges.size());

        Map<JoinType, JoinEdge> byType = edges.stream()
            .collect(Collectors.toMap(JoinEdge::getJoinType, e -> e));

        assertNotNull(byType.get(JoinType.INNER));
        assertNotNull(byType.get(JoinType.LEFT));
    }
}
```

---

### 2.4 Use Case Tests (with Mocks)

```java
@DisplayName("AnalyzeQueryUseCase")
@ExtendWith(MockitoExtension.class)
class AnalyzeQueryUseCaseTest {

    @Mock SqlValidator validationChain;
    @Mock SqlParser parser;
    @Mock DiagramModelBuilder builder;

    @InjectMocks
    AnalyzeQueryUseCase useCase;

    @Test
    @DisplayName("returns failure when validation fails")
    void returnsFailureOnValidationError() {
        ParseError error = ParseError.emptyInput();
        when(validationChain.validate(any())).thenReturn(Optional.of(error));

        var result = useCase.execute("");

        assertFalse(result.isSuccess());
        verifyNoInteractions(parser, builder);
    }

    @Test
    @DisplayName("returns failure when parser fails")
    void returnsFailureOnParseError() {
        when(validationChain.validate(any())).thenReturn(Optional.empty());
        ParseError parseError = new ParseError("Syntax error", 1, 5);
        when(parser.parse(any())).thenReturn(ParseResult.failure(parseError));

        var result = useCase.execute("INVALID SQL");

        assertFalse(result.isSuccess());
        verifyNoInteractions(builder);
    }

    @Test
    @DisplayName("returns success with QueryGraph on valid input")
    void returnsSuccessOnValidInput() throws Exception {
        Statement stmt = CCJSqlParserUtil.parse("SELECT * FROM orders");
        QueryGraph graph = new QueryGraph.Builder()
            .addNode(new BaseTableNode("orders", "orders", null))
            .build();

        when(validationChain.validate(any())).thenReturn(Optional.empty());
        when(parser.parse(any())).thenReturn(ParseResult.success(stmt));
        when(builder.build(stmt)).thenReturn(graph);

        var result = useCase.execute("SELECT * FROM orders");

        assertTrue(result.isSuccess());
    }
}
```

---

### 2.5 Mapper Tests

```java
@DisplayName("DiagramDtoMapper")
class DiagramDtoMapperTest {

    private DiagramDtoMapper mapper = new DiagramDtoMapper();

    @Test
    @DisplayName("maps BaseTableNode to NodeDto with correct type")
    void mapsBaseTableNode() {
        TableNode node = new BaseTableNode("orders", "orders", "o");
        NodeDto dto = mapper.toNodeDto(node);

        assertEquals("orders", dto.getId());
        assertEquals("orders (o)", dto.getLabel());
        assertEquals("table", dto.getType());
        assertNull(dto.getInnerDiagram());
    }

    @Test
    @DisplayName("maps CteNode to NodeDto with cte type")
    void mapsCteNode() {
        CteNode node = new CteNode("active_orders", false);
        NodeDto dto = mapper.toNodeDto(node);

        assertEquals("cte", dto.getType());
    }

    @Test
    @DisplayName("maps SubqueryNode with nested diagram")
    void mapsSubqueryNodeWithInnerDiagram() {
        QueryGraph inner = new QueryGraph.Builder()
            .addNode(new BaseTableNode("orders", "orders", null))
            .build();
        SubqueryNode node = new SubqueryNode("o", inner);

        NodeDto dto = mapper.toNodeDto(node);

        assertEquals("subquery", dto.getType());
        assertNotNull(dto.getInnerDiagram());
        assertEquals(1, dto.getInnerDiagram().getNodes().size());
    }

    @Test
    @DisplayName("maps JoinEdge with all JOIN type colors")
    void mapsJoinEdgeColors() {
        for (JoinType type : JoinType.values()) {
            JoinEdge edge = new JoinEdge("a", "b", type, "a.id = b.id", List.of());
            EdgeDto dto = mapper.toEdgeDto(edge);

            assertEquals(type.getColor(), dto.getColor());
            assertEquals(type.name(), dto.getJoinType());
        }
    }
}
```

---

## 3. Backend Integration Tests (Spring Boot)

These tests start the full Spring context and test the HTTP API layer.

### 3.1 Controller Integration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("SqlAnalyzeController Integration")
class SqlAnalyzeControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/sql/analyze returns 200 with nodes and edges for valid query")
    void analyzeSimpleQuery() throws Exception {
        String sql = "SELECT * FROM orders o INNER JOIN customers c ON o.customer_id = c.id";

        mockMvc.perform(post("/api/sql/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sql", sql))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nodes").isArray())
            .andExpect(jsonPath("$.nodes.length()").value(2))
            .andExpect(jsonPath("$.edges.length()").value(1))
            .andExpect(jsonPath("$.edges[0].joinType").value("INNER"))
            .andExpect(jsonPath("$.edges[0].condition").value("o.customer_id = c.id"));
    }

    @Test
    @DisplayName("POST /api/sql/analyze returns 400 with error details for syntax error")
    void analyzeSyntaxError() throws Exception {
        mockMvc.perform(post("/api/sql/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sql", "SELECT * FORM orders"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/sql/analyze returns 400 for empty input")
    void analyzeEmptyInput() throws Exception {
        mockMvc.perform(post("/api/sql/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sql", ""))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("EMPTY_INPUT"));
    }

    @Test
    @DisplayName("POST /api/sql/analyze returns nodes with correct types for CTE query")
    void analyzeCteQuery() throws Exception {
        String sql = """
            WITH active_orders AS (SELECT * FROM orders WHERE status = 'active')
            SELECT ao.id, c.name
            FROM active_orders ao
            JOIN customers c ON ao.customer_id = c.id
        """;

        mockMvc.perform(post("/api/sql/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sql", sql))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nodes[?(@.type == 'cte')]").exists())
            .andExpect(jsonPath("$.nodes[?(@.type == 'table')]").exists());
    }

    @Test
    @DisplayName("Response includes complexity warning when nodes > 20")
    void includesComplexityWarningForLargeQuery() throws Exception {
        // Build a 22-table query
        StringBuilder sql = new StringBuilder("SELECT * FROM t0");
        for (int i = 1; i <= 21; i++) {
            sql.append(String.format(" JOIN t%d ON t%d.id = t%d.fk", i, i-1, i));
        }

        mockMvc.perform(post("/api/sql/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sql", sql.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.warning").exists())
            .andExpect(jsonPath("$.warning").value(containsString("complex")));
    }

    @Test
    @DisplayName("CORS headers are present in response")
    void corsHeadersPresent() throws Exception {
        mockMvc.perform(options("/api/sql/analyze")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("Response time under 1 second for 10-table query")
    void responseTimeWithinLimit() throws Exception {
        // Build 10-table query
        StringBuilder sql = new StringBuilder("SELECT * FROM t0");
        for (int i = 1; i < 10; i++) {
            sql.append(String.format(" JOIN t%d ON t%d.id = t%d.fk", i, i-1, i));
        }

        long start = System.currentTimeMillis();
        mockMvc.perform(post("/api/sql/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sql", sql.toString()))))
            .andExpect(status().isOk());
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration < 1_000, "Response took " + duration + "ms, expected < 1000ms");
    }
}
```

---

## 4. Frontend Unit Tests (Jest + Angular Testing Library)

### 4.1 Service Tests

#### `DiagramStateService`

```typescript
// diagram-state.service.spec.ts
describe('DiagramStateService', () => {
  let service: DiagramStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DiagramStateService);
  });

  it('initial state has null graph and no error', () => {
    service.queryGraph$.pipe(take(1)).subscribe(graph => {
      expect(graph).toBeNull();
    });
    service.error$.pipe(take(1)).subscribe(error => {
      expect(error).toBeNull();
    });
  });

  it('dispatching MoveNodeCommand updates node position', () => {
    const cmd = new MoveNodeCommand('orders', { x: 200, y: 100 }, { x: 0, y: 0 });
    service.dispatch(cmd);

    const state = (service as any).state$.getValue();
    expect(state.nodePositions['orders']).toEqual({ x: 200, y: 100 });
  });

  it('dispatching ZoomCommand clamps to min 50%', () => {
    // Zoom out way too much
    for (let i = 0; i < 50; i++) {
      service.dispatch(new ZoomCommand(-0.1, { x: 400, y: 300 }));
    }

    service.zoom$.pipe(take(1)).subscribe(zoom => {
      expect(zoom).toBeGreaterThanOrEqual(0.5);
    });
  });

  it('dispatching ZoomCommand clamps to max 200%', () => {
    for (let i = 0; i < 50; i++) {
      service.dispatch(new ZoomCommand(0.1, { x: 400, y: 300 }));
    }

    service.zoom$.pipe(take(1)).subscribe(zoom => {
      expect(zoom).toBeLessThanOrEqual(2.0);
    });
  });
});
```

#### `HistoryService`

```typescript
// history.service.spec.ts
describe('HistoryService', () => {
  let service: HistoryService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(HistoryService);
  });

  it('saves a query to localStorage', () => {
    service.save('SELECT * FROM orders');
    const entries = service.load();
    expect(entries).toHaveLength(1);
    expect(entries[0].preview).toContain('SELECT * FROM orders');
  });

  it('limits stored entries to 10 (FIFO eviction)', () => {
    for (let i = 0; i < 15; i++) {
      service.save(`SELECT * FROM table_${i}`);
    }
    const entries = service.load();
    expect(entries).toHaveLength(10);
    // Most recent entry is first
    expect(entries[0].preview).toContain('table_14');
  });

  it('clear() removes all entries from localStorage', () => {
    service.save('SELECT * FROM orders');
    service.clear();
    expect(service.load()).toHaveLength(0);
    expect(localStorage.getItem('sqlens_history')).toBeNull();
  });

  it('deduplicates identical queries', () => {
    service.save('SELECT * FROM orders');
    service.save('SELECT * FROM orders');
    expect(service.load()).toHaveLength(1);
  });
});
```

#### `SqlAnalyzerApiService`

```typescript
// sql-analyzer-api.service.spec.ts
describe('SqlAnalyzerApiService', () => {
  let service: SqlAnalyzerApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(SqlAnalyzerApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('POST to /api/sql/analyze with correct body', () => {
    const sql = 'SELECT * FROM orders';
    service.analyze(sql).subscribe();

    const req = httpMock.expectOne('/api/sql/analyze');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ sql });
    req.flush({ nodes: [], edges: [] });
  });

  it('maps response to QueryGraph model', () => {
    const mockResponse = {
      nodes: [{ id: 'orders', label: 'orders (o)', type: 'table', backgroundColor: '#fff' }],
      edges: [{ from: 'orders', to: 'customers', joinType: 'INNER', condition: 'o.cid = c.id' }],
    };

    let result: QueryGraph | undefined;
    service.analyze('SELECT * FROM orders o JOIN customers c ON o.cid = c.id')
      .subscribe(g => result = g);

    httpMock.expectOne('/api/sql/analyze').flush(mockResponse);

    expect(result!.nodes).toHaveLength(1);
    expect(result!.edges).toHaveLength(1);
  });
});
```

---

### 4.2 Component Tests

#### `SqlInputComponent`

```typescript
// sql-input.component.spec.ts
describe('SqlInputComponent', () => {
  let fixture: ComponentFixture<SqlInputComponent>;
  let component: SqlInputComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SqlInputComponent, ReactiveFormsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(SqlInputComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('emits SqlSubmitEvent when Visualize is clicked', () => {
    const spy = jest.fn();
    component.sqlSubmit.subscribe(spy);

    const textarea = fixture.nativeElement.querySelector('[data-testid="sql-input"]');
    textarea.value = 'SELECT * FROM orders';
    textarea.dispatchEvent(new Event('input'));

    fixture.nativeElement.querySelector('[data-testid="visualize-btn"]').click();
    expect(spy).toHaveBeenCalledWith('SELECT * FROM orders');
  });

  it('does not emit when input is empty', () => {
    const spy = jest.fn();
    component.sqlSubmit.subscribe(spy);

    fixture.nativeElement.querySelector('[data-testid="visualize-btn"]').click();
    expect(spy).not.toHaveBeenCalled();
  });
});
```

#### `ErrorDisplayComponent`

```typescript
// error-display.component.spec.ts
describe('ErrorDisplayComponent', () => {
  it('displays error message when error input is set', () => {
    const fixture = TestBed.createComponent(ErrorDisplayComponent);
    fixture.componentInstance.error = {
      message: 'Syntax error at line 1, column 15',
      line: 1,
      column: 15,
    };
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('[data-testid="error-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('line 1');
  });

  it('is hidden when error is null', () => {
    const fixture = TestBed.createComponent(ErrorDisplayComponent);
    fixture.componentInstance.error = null;
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('[data-testid="error-banner"]');
    expect(banner).toBeNull();
  });
});
```

---

## 5. Test Coverage Targets

| Layer | Tool | Target |
|-------|------|--------|
| Domain model | JUnit 5 | 100% |
| Validation chain | JUnit 5 | 100% |
| Extractors | JUnit 5 | 90% |
| Use cases | JUnit 5 + Mockito | 95% |
| Mapper | JUnit 5 | 90% |
| REST controller (integration) | MockMvc | 85% |
| Angular services | Jest | 90% |
| Angular components | Angular Testing Library | 80% |
| E2E (acceptance) | Playwright | All P0+P1 stories |

---

## 6. Test Naming Convention

**Backend (JUnit 5):**
```
[unit under test]_[given condition]_[expected result]
OR use @DisplayName with natural language sentences
```

**Frontend (Jest):**
```
describe('[ComponentOrService]', () => {
  it('[should do X when Y]', ...)
})
```

---

## 7. What NOT to Test

| Avoid | Reason |
|-------|--------|
| Testing private methods directly | Test through public API; if private method is complex, extract a class |
| Testing Spring annotations (`@Service`, `@Autowired`) | Framework is trusted |
| Testing JSQLParser's own parsing | It has its own test suite |
| Testing JointJS rendering internals | Library is trusted; test behaviour from user perspective |
| 100% line coverage at cost of test quality | Meaningless coverage is worse than no test |

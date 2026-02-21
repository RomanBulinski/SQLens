# Product Requirements Document (PRD)
## SQLens - SQL Query Structure Visualizer

**Version:** 1.0
**Date:** February 21, 2026
**Author:** Roman Bulinski
**Status:** Draft

---

## Executive Summary

### Product Vision

SQLens is a web-based tool that transforms complex SQL queries into interactive visual diagrams, showing table relationships and JOIN structures without requiring database connectivity. The tool addresses a critical gap in the developer workflow by enabling rapid understanding of query structure during code reviews, debugging, and documentation.

### Problem Statement

Developers frequently encounter complex SQL queries containing multiple JOINs, subqueries, and CTEs that are difficult to comprehend from text alone. Existing solutions either require active database connections (ERD tools) or focus on execution plans rather than query structure. This creates friction in code review processes, onboarding new team members, and debugging production issues where database access may be restricted.

### Goals and Success Metrics

> *Table 1: Product goals and success metrics*

---

## Target Users

### Primary Persona: Backend Developer
- **Background:** 3-7 years experience in Java/Spring development
- **Context:** Reviews PRs containing complex ORM-generated or hand-written SQL
- **Pain Points:** Struggles to visualize query flow, especially with 5+ JOINs
- **Goals:** Quickly understand query structure without connecting to database
- **Technical Environment:** IntelliJ IDEA, Spring Boot, PostgreSQL/MySQL

### Secondary Persona: Database Administrator
- **Background:** Senior DBA managing multiple production databases
- **Context:** Analyzes slow queries reported by monitoring tools
- **Pain Points:** Query logs contain complex SQL that's hard to parse mentally
- **Goals:** Identify problematic JOIN patterns and missing indexes
- **Technical Environment:** DBeaver, MySQL Workbench, monitoring dashboards

### Tertiary Persona: Technical Writer / Analyst
- **Background:** Documents system architecture and data flows
- **Context:** Needs to include query structure diagrams in documentation
- **Pain Points:** Manual diagram creation in draw.io is time-consuming
- **Goals:** Auto-generate diagrams from SQL for technical documentation
- **Technical Environment:** Confluence, Google Docs, Markdown editors

---

## Product Architecture

### Technology Stack

**Backend:**
- Framework: Spring Boot 3.x
- Language: Java 17+
- SQL Parser: JSQLParser (Apache 2.0 license)
- API: RESTful JSON endpoints

**Frontend:**
- Framework: Angular 17+
- Diagramming: JointJS Core (Mozilla MPL 2.0)
- State Management: RxJS
- UI Components: Angular Material

### System Architecture

> *Figure 1: High-level system architecture showing frontend-backend interaction*

**Data Flow:**
1. User pastes SQL query into Angular textarea component
2. Frontend sends POST request to `/api/sql/analyze`
3. Spring Backend parses SQL using JSQLParser
4. Backend extracts nodes (tables) and edges (JOINs) into JSON
5. Frontend receives JSON and renders diagram using JointJS
6. User interacts with diagram (drag, zoom, export)

### Core Components

**Backend Services:**
- `SqlAnalyzerService`: Orchestrates parsing and extraction logic
- `SqlParserService`: Wraps JSQLParser, handles exceptions
- `TableExtractor`: Identifies tables, aliases, subqueries
- `JoinExtractor`: Extracts JOIN types, conditions, column mappings
- `DiagramModelBuilder`: Converts AST to frontend JSON format

**Frontend Components:**
- `SqlInputComponent`: Textarea with syntax highlighting
- `DiagramCanvasComponent`: JointJS rendering container
- `ControlPanelComponent`: Zoom, layout, export controls
- `ErrorDisplayComponent`: Shows parsing errors with suggestions
- `HistoryComponent`: Local storage of recent queries

---

## Functional Requirements

### FR-1: SQL Query Input
**Description:** User can paste SQL query text into dedicated input field.

**Acceptance Criteria:**
- Input field accepts queries up to 100,000 characters
- Syntax highlighting for SQL keywords (optional for MVP)
- Paste from clipboard works in all major browsers
- Multi-line editing with line numbers

### FR-2: Query Parsing
**Description:** System parses SQL and extracts structural components.

**Supported SQL Features:**
- SELECT statements with FROM clause
- INNER, LEFT, RIGHT, FULL OUTER, CROSS JOINs
- Table aliases (explicit and implicit)
- Subqueries in FROM, WHERE, SELECT clauses
- Common Table Expressions (WITH clauses)
- UNION/UNION ALL operations

**Unsupported (Phase 1):**
- INSERT/UPDATE/DELETE statements
- Database-specific syntax (Oracle CONNECT BY, etc.)

### FR-3: Diagram Generation
**Description:** System generates interactive visual diagram from parsed SQL.

**Diagram Elements:**

*Nodes (Tables):*
- Rectangle shape with rounded corners
- Header showing table name and alias
- Background color indicates table type (base table vs. subquery)
- Draggable by user

*Edges (JOINs):*
- Directed arrow from source to target table
- Label showing JOIN type (INNER, LEFT, etc.)
- Tooltip displaying ON condition
- Color-coded by JOIN type

### FR-4: Auto-Layout
**Description:** System automatically positions nodes to minimize edge crossings.

**Acceptance Criteria:**
- Initial layout uses directed graph algorithm
- Layout completes within 2 seconds for queries with 10 tables
- User can manually adjust positions after auto-layout
- Re-layout button available to reset positions

### FR-5: Diagram Interaction
**Description:** User can interact with generated diagram.

**Interactions:**
- Drag nodes to reposition
- Zoom in/out (mouse wheel, pinch gesture)
- Pan canvas (mouse drag, touch drag)
- Click edge to highlight ON condition
- Click node to highlight all connected edges

### FR-6: Export Functionality
**Description:** User can export diagram to image formats.

**Supported Formats:**
- PNG (default, 1920x1080 resolution)
- SVG (vector format for documentation)

**Acceptance Criteria:**
- Export preserves current zoom and pan state
- Filename defaults to `sqlens_diagram_YYYYMMDD.png`
- Export completes within 3 seconds

### FR-7: Error Handling
**Description:** System provides clear feedback for invalid SQL.

**Error Scenarios:**
- Syntax errors: Show line number and expected token
- Unsupported features: Explain what's not supported
- Empty input: Prompt user to paste SQL

**Acceptance Criteria:**
- Error messages display within 500ms of input
- Errors include suggestions for common mistakes
- User can correct and re-submit without page refresh

### FR-8: Query History
**Description:** System stores recent queries in browser local storage.

**Acceptance Criteria:**
- Last 10 queries saved automatically
- History persists across browser sessions
- User can click history item to reload query
- Clear history button available

---

## Non-Functional Requirements

### NFR-1: Performance
- Query parsing completes within 1 second for queries up to 50 tables
- Diagram rendering completes within 2 seconds
- Frontend bundle size under 2 MB (gzipped)

### NFR-2: Browser Compatibility
- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

### NFR-3: Accessibility
- Keyboard navigation for all interactive elements
- ARIA labels for screen readers
- Color contrast ratio 4.5:1 minimum
- Focus indicators visible

### NFR-4: Security
- No SQL execution (parser only, no database connection)
- Input sanitization for XSS prevention
- CORS policy configured for API endpoints
- HTTPS required in production

### NFR-5: Scalability
- Backend handles 100 concurrent requests
- Stateless API design for horizontal scaling
- CDN delivery for frontend assets

---

## User Interface Design

### Main Screen Layout

> *Figure 2: Main application interface showing split-pane layout*

**Layout Structure:**
- Top navigation bar (logo, settings, help)
- Left panel: SQL input textarea (30% width)
- Right panel: Diagram canvas (70% width)
- Bottom toolbar: Zoom controls, export, layout options

### Color Scheme

**JOIN Type Colors:**
| JOIN Type | Color | Hex |
|-----------|-------|-----|
| INNER JOIN | Teal | `#21808d` |
| LEFT JOIN | Orange | `#a84b2f` |
| RIGHT JOIN | Purple | `#6b21a8` |
| FULL OUTER JOIN | Red | `#c0152f` |
| CROSS JOIN | Gray | `#626c71` |

### Responsive Design
- **Desktop (1920x1080):** Full feature set
- **Tablet (768x1024):** Vertical split-pane
- **Mobile (375x667):** Single pane with toggle button

---

## Development Phases

### Phase 1 - MVP (Weeks 1-4)
- Setup Spring Boot project with JSQLParser dependency
- Implement basic SQL parsing (SELECT with INNER/LEFT JOIN)
- Create REST endpoint `/api/sql/analyze`
- Setup Angular project with JointJS
- Implement SQL input component
- Implement basic diagram rendering
- Deploy to staging environment

**Success Criteria:** Can visualize simple 3-table JOIN queries

### Phase 2 - Extended Features (Weeks 5-7)
- Add support for RIGHT, FULL, CROSS JOINs
- Implement subquery parsing
- Add CTE (WITH clause) support
- Implement auto-layout algorithm
- Add zoom, pan, drag interactions
- Implement error handling UI

**Success Criteria:** Can handle 90% of production SQL queries

### Phase 3 - Polish (Weeks 8-10)
- Add PNG/SVG export
- Implement query history (local storage)
- Add syntax highlighting in input
- Optimize performance for 15+ table queries
- Add keyboard shortcuts
- Write user documentation

**Success Criteria:** Production-ready application with full feature set

### Phase 4 - Advanced (Weeks 11-12)
- Optional database connection for column metadata
- Support for UNION operations
- Custom color themes
- Shareable diagram URLs
- API for programmatic access

**Success Criteria:** Differentiation from basic visualizers

---

## Open Questions and Risks

### Technical Risks

> *Table 2: Technical risk assessment and mitigation strategies*

### Open Questions
1. Should we support stored procedure bodies in Phase 1?
2. Do users need collaborative features (sharing diagrams)?
3. Should we integrate with IDE plugins (IntelliJ, VS Code)?
4. Is there demand for API access for CI/CD pipelines?

---

## Success Criteria

### Phase 1 (MVP) Success
- 50 beta users can visualize their production queries
- 80% of submitted queries parse successfully
- Average diagram generation time under 3 seconds
- Zero critical bugs in core parsing logic

### Phase 3 (Launch) Success
- 500 weekly active users within 3 months
- 90% query success rate across all submitted SQL
- 4.5+ star rating on ProductHunt
- 10+ organic mentions on Reddit/StackOverflow

---

## Appendix A: API Specification

### POST /api/sql/analyze

**Request:**
```json
{
  "sql": "SELECT o.id, c.name FROM orders o LEFT JOIN customers c ON o.customer_id = c.id"
}
```

**Response:**
```json
{
  "nodes": [
    { "id": "orders", "alias": "o", "type": "table" },
    { "id": "customers", "alias": "c", "type": "table" }
  ],
  "edges": [
    {
      "from": "orders",
      "to": "customers",
      "joinType": "LEFT",
      "condition": "o.customer_id = c.id",
      "columns": ["customer_id", "id"]
    }
  ]
}
```

**Error Response (400):**
```json
{
  "error": "ParseException",
  "message": "Syntax error at line 1, column 15: Expected FROM keyword",
  "line": 1,
  "column": 15
}
```

---

## Appendix B: Competitor Analysis

> *Table 3: Competitive feature comparison*

---

## References

1. JSQLParser GitHub Repository. https://github.com/JSQLParser/JSqlParser
2. JointJS Documentation. https://www.jointjs.com
3. Spring Boot Documentation. https://spring.io/projects/spring-boot
4. Angular Framework Documentation. https://angular.io
5. Chisell Labs. (2025). 9 Best Product Requirement Document (PRD) Templates. https://chisellabs.com/blog/product-requirement-document-prd-templates/

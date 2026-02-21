# User Stories Document
## SQLens - SQL Query Structure Visualizer

**Version:** 1.0
**Date:** February 21, 2026
**Sprint Planning:** Phases 1-4

---

## Epic 1: Core Query Visualization

### User Story 1.1: Paste SQL Query

> As a **backend developer**
> I want to **paste a SQL query into an input field**
> So that I can **quickly analyze its structure without setting up a database connection**

**Priority:** Must Have (P0)
**Story Points:** 3
**Sprint:** 1

**Acceptance Criteria:**

- **Given** I am on the SQLens home page
  **When** I click on the SQL input textarea
  **Then** the cursor appears and I can paste text

- **Given** I have a SQL query in my clipboard
  **When** I paste it into the input field (Ctrl+V or Cmd+V)
  **Then** the full query text appears in the textarea

- **Given** I paste a query longer than 100,000 characters
  **When** I try to submit it
  **Then** I see an error message "Query too long (max 100,000 characters)"

**Technical Notes:**
- Use Angular reactive forms for input handling
- Debounce input events (500ms) before triggering analysis
- Store raw SQL in component state

---

### User Story 1.2: Parse Simple JOIN Query

> As a **developer**
> I want to **automatically parse a SQL query with INNER JOIN**
> So that I can **see which tables are involved and how they're connected**

**Priority:** Must Have (P0)
**Story Points:** 8
**Sprint:** 1

**Acceptance Criteria:**

- **Given** I paste a query: `SELECT * FROM orders o INNER JOIN customers c ON o.customer_id = c.id`
  **When** I click "Visualize"
  **Then** I see two table nodes labeled "orders (o)" and "customers (c)"
  **And** I see an edge connecting them labeled "INNER JOIN"
  **And** the edge tooltip shows "o.customer_id = c.id"

- **Given** the query has syntax errors
  **When** I click "Visualize"
  **Then** I see an error message with line number and description
  **And** the diagram area remains empty

- **Given** the query is valid but uses unsupported features
  **When** I click "Visualize"
  **Then** I see a warning message explaining what's not supported
  **And** partial diagram is shown if possible

**Technical Notes:**
- Implement `SqlAnalyzerService` in Spring Boot
- Use JSQLParser to create AST
- Extract tables via `TablesNamesFinder`
- Extract JOINs via `plainSelect.getJoins()`

---

### User Story 1.3: Visualize Multiple JOINs

> As a **database administrator**
> I want to **visualize queries with multiple JOINs (3+ tables)**
> So that I can **understand complex query patterns at a glance**

**Priority:** Must Have (P0)
**Story Points:** 5
**Sprint:** 2

**Acceptance Criteria:**

- **Given** I paste a query with 5 tables and 4 JOINs
  **When** the diagram renders
  **Then** I see 5 distinct table nodes
  **And** 4 edges connecting them according to JOIN order
  **And** each edge is labeled with its JOIN type

- **Given** the query has LEFT and INNER JOINs mixed
  **When** the diagram renders
  **Then** LEFT JOIN edges are orange
  **And** INNER JOIN edges are teal
  **And** colors are distinguishable for colorblind users

- **Given** the query has circular JOIN patterns
  **When** the diagram renders
  **Then** the layout algorithm prevents edge overlap
  **And** I can still distinguish the flow

**Technical Notes:**
- Implement recursive JOIN extraction
- Handle JOIN chains (A → B → C → D)
- Detect and handle circular references

---

### User Story 1.4: Auto-Layout Diagram

> As a **user**
> I want to **see tables automatically positioned in a readable layout**
> So that I **don't have to manually arrange complex diagrams**

**Priority:** Should Have (P1)
**Story Points:** 8
**Sprint:** 2

**Acceptance Criteria:**

- **Given** a query with 5 tables in a linear JOIN chain
  **When** the diagram renders
  **Then** tables are arranged left-to-right in JOIN order
  **And** edges flow left-to-right with minimal crossings

- **Given** a query with a star schema (1 central table, 4 joined tables)
  **When** the diagram renders
  **Then** the central table is positioned in the middle
  **And** surrounding tables form a circle around it

- **Given** any diagram
  **When** I click the "Re-layout" button
  **Then** nodes return to auto-calculated positions
  **And** my manual adjustments are discarded

- **Given** a query with 10+ tables
  **When** the diagram renders
  **Then** layout completes within 2 seconds
  **And** the result is reasonably readable

**Technical Notes:**
- Use JointJS DirectedGraph layout plugin
- Configure rank direction (LR for linear, TB for hierarchical)
- Implement layout presets (linear, hierarchical, circular)

---

## Epic 2: Advanced SQL Features

### User Story 2.1: Support Subqueries

> As a **backend developer**
> I want to **visualize queries containing subqueries in FROM clause**
> So that I can **understand complex nested query structures**

**Priority:** Should Have (P1)
**Story Points:** 13
**Sprint:** 3

**Acceptance Criteria:**

- **Given** a query: `SELECT * FROM (SELECT * FROM orders WHERE status='active') o JOIN customers c ON o.customer_id = c.id`
  **When** the diagram renders
  **Then** I see a node labeled "(subquery) o" with different background color
  **And** this node connects to "customers" node
  **And** I can click the subquery node to see its internal structure

- **Given** a query with nested subqueries (3 levels deep)
  **When** the diagram renders
  **Then** each subquery level is visually distinguished
  **And** I can expand/collapse subquery details

- **Given** a query with subquery in WHERE clause
  **When** the diagram renders
  **Then** the subquery is shown as a separate group
  **And** a dotted line connects it to the main query

**Technical Notes:**
- Recursively parse `SelectBody` for subqueries
- Create nested node groups in JointJS
- Store subquery AST for drill-down feature

---

### User Story 2.2: Support CTEs (WITH Clause)

> As a **developer writing complex analytical queries**
> I want to **visualize Common Table Expressions**
> So that I can **see how CTEs flow into the main query**

**Priority:** Should Have (P1)
**Story Points:** 8
**Sprint:** 3

**Acceptance Criteria:**

- **Given** a query: `WITH active_orders AS (SELECT * FROM orders WHERE status='active') SELECT * FROM active_orders JOIN customers ON ...`
  **When** the diagram renders
  **Then** I see a node labeled "active_orders (CTE)"
  **And** this node is visually distinct from regular tables
  **And** it connects to the main query tables

- **Given** a query with multiple CTEs
  **When** the diagram renders
  **Then** all CTEs are shown in a separate "CTEs" group at the top
  **And** arrows show which CTEs feed into the main query

- **Given** a query with recursive CTE
  **When** the diagram renders
  **Then** the CTE node shows a self-referencing edge
  **And** a tooltip explains it's recursive

**Technical Notes:**
- Parse `WithItem` objects from JSQLParser
- Treat CTEs as virtual tables in graph model
- Position CTE nodes above main query nodes

---

### User Story 2.3: Support All JOIN Types

> As a **database administrator**
> I want to **see RIGHT, FULL OUTER, and CROSS JOINs visualized**
> So that I have **complete coverage of JOIN types**

**Priority:** Should Have (P1)
**Story Points:** 5
**Sprint:** 3

**Acceptance Criteria:**

- **Given** a query with RIGHT JOIN
  **When** the diagram renders
  **Then** the edge is purple and labeled "RIGHT JOIN"
  **And** the arrow points in the correct direction

- **Given** a query with FULL OUTER JOIN
  **When** the diagram renders
  **Then** the edge is red and labeled "FULL OUTER JOIN"
  **And** the edge has bidirectional arrows

- **Given** a query with CROSS JOIN
  **When** the diagram renders
  **Then** the edge is gray and labeled "CROSS JOIN"
  **And** the edge has a dotted line style

- **Given** a query mixing all JOIN types
  **When** the diagram renders
  **Then** each JOIN type is visually distinguishable
  **And** a legend shows the color mapping

**Technical Notes:**
- Check `join.isRight()`, `join.isFull()`, `join.isCross()`
- Define color constants for each JOIN type
- Implement edge styling in JointJS

---

## Epic 3: User Interaction

### User Story 3.1: Drag and Reposition Tables

> As a **user**
> I want to **drag table nodes to different positions**
> So that I can **customize the layout for my specific needs**

**Priority:** Should Have (P1)
**Story Points:** 3
**Sprint:** 4

**Acceptance Criteria:**

- **Given** a rendered diagram
  **When** I click and hold on a table node
  **Then** the cursor changes to a grab hand
  **And** I can drag the node to a new position

- **Given** I drag a node to a new position
  **When** I release the mouse
  **Then** the node stays in its new position
  **And** connected edges automatically re-route

- **Given** I drag a node outside the visible canvas
  **When** I release the mouse
  **Then** the canvas auto-scrolls to keep the node visible

**Technical Notes:**
- Enable JointJS element dragging
- Implement edge router for automatic path recalculation
- Persist node positions in component state

---

### User Story 3.2: Zoom and Pan Canvas

> As a **user viewing a complex diagram**
> I want to **zoom in/out and pan the canvas**
> So that I can **focus on specific parts of large diagrams**

**Priority:** Should Have (P1)
**Story Points:** 5
**Sprint:** 4

**Acceptance Criteria:**

- **Given** a rendered diagram
  **When** I scroll the mouse wheel up
  **Then** the diagram zooms in (max 200%)
  **And** zoom centers on the mouse cursor position

- **Given** a rendered diagram
  **When** I scroll the mouse wheel down
  **Then** the diagram zooms out (min 50%)
  **And** the entire diagram remains visible

- **Given** a zoomed-in diagram
  **When** I click and drag on empty canvas space
  **Then** the canvas pans in the drag direction
  **And** table nodes move accordingly

- **Given** zoom controls in the toolbar
  **When** I click "Fit to Screen"
  **Then** zoom adjusts so entire diagram is visible
  **And** diagram is centered in the viewport

**Technical Notes:**
- Implement JointJS `PaperScroller` plugin
- Add zoom level indicator (e.g., "100%")
- Store zoom/pan state for export

---

### User Story 3.3: Highlight Related Elements

> As a **user analyzing complex queries**
> I want to **click on a table to highlight all its connections**
> So that I can **quickly see which other tables it interacts with**

**Priority:** Could Have (P2)
**Story Points:** 5
**Sprint:** 5

**Acceptance Criteria:**

- **Given** a rendered diagram with 5+ tables
  **When** I click on a table node
  **Then** that node is highlighted with a thick border
  **And** all edges connected to it are highlighted
  **And** connected table nodes are slightly highlighted
  **And** unrelated elements are dimmed (opacity 30%)

- **Given** a highlighted element
  **When** I click on empty space
  **Then** all highlights are removed
  **And** the diagram returns to normal state

- **Given** a diagram
  **When** I hover over an edge
  **Then** a tooltip appears showing the JOIN condition
  **And** the tooltip follows the mouse cursor

**Technical Notes:**
- Implement click handlers for JointJS elements
- Add/remove CSS classes for highlighting
- Store highlight state in component

---

## Epic 4: Export and History

### User Story 4.1: Export Diagram as PNG

> As a **developer writing documentation**
> I want to **export the diagram as a PNG image**
> So that I can **include it in technical documents**

**Priority:** Should Have (P1)
**Story Points:** 5
**Sprint:** 5

**Acceptance Criteria:**

- **Given** a rendered diagram
  **When** I click "Export as PNG"
  **Then** a file download begins automatically
  **And** the filename is `sqlens_diagram_20260221_0918.png`
  **And** the image resolution is 1920x1080 pixels
  **And** the image includes all visible elements at current zoom

- **Given** a diagram with custom positioning
  **When** I export as PNG
  **Then** the exported image preserves my custom layout
  **And** there is no whitespace cropping

- **Given** I export a diagram with 10+ tables
  **When** the export completes
  **Then** the process takes less than 3 seconds
  **And** all text is readable in the image

**Technical Notes:**
- Use JointJS `paper.toDataURL()` for PNG export
- Set fixed dimensions or allow user selection
- Handle high-DPI displays (2x scaling)

---

### User Story 4.2: Save Query History

> As a **frequent user**
> I want to **see my recently analyzed queries**
> So that I can **quickly re-visualize queries I've worked on**

**Priority:** Could Have (P2)
**Story Points:** 3
**Sprint:** 6

**Acceptance Criteria:**

- **Given** I visualize a SQL query
  **When** the diagram renders successfully
  **Then** the query is automatically saved to history
  **And** history is stored in browser local storage

- **Given** I have 15 queries in history
  **When** I open the history panel
  **Then** I see the 10 most recent queries
  **And** each entry shows first 100 characters + timestamp

- **Given** I click on a history entry
  **When** I select it
  **Then** the SQL input field populates with that query
  **And** the diagram automatically re-renders

- **Given** I click "Clear History"
  **When** I confirm the action
  **Then** all history entries are deleted
  **And** local storage is cleared

**Technical Notes:**
- Implement Angular service for localStorage
- Store query text + timestamp + hash
- Limit to 10 entries (FIFO eviction)

---

## Epic 5: Error Handling and Polish

### User Story 5.1: Display Helpful Error Messages

> As a **user who pastes invalid SQL**
> I want to **see clear error messages with suggestions**
> So that I can **fix my query and try again**

**Priority:** Must Have (P0)
**Story Points:** 5
**Sprint:** 2

**Acceptance Criteria:**

- **Given** I paste SQL with syntax error at line 3
  **When** I click "Visualize"
  **Then** I see an error banner at the top
  **And** the message says "Syntax error at line 3, column 15: Expected 'FROM' keyword"
  **And** line 3 in the input is highlighted

- **Given** I paste a valid INSERT statement
  **When** I click "Visualize"
  **Then** I see a warning message "Only SELECT statements are supported"
  **And** a suggestion to "Try analyzing a SELECT query instead"

- **Given** I submit an empty input field
  **When** I click "Visualize"
  **Then** I see a gentle prompt "Paste a SQL query to get started"
  **And** example queries are shown

**Technical Notes:**
- Catch `JSQLParserException` in backend
- Return structured error with line/column info
- Implement error display component in Angular

---

### User Story 5.2: Optimize Performance for Large Queries

> As a **user analyzing complex production queries**
> I want to **visualize queries with 15+ tables**
> So that the **tool scales to real-world complexity**

**Priority:** Should Have (P1)
**Story Points:** 8
**Sprint:** 6

**Acceptance Criteria:**

- **Given** a query with 15 tables and 14 JOINs
  **When** I click "Visualize"
  **Then** the diagram renders within 3 seconds
  **And** all elements are interactive

- **Given** a query with 20+ tables
  **When** I attempt to visualize
  **Then** I see a warning "Very complex query detected"
  **And** the option to "Visualize anyway" or "Simplify query first"

- **Given** a rendered diagram with 15+ tables
  **When** I drag a node
  **Then** the UI remains responsive (60 FPS)
  **And** edge re-routing completes within 100ms

**Technical Notes:**
- Profile JSQLParser performance, optimize if needed
- Implement diagram complexity warning (>15 tables)
- Consider virtualization for very large diagrams

---

## Definition of Done (DoD)

A user story is considered **Done** when:

- [ ] All acceptance criteria are met and verified
- [ ] Unit tests written with >80% code coverage
- [ ] Integration tests pass for API endpoints
- [ ] Code reviewed and approved by at least one team member
- [ ] Manual testing completed on Chrome, Firefox, Safari
- [ ] No critical or high-priority bugs remain open
- [ ] Documentation updated (README, API docs if applicable)
- [ ] Deployed to staging environment and smoke tested

---

## Story Mapping

> *Table 4: User story priority mapping using MoSCoW method*

| Priority | Stories |
|----------|---------|
| Must Have (P0) | US 1.1, 1.2, 1.3, 5.1 |
| Should Have (P1) | US 1.4, 2.1, 2.2, 2.3, 3.1, 3.2, 4.1, 5.2 |
| Could Have (P2) | US 3.3, 4.2 |

---

## References

1. The Story. (n.d.). User Story Acceptance Criteria. https://thestory.is/en/journal/user-story-acceptance-criteria/
2. AltexSoft. (2023). Acceptance Criteria: Purposes, Types, Examples and Best Practices. https://www.altexsoft.com/blog/acceptance-criteria-purposes-formats-and-best-practices/
3. Intellisoft. (2024). User Story Acceptance Criteria Explained with Examples. https://intellisoft.io/user-story-acceptance-criteria-explained-with-examples/

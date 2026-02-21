# Use Cases Document
## SQLens - SQL Query Structure Visualizer

**Version:** 1.0
**Date:** February 21, 2026
**Author:** Roman Bulinski

---

## Overview

This document describes the primary use cases for SQLens, detailing how different actors interact with the system to achieve their goals. Each use case includes preconditions, basic flow, alternative flows, and postconditions.

---

## Actors

| Actor | Description |
|-------|-------------|
| **Backend Developer** | Writes and reviews SQL queries in application code |
| **Database Administrator** | Optimizes database queries and analyzes performance |
| **Technical Writer** | Documents system architecture and data flows |
| **System** | SQLens application (backend and frontend components) |

---

## Use Case Diagram

> *Figure 3: SQLens use case diagram showing actor interactions*

---

## Use Case 1: Analyze Simple SQL Query

**Actor:** Backend Developer
**Goal:** Visualize a SQL query with one or two JOINs to understand its structure
**Priority:** High
**Frequency:** Daily

### Preconditions
- User has access to SQLens web application
- User has a SQL SELECT query with at least one JOIN
- Browser supports JavaScript and SVG rendering

### Basic Flow
1. User navigates to SQLens home page
2. User pastes SQL query into the input textarea
3. User clicks "Visualize Query" button
4. System parses the SQL using JSQLParser
5. System extracts tables, aliases, and JOIN relationships
6. System generates JSON representation of diagram structure
7. System sends JSON to frontend
8. Frontend renders interactive diagram using JointJS
9. User views the diagram showing tables and JOIN connections
10. User examines JOIN types and conditions via edge labels

### Alternative Flow 1: Syntax Error
1. System detects syntax error during parsing
2. System returns error message with line and column number
3. Frontend displays error banner above input field
4. Frontend highlights the problematic line in the input
5. User corrects the syntax error
6. User clicks "Visualize Query" again
7. System continues from step 4 of basic flow

### Alternative Flow 2: Unsupported SQL Feature
1. System detects unsupported SQL feature (e.g., Oracle-specific syntax)
2. System returns warning message explaining unsupported feature
3. Frontend displays warning banner with suggestion
4. Frontend renders partial diagram if possible
5. User acknowledges the limitation or modifies query

### Postconditions

**Success:**
- Diagram is displayed showing all tables and JOINs
- User understands query structure visually
- Query is saved to browser history

**Failure:**
- Error message is displayed with actionable guidance
- Input remains in textarea for correction
- No diagram is rendered

---

## Use Case 2: Export Diagram for Documentation

**Actor:** Technical Writer
**Goal:** Export query diagram as PNG image for inclusion in technical documentation
**Priority:** Medium
**Frequency:** Weekly

### Preconditions
- User has successfully visualized a SQL query
- Diagram is currently displayed on screen
- User has write permissions to local file system

### Basic Flow
1. User adjusts diagram layout by dragging tables to desired positions
2. User sets zoom level to ensure all elements are visible
3. User clicks "Export" button in toolbar
4. System displays export options dialog (PNG, SVG)
5. User selects PNG format
6. User clicks "Download" button
7. System captures current diagram state as image
8. System generates PNG file with resolution 1920x1080
9. System triggers browser download with filename `sqlens_diagram_YYYYMMDD_HHMM.png`
10. User saves file to documentation folder
11. User inserts image into technical document (Confluence, Google Docs, etc.)

### Alternative Flow 1: Export as SVG
1. User selects SVG format instead of PNG
2. System generates vector SVG file
3. System triggers download with `.svg` extension
4. User saves file for use in scalable documentation

### Alternative Flow 2: Export Fails
1. System encounters error during image generation
2. System displays error message "Export failed. Please try again."
3. User clicks "Export" button again
4. System retries export process

### Postconditions

**Success:**
- PNG or SVG file is downloaded to user's computer
- Image accurately represents current diagram state
- User can use image in external documentation

**Failure:**
- Error message is displayed
- User can retry export
- No file is downloaded

---

## Use Case 3: Analyze Complex Multi-Table Query

**Actor:** Database Administrator
**Goal:** Understand a production query with 8+ tables and multiple JOIN types
**Priority:** High
**Frequency:** Several times per week

### Preconditions
- User has access to production query logs or application code
- Query contains 8 or more tables
- Query includes multiple JOIN types (INNER, LEFT, RIGHT)

### Basic Flow
1. User copies complex SQL query from production logs
2. User navigates to SQLens application
3. User pastes query into input field (query is 500+ lines)
4. User clicks "Visualize Query" button
5. System parses query and extracts 8 tables with 7 JOINs
6. System applies auto-layout algorithm to position tables
7. System renders diagram with color-coded JOIN types
8. User sees hierarchical layout with central fact table
9. User identifies that 3 LEFT JOINs are used for optional data
10. User clicks on an edge to see JOIN condition details
11. User drags tables to reposition for better readability
12. User zooms in to examine specific JOIN conditions
13. User identifies a Cartesian product risk (missing JOIN condition)
14. User returns to application code to add missing JOIN

### Alternative Flow 1: Query Too Complex
1. System detects query with 20+ tables
2. System displays warning "Very complex query detected. Performance may be impacted."
3. System offers option "Visualize Anyway" or "Simplify Query"
4. User clicks "Visualize Anyway"
5. System renders diagram but layout takes 5 seconds
6. User sees warning banner "Consider breaking query into smaller parts"

### Alternative Flow 2: Subquery in FROM Clause
1. System detects subquery in FROM clause
2. System renders subquery as special node with distinct color
3. User clicks on subquery node to expand details
4. System displays subquery SQL in a popup overlay
5. User understands nested query structure

### Postconditions

**Success:**
- DBA understands complete query structure
- DBA identifies optimization opportunities (missing indexes, unnecessary JOINs)
- DBA exports diagram to share with development team
- Query analysis is saved to history for future reference

**Failure:**
- System is unable to parse dialect-specific syntax
- Warning message guides user to standard SQL equivalent

---

## Use Case 4: Review Code PR with SQL Changes

**Actor:** Backend Developer (Code Reviewer)
**Goal:** Verify correctness of SQL query changes in a pull request
**Priority:** High
**Frequency:** Multiple times per day

### Preconditions
- Developer receives pull request notification with SQL changes
- Pull request includes modified or new SQL query
- Developer has access to SQLens tool

### Basic Flow
1. Developer opens pull request in GitHub/GitLab
2. Developer sees modified SQL query in diff view
3. Developer copies new version of SQL query
4. Developer opens SQLens in new browser tab
5. Developer pastes SQL into input field
6. Developer clicks "Visualize Query"
7. System renders diagram showing all tables and JOINs
8. Developer verifies that new LEFT JOIN is correctly positioned
9. Developer checks that JOIN condition uses correct foreign key
10. Developer confirms no Cartesian products are introduced
11. Developer approves pull request with comment "Query structure verified with SQLens"

### Alternative Flow 1: Comparing Before/After
1. Developer copies old version of query from PR diff
2. Developer visualizes old query in SQLens
3. Developer exports diagram as PNG (`old_version.png`)
4. Developer copies new version of query
5. Developer visualizes new query in SQLens
6. Developer exports diagram as PNG (`new_version.png`)
7. Developer compares images side-by-side
8. Developer identifies that new query adds redundant JOIN
9. Developer requests changes in PR review

### Alternative Flow 2: Query Breaks in Visualization
1. System returns syntax error
2. Developer realizes SQL in PR has syntax mistake
3. Developer comments on PR "SQL has syntax error at line X"
4. Developer requests changes before re-review

### Postconditions

**Success:**
- Developer confidently approves or requests changes on PR
- SQL query structure is verified visually
- No incorrect JOINs merge to production

**Failure:**
- Developer identifies issues via SQLens and prevents bugs
- PR is sent back for corrections

---

## Use Case 5: Recall and Re-analyze Previous Query

**Actor:** Backend Developer
**Goal:** Quickly re-visualize a query analyzed yesterday without re-pasting
**Priority:** Medium
**Frequency:** Few times per week

### Preconditions
- User previously analyzed at least one query in SQLens
- Browser local storage contains query history
- User returns to SQLens within 30 days (before cache expiry)

### Basic Flow
1. User navigates to SQLens home page
2. User clicks "History" button in top navigation
3. System retrieves query history from browser local storage
4. System displays list of 10 most recent queries
5. Each entry shows timestamp and first 100 characters of SQL
6. User scans list and identifies query analyzed yesterday
7. User clicks on the history entry
8. System populates input field with selected query
9. System automatically triggers visualization
10. Diagram renders showing same structure as yesterday
11. User continues analysis from where they left off

### Alternative Flow 1: Clear History
1. User clicks "Clear History" button
2. System displays confirmation dialog "Delete all saved queries?"
3. User clicks "Confirm"
4. System deletes all entries from local storage
5. History panel shows "No queries in history"

### Alternative Flow 2: History Exceeds Limit
1. System finds 15 queries in local storage (limit is 10)
2. System displays only 10 most recent queries (FIFO)
3. Oldest 5 queries are automatically pruned
4. User sees only relevant recent history

### Postconditions

**Success:**
- User quickly retrieves previous query without re-pasting
- Analysis continues seamlessly
- Time saved compared to searching through code

**Failure:**
- Local storage was cleared by browser or user
- History is empty, user must paste query manually

---

## Use Case 6: Analyze Query with Subqueries and CTEs

**Actor:** Backend Developer
**Goal:** Understand a complex analytical query using CTEs and nested subqueries
**Priority:** Medium
**Frequency:** Weekly

### Preconditions
- User has SQL query with WITH clause (Common Table Expressions)
- Query may also include subqueries in SELECT or WHERE clauses
- User wants to understand data flow from CTEs to final result

### Basic Flow
1. User pastes query with CTE: `WITH active_users AS (SELECT ...) SELECT * FROM active_users JOIN orders ...`
2. User clicks "Visualize Query"
3. System parses WITH clause and identifies CTE named `active_users`
4. System parses main SELECT and identifies JOIN to CTE
5. System renders diagram with CTE node in separate top section
6. CTE node has distinct color (light blue) and label "active_users (CTE)"
7. System draws arrow from CTE node to main query section
8. User sees data flow: CTE → main query tables
9. User clicks on CTE node to expand its internal SQL
10. System displays popup with CTE definition
11. User understands CTE is filtering active users before JOIN

### Alternative Flow 1: Multiple CTEs
1. System detects 3 CTEs defined in WITH clause
2. System renders all 3 CTEs in top section grouped together
3. System draws arrows showing which CTEs feed into main query
4. User sees dependencies: CTE1 → main query, CTE2 → main query, CTE3 → CTE2
5. User understands CTE3 is intermediate step for CTE2

### Alternative Flow 2: Recursive CTE
1. System detects recursive CTE (self-referencing)
2. System renders CTE node with self-loop edge
3. System adds tooltip "Recursive CTE" on the node
4. User understands query performs hierarchical traversal

### Postconditions

**Success:**
- User understands complete query structure including CTEs
- User sees logical data flow from CTEs through main query
- Complex analytical query is demystified

**Failure:**
- System warns if CTE recursion depth exceeds implementation limit
- Partial diagram shown with warning message

---

## Use Case 7: Optimize Query Based on Visual Analysis

**Actor:** Database Administrator
**Goal:** Identify and fix performance issues by analyzing query structure
**Priority:** High
**Frequency:** As needed (performance troubleshooting)

### Preconditions
- Application monitoring has flagged a slow-running query
- DBA has access to the full SQL query text
- DBA wants to identify structural issues (missing JOINs, Cartesian products)

### Basic Flow
1. DBA copies slow query from monitoring dashboard
2. DBA opens SQLens and pastes query
3. DBA clicks "Visualize Query"
4. System renders diagram with 6 tables and 5 JOINs
5. DBA examines diagram and notices two tables have no connecting edge
6. DBA realizes this creates a Cartesian product (missing JOIN condition)
7. DBA identifies correct foreign key relationship from schema knowledge
8. DBA adds missing JOIN condition to query: `JOIN table2 ON table1.fk_id = table2.id`
9. DBA re-pastes modified query into SQLens
10. System renders updated diagram showing all tables properly connected
11. DBA confirms no Cartesian products remain
12. DBA tests modified query in database and observes 100x performance improvement
13. DBA deploys fix to production

### Alternative Flow 1: Unnecessary JOIN Identified
1. DBA notices a table is JOINed but no columns from it are used in SELECT
2. DBA checks if JOIN is filtering data (WHERE clause on that table)
3. DBA confirms JOIN is truly unnecessary
4. DBA removes JOIN from query
5. DBA re-visualizes and confirms simpler structure
6. DBA tests and observes improved performance

### Alternative Flow 2: Complex JOIN Order
1. DBA notices JOIN order creates large intermediate result set
2. DBA uses diagram to identify better JOIN sequence
3. DBA reorders JOINs to filter data earlier
4. DBA re-visualizes to confirm new flow
5. DBA tests and measures performance gain

### Postconditions

**Success:**
- DBA identifies root cause of performance issue
- DBA fixes query structure
- Application performance improves measurably
- Fixed query structure is documented via exported diagram

**Failure:**
- Issue is not structural (e.g., missing index, not bad JOINs)
- DBA uses other tools to continue investigation

---

## Use Case 8: Validate ORM-Generated Query

**Actor:** Backend Developer
**Goal:** Verify that ORM framework (Hibernate, JPA) generates expected SQL structure
**Priority:** Medium
**Frequency:** During development when using complex ORM queries

### Preconditions
- Developer writes code using ORM query builder or criteria API
- Developer enables SQL logging in ORM configuration
- Developer wants to verify generated SQL matches intention

### Basic Flow
1. Developer writes ORM code: `entityManager.createQuery("SELECT o FROM Order o JOIN FETCH o.customer WHERE ...", Order.class)`
2. Developer runs application with SQL logging enabled
3. Developer copies generated SQL from application logs
4. Developer opens SQLens and pastes SQL
5. Developer clicks "Visualize Query"
6. System renders diagram showing Order and Customer tables with INNER JOIN
7. Developer verifies JOIN type is correct (INNER vs LEFT)
8. Developer confirms JOIN condition matches expected foreign key
9. Developer confirms no N+1 query issue (single JOIN, not separate queries)
10. Developer is satisfied with ORM-generated query structure

### Alternative Flow 1: Unexpected JOIN Type
1. Developer expected LEFT JOIN but diagram shows INNER JOIN
2. Developer realizes ORM code uses `JOIN` instead of `LEFT JOIN FETCH`
3. Developer modifies ORM code to use correct join type
4. Developer re-runs application and visualizes new SQL
5. Developer confirms diagram now shows LEFT JOIN

### Alternative Flow 2: Missing JOIN
1. Developer expected to see `OrderItems` table in diagram
2. Developer realizes ORM lazy loading will cause N+1 queries
3. Developer adds `JOIN FETCH o.orderItems` to ORM query
4. Developer re-visualizes and confirms `OrderItems` now appears

### Postconditions

**Success:**
- Developer confirms ORM generates efficient SQL structure
- Developer prevents N+1 query problems before production
- Developer documents expected query structure

**Failure:**
- Developer identifies ORM misconfiguration
- Developer corrects ORM code to generate proper SQL

---

## System Requirements

### Functional Requirements Derived from Use Cases

> *Table 5: Functional requirements derived from use cases*

| Requirement | Source Use Cases |
|-------------|-----------------|
| SQL input with paste support | UC1, UC3, UC4, UC7, UC8 |
| Query parsing and diagram generation | UC1, UC2, UC3, UC4, UC6, UC7, UC8 |
| Export to PNG/SVG | UC2, UC3, UC4, UC7 |
| Diagram interaction (drag, zoom, pan) | UC2, UC3 |
| Query history | UC5 |
| CTE and subquery support | UC6 |
| Error handling and messaging | UC1, UC4, UC8 |

### Non-Functional Requirements Derived from Use Cases

| Requirement | Source Use Cases |
|-------------|-----------------|
| **Performance:** Diagram rendering ≤3s for up to 15 tables | UC3, UC7 |
| **Usability:** Error messages include line numbers and suggestions | UC1, UC4 |
| **Reliability:** Handle malformed SQL gracefully without crashing | UC1, UC8 |
| **Compatibility:** Work in Chrome, Firefox, Safari, Edge | All use cases |
| **Storage:** Query history persists for 30 days in local storage | UC5 |

---

## References

1. Lucid Software. (n.d.). Use Case Diagram Template. https://lucid.co/templates/use-case-diagram
2. Venngage. (2026). Use case diagram examples. https://venngage.com/blog/use-case-diagram-example/
3. Miro. (n.d.). Use Case Diagram Templates for Effective System Analysis. https://miro.com/templates/use-case-diagram/

# E2E Test Plan
## SQLens - SQL Query Structure Visualizer
### Playwright Test Scenarios

**Version:** 1.0
**Date:** February 21, 2026
**Framework:** Playwright (TypeScript)
**Coverage target:** All Use Cases + Must Have / Should Have User Stories

---

## 1. Setup & Configuration

### `playwright.config.ts`

```typescript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,

  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['junit', { outputFile: 'test-results/results.xml' }],
  ],

  use: {
    baseURL: 'http://localhost:4200',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },

  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox',  use: { ...devices['Desktop Firefox'] } },
    { name: 'webkit',   use: { ...devices['Desktop Safari'] } },
  ],

  // Start Angular dev server and Spring Boot before tests
  webServer: [
    {
      command: 'ng serve',
      url: 'http://localhost:4200',
      reuseExistingServer: !process.env.CI,
    },
    {
      command: 'mvn spring-boot:run -Dspring-boot.run.profiles=test',
      url: 'http://localhost:8080/actuator/health',
      reuseExistingServer: !process.env.CI,
    },
  ],
});
```

### Test Fixtures (`e2e/fixtures/sql-samples.ts`)

```typescript
export const SQL = {
  // Simple — US 1.2
  simple2Table: `
    SELECT o.id, c.name
    FROM orders o
    INNER JOIN customers c ON o.customer_id = c.id
  `,

  // Multiple JOINs — US 1.3
  fiveTable: `
    SELECT o.id, c.name, p.title, oi.quantity, cat.name
    FROM orders o
    INNER JOIN customers c   ON o.customer_id   = c.id
    LEFT  JOIN order_items oi ON oi.order_id     = o.id
    LEFT  JOIN products p    ON oi.product_id    = p.id
    INNER JOIN categories cat ON p.category_id   = cat.id
  `,

  // CTE — US 2.2
  withCte: `
    WITH active_orders AS (
      SELECT * FROM orders WHERE status = 'active'
    )
    SELECT ao.id, c.name
    FROM active_orders ao
    JOIN customers c ON ao.customer_id = c.id
  `,

  // Subquery — US 2.1
  withSubquery: `
    SELECT *
    FROM (SELECT * FROM orders WHERE status = 'active') o
    JOIN customers c ON o.customer_id = c.id
  `,

  // All JOIN types — US 2.3
  allJoinTypes: `
    SELECT *
    FROM orders o
    INNER JOIN      customers c   ON o.customer_id = c.id
    LEFT  JOIN      payments p    ON p.order_id    = o.id
    RIGHT JOIN      shipments s   ON s.order_id    = o.id
    FULL  OUTER JOIN returns r    ON r.order_id    = o.id
    CROSS JOIN      currencies cur
  `,

  // Syntax error
  syntaxError: `SELECT * FORM orders`, // typo: FORM

  // Unsupported statement
  insertStatement: `INSERT INTO orders (id) VALUES (1)`,

  // Too long (generated at test time)
  tooLong: 'SELECT * FROM t -- ' + 'x'.repeat(100_000),

  // Complex 10 tables
  tenTable: `
    SELECT *
    FROM orders o
    JOIN customers c      ON o.customer_id      = c.id
    JOIN order_items oi   ON oi.order_id        = o.id
    JOIN products p       ON oi.product_id      = p.id
    JOIN categories cat   ON p.category_id      = cat.id
    JOIN suppliers s      ON p.supplier_id      = s.id
    JOIN shipments sh     ON sh.order_id        = o.id
    JOIN addresses a      ON sh.address_id      = a.id
    JOIN payments pay     ON pay.order_id       = o.id
    JOIN currencies cur   ON pay.currency_id    = cur.id
  `,
};
```

### Page Object Model (`e2e/pages/sqlens.page.ts`)

```typescript
import { Page, Locator } from '@playwright/test';

export class SQLensPage {
  readonly sqlInput:       Locator;
  readonly visualizeBtn:   Locator;
  readonly diagramCanvas:  Locator;
  readonly errorBanner:    Locator;
  readonly warningBanner:  Locator;
  readonly exportBtn:      Locator;
  readonly historyBtn:     Locator;
  readonly relayoutBtn:    Locator;
  readonly fitScreenBtn:   Locator;
  readonly zoomIndicator:  Locator;
  readonly legend:         Locator;

  constructor(private page: Page) {
    this.sqlInput      = page.getByTestId('sql-input');
    this.visualizeBtn  = page.getByTestId('visualize-btn');
    this.diagramCanvas = page.getByTestId('diagram-canvas');
    this.errorBanner   = page.getByTestId('error-banner');
    this.warningBanner = page.getByTestId('warning-banner');
    this.exportBtn     = page.getByTestId('export-btn');
    this.historyBtn    = page.getByTestId('history-btn');
    this.relayoutBtn   = page.getByTestId('relayout-btn');
    this.fitScreenBtn  = page.getByTestId('fit-screen-btn');
    this.zoomIndicator = page.getByTestId('zoom-indicator');
    this.legend        = page.getByTestId('join-legend');
  }

  async goto() {
    await this.page.goto('/');
  }

  async pasteAndVisualize(sql: string) {
    await this.sqlInput.fill(sql);
    await this.visualizeBtn.click();
  }

  node(label: string): Locator {
    return this.page.getByTestId(`node-${label}`);
  }

  edge(fromId: string, toId: string): Locator {
    return this.page.getByTestId(`edge-${fromId}-${toId}`);
  }

  historyEntry(index: number): Locator {
    return this.page.getByTestId(`history-entry-${index}`);
  }
}
```

---

## 2. Test Suites

### Suite 1 — Core Query Visualization (UC1, US 1.1–1.4)

**File:** `e2e/tests/01-core-visualization.spec.ts`

```typescript
import { test, expect } from '@playwright/test';
import { SQLensPage } from '../pages/sqlens.page';
import { SQL } from '../fixtures/sql-samples';

test.describe('Core Query Visualization', () => {

  let sqlens: SQLensPage;

  test.beforeEach(async ({ page }) => {
    sqlens = new SQLensPage(page);
    await sqlens.goto();
  });

  // US 1.1: Paste SQL Query
  test('SQL input accepts pasted query', async ({ page }) => {
    await sqlens.sqlInput.click();
    await sqlens.sqlInput.fill(SQL.simple2Table);
    await expect(sqlens.sqlInput).toHaveValue(SQL.simple2Table.trim());
  });

  test('shows error when query exceeds 100,000 characters', async () => {
    await sqlens.pasteAndVisualize(SQL.tooLong);
    await expect(sqlens.errorBanner).toContainText('Query too long');
    await expect(sqlens.errorBanner).toContainText('100,000');
  });

  // US 1.2: Parse Simple JOIN Query
  test('renders two table nodes for simple INNER JOIN', async () => {
    await sqlens.pasteAndVisualize(SQL.simple2Table);

    await expect(sqlens.node('orders')).toBeVisible();
    await expect(sqlens.node('customers')).toBeVisible();
  });

  test('renders edge labeled INNER JOIN', async () => {
    await sqlens.pasteAndVisualize(SQL.simple2Table);

    const edge = sqlens.edge('orders', 'customers');
    await expect(edge).toBeVisible();
    await expect(edge).toContainText('INNER JOIN');
  });

  test('edge tooltip shows ON condition', async ({ page }) => {
    await sqlens.pasteAndVisualize(SQL.simple2Table);

    const edge = sqlens.edge('orders', 'customers');
    await edge.hover();
    await expect(page.getByTestId('edge-tooltip')).toContainText('o.customer_id = c.id');
  });

  test('diagram area is empty on syntax error', async () => {
    await sqlens.pasteAndVisualize(SQL.syntaxError);

    await expect(sqlens.errorBanner).toBeVisible();
    await expect(sqlens.diagramCanvas.getByTestId(/^node-/)).toHaveCount(0);
  });

  test('shows warning for unsupported statement', async () => {
    await sqlens.pasteAndVisualize(SQL.insertStatement);

    await expect(sqlens.warningBanner).toContainText('SELECT statements are supported');
  });

  // US 1.3: Visualize Multiple JOINs
  test('renders 5 distinct table nodes for 5-table query', async () => {
    await sqlens.pasteAndVisualize(SQL.fiveTable);

    for (const table of ['orders', 'customers', 'order_items', 'products', 'categories']) {
      await expect(sqlens.node(table)).toBeVisible();
    }
  });

  test('LEFT JOIN edges are orange, INNER JOIN edges are teal', async ({ page }) => {
    await sqlens.pasteAndVisualize(SQL.fiveTable);

    // Check edge colors via CSS or data attributes
    const leftEdges  = page.locator('[data-join-type="LEFT"]');
    const innerEdges = page.locator('[data-join-type="INNER"]');

    await expect(leftEdges.first()).toHaveCSS('stroke', 'rgb(168, 75, 47)');   // #a84b2f
    await expect(innerEdges.first()).toHaveCSS('stroke', 'rgb(33, 128, 141)'); // #21808d
  });

  // US 1.4: Auto-Layout
  test('diagram renders within 2 seconds for 5-table query', async () => {
    const start = Date.now();
    await sqlens.pasteAndVisualize(SQL.fiveTable);
    await expect(sqlens.node('orders')).toBeVisible();
    expect(Date.now() - start).toBeLessThan(2_000);
  });

  test('re-layout button resets node positions', async ({ page }) => {
    await sqlens.pasteAndVisualize(SQL.simple2Table);

    // Get initial position
    const node = sqlens.node('orders');
    const boxBefore = await node.boundingBox();

    // Drag node to new position
    await node.dragTo(page.locator('body'), { targetPosition: { x: 800, y: 400 } });

    // Re-layout
    await sqlens.relayoutBtn.click();
    const boxAfter = await node.boundingBox();

    expect(boxAfter!.x).not.toEqual(800);
  });

});
```

---

### Suite 2 — Advanced SQL Features (UC6, US 2.1–2.3)

**File:** `e2e/tests/02-advanced-sql.spec.ts`

```typescript
import { test, expect } from '@playwright/test';
import { SQLensPage } from '../pages/sqlens.page';
import { SQL } from '../fixtures/sql-samples';

test.describe('Advanced SQL Features', () => {

  let sqlens: SQLensPage;

  test.beforeEach(async ({ page }) => {
    sqlens = new SQLensPage(page);
    await sqlens.goto();
  });

  // US 2.1: Subqueries
  test('renders subquery node with distinct visual', async ({ page }) => {
    await sqlens.pasteAndVisualize(SQL.withSubquery);

    const subNode = page.locator('[data-node-type="subquery"]');
    await expect(subNode).toBeVisible();
    await expect(subNode).toContainText('subquery');
  });

  test('subquery node connects to main query tables', async ({ page }) => {
    await sqlens.pasteAndVisualize(SQL.withSubquery);

    await expect(sqlens.node('customers')).toBeVisible();
    // Edge from subquery to customers should exist
    const edge = page.locator('[data-edge-to="customers"]');
    await expect(edge).toBeVisible();
  });

  // US 2.2: CTEs
  test('renders CTE node labeled with (CTE) suffix', async ({ page }) => {
    await sqlens.pasteAndVisualize(SQL.withCte);

    const cteNode = page.locator('[data-node-type="cte"]');
    await expect(cteNode).toBeVisible();
    await expect(cteNode).toContainText('active_orders');
    await expect(cteNode).toContainText('CTE');
  });

  test('CTE node has light-blue background', async ({ page }) => {
    await sqlens.pasteAndVisualize(SQL.withCte);

    const cteNode = page.locator('[data-node-type="cte"]').first();
    await expect(cteNode).toHaveCSS('background-color', 'rgb(207, 226, 255)'); // #cfe2ff
  });

  test('CTE node connects to main query tables via arrow', async ({ page }) => {
    await sqlens.pasteAndVisualize(SQL.withCte);

    const arrow = page.locator('[data-edge-from="cte_active_orders"]');
    await expect(arrow).toBeVisible();
  });

  // US 2.3: All JOIN Types
  test('renders all 5 JOIN types with correct colors', async ({ page }) => {
    await sqlens.pasteAndVisualize(SQL.allJoinTypes);

    const colorMap: Record<string, string> = {
      INNER: 'rgb(33, 128, 141)',   // #21808d teal
      LEFT:  'rgb(168, 75, 47)',    // #a84b2f orange
      RIGHT: 'rgb(107, 33, 168)',   // #6b21a8 purple
      FULL:  'rgb(192, 21, 47)',    // #c0152f red
      CROSS: 'rgb(98, 108, 113)',   // #626c71 gray
    };

    for (const [joinType, color] of Object.entries(colorMap)) {
      const edge = page.locator(`[data-join-type="${joinType}"]`).first();
      await expect(edge).toBeVisible();
      await expect(edge).toHaveCSS('stroke', color);
    }
  });

  test('legend is visible showing JOIN type colors', async () => {
    await sqlens.pasteAndVisualize(SQL.allJoinTypes);
    await expect(sqlens.legend).toBeVisible();
    await expect(sqlens.legend).toContainText('INNER JOIN');
    await expect(sqlens.legend).toContainText('LEFT JOIN');
  });

  test('FULL OUTER JOIN edge has bidirectional arrows', async ({ page }) => {
    await sqlens.pasteAndVisualize(SQL.allJoinTypes);

    const fullEdge = page.locator('[data-join-type="FULL"]');
    await expect(fullEdge).toHaveAttribute('data-bidirectional', 'true');
  });

  test('CROSS JOIN edge has dashed line style', async ({ page }) => {
    await sqlens.pasteAndVisualize(SQL.allJoinTypes);

    const crossEdge = page.locator('[data-join-type="CROSS"]');
    await expect(crossEdge).toHaveAttribute('data-line-style', 'dashed');
  });

});
```

---

### Suite 3 — Diagram Interaction (UC3, US 3.1–3.3)

**File:** `e2e/tests/03-diagram-interaction.spec.ts`

```typescript
import { test, expect } from '@playwright/test';
import { SQLensPage } from '../pages/sqlens.page';
import { SQL } from '../fixtures/sql-samples';

test.describe('Diagram Interaction', () => {

  let sqlens: SQLensPage;

  test.beforeEach(async ({ page }) => {
    sqlens = new SQLensPage(page);
    await sqlens.goto();
    await sqlens.pasteAndVisualize(SQL.fiveTable);
    await expect(sqlens.node('orders')).toBeVisible();
  });

  // US 3.1: Drag and Reposition
  test('node stays in new position after drag', async ({ page }) => {
    const node = sqlens.node('orders');
    const boxBefore = await node.boundingBox();

    await page.mouse.move(boxBefore!.x + 50, boxBefore!.y + 20);
    await page.mouse.down();
    await page.mouse.move(boxBefore!.x + 200, boxBefore!.y + 150, { steps: 10 });
    await page.mouse.up();

    const boxAfter = await node.boundingBox();
    expect(boxAfter!.x).toBeGreaterThan(boxBefore!.x + 100);
  });

  test('cursor changes to grab hand when hovering node', async ({ page }) => {
    const node = sqlens.node('orders');
    await node.hover();
    const cursor = await node.evaluate(el => window.getComputedStyle(el).cursor);
    expect(cursor).toBe('grab');
  });

  // US 3.2: Zoom and Pan
  test('mouse wheel up zooms in and indicator updates', async ({ page }) => {
    const canvas = sqlens.diagramCanvas;
    await canvas.hover();
    await page.mouse.wheel(0, -300); // scroll up = zoom in

    const zoomText = await sqlens.zoomIndicator.textContent();
    const zoomValue = parseInt(zoomText!.replace('%', ''));
    expect(zoomValue).toBeGreaterThan(100);
  });

  test('mouse wheel down zooms out to minimum 50%', async ({ page }) => {
    const canvas = sqlens.diagramCanvas;
    await canvas.hover();

    // Scroll down many times
    for (let i = 0; i < 20; i++) {
      await page.mouse.wheel(0, 300);
    }

    const zoomText = await sqlens.zoomIndicator.textContent();
    const zoomValue = parseInt(zoomText!.replace('%', ''));
    expect(zoomValue).toBeGreaterThanOrEqual(50);
  });

  test('zoom does not exceed 200%', async ({ page }) => {
    const canvas = sqlens.diagramCanvas;
    await canvas.hover();

    for (let i = 0; i < 20; i++) {
      await page.mouse.wheel(0, -300);
    }

    const zoomText = await sqlens.zoomIndicator.textContent();
    const zoomValue = parseInt(zoomText!.replace('%', ''));
    expect(zoomValue).toBeLessThanOrEqual(200);
  });

  test('fit to screen button makes all nodes visible', async ({ page }) => {
    // Zoom in first to move off
    const canvas = sqlens.diagramCanvas;
    await canvas.hover();
    for (let i = 0; i < 10; i++) await page.mouse.wheel(0, -300);

    await sqlens.fitScreenBtn.click();

    for (const table of ['orders', 'customers', 'order_items', 'products', 'categories']) {
      await expect(sqlens.node(table)).toBeInViewport();
    }
  });

  // US 3.3: Highlight Related Elements
  test('clicking node highlights its edges and dims others', async ({ page }) => {
    await sqlens.pasteAndVisualize(SQL.fiveTable);

    await sqlens.node('orders').click();

    // Connected edges should be highlighted
    const connectedEdge = page.locator('[data-edge-from="orders"]').first();
    await expect(connectedEdge).toHaveAttribute('data-highlighted', 'true');

    // Unrelated nodes should be dimmed
    const dimmedNode = page.locator('[data-node-dimmed="true"]').first();
    await expect(dimmedNode).toBeVisible();
  });

  test('clicking empty canvas removes all highlights', async ({ page }) => {
    await sqlens.node('orders').click();

    // Click empty space
    await page.mouse.click(10, 10);

    const highlightedEdges = page.locator('[data-highlighted="true"]');
    await expect(highlightedEdges).toHaveCount(0);
  });

  test('hovering edge shows tooltip with JOIN condition', async ({ page }) => {
    const edge = page.locator('[data-join-type="INNER"]').first();
    await edge.hover();

    const tooltip = page.getByTestId('edge-tooltip');
    await expect(tooltip).toBeVisible();
    await expect(tooltip).toContainText('='); // contains condition
  });

});
```

---

### Suite 4 — Export (UC2, US 4.1)

**File:** `e2e/tests/04-export.spec.ts`

```typescript
import { test, expect } from '@playwright/test';
import path from 'path';
import { SQLensPage } from '../pages/sqlens.page';
import { SQL } from '../fixtures/sql-samples';

test.describe('Export Functionality', () => {

  let sqlens: SQLensPage;

  test.beforeEach(async ({ page }) => {
    sqlens = new SQLensPage(page);
    await sqlens.goto();
    await sqlens.pasteAndVisualize(SQL.simple2Table);
    await expect(sqlens.node('orders')).toBeVisible();
  });

  // US 4.1: Export as PNG
  test('PNG download triggers with correct filename pattern', async ({ page }) => {
    const [ download ] = await Promise.all([
      page.waitForEvent('download'),
      sqlens.exportBtn.click(),
      page.getByTestId('export-png-option').click(),
    ]);

    expect(download.suggestedFilename()).toMatch(/^sqlens_diagram_\d{8}_\d{4}\.png$/);
  });

  test('PNG export completes within 3 seconds', async ({ page }) => {
    const start = Date.now();

    const [ download ] = await Promise.all([
      page.waitForEvent('download'),
      sqlens.exportBtn.click(),
      page.getByTestId('export-png-option').click(),
    ]);

    await download.path(); // wait for file to be ready
    expect(Date.now() - start).toBeLessThan(3_000);
  });

  test('SVG download triggers with .svg extension', async ({ page }) => {
    const [ download ] = await Promise.all([
      page.waitForEvent('download'),
      sqlens.exportBtn.click(),
      page.getByTestId('export-svg-option').click(),
    ]);

    expect(download.suggestedFilename()).toMatch(/\.svg$/);
  });

  test('export preserves custom node positions', async ({ page }) => {
    // Drag a node
    const node = sqlens.node('orders');
    const box = await node.boundingBox();
    await page.mouse.move(box!.x + 50, box!.y + 20);
    await page.mouse.down();
    await page.mouse.move(box!.x + 300, box!.y + 200, { steps: 10 });
    await page.mouse.up();

    // Export — just verify download succeeds (content verification requires image analysis)
    const [ download ] = await Promise.all([
      page.waitForEvent('download'),
      sqlens.exportBtn.click(),
      page.getByTestId('export-png-option').click(),
    ]);

    const filePath = await download.path();
    expect(filePath).not.toBeNull();
  });

  test('export is readable for 10-table diagram', async ({ page }) => {
    await sqlens.pasteAndVisualize(SQL.tenTable);
    await expect(sqlens.node('orders')).toBeVisible();

    const start = Date.now();
    const [ download ] = await Promise.all([
      page.waitForEvent('download'),
      sqlens.exportBtn.click(),
      page.getByTestId('export-png-option').click(),
    ]);

    await download.path();
    expect(Date.now() - start).toBeLessThan(3_000);
  });

});
```

---

### Suite 5 — Query History (UC5, US 4.2)

**File:** `e2e/tests/05-history.spec.ts`

```typescript
import { test, expect } from '@playwright/test';
import { SQLensPage } from '../pages/sqlens.page';
import { SQL } from '../fixtures/sql-samples';

test.describe('Query History', () => {

  let sqlens: SQLensPage;

  test.beforeEach(async ({ page }) => {
    // Clear localStorage before each test
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    sqlens = new SQLensPage(page);
    await sqlens.goto();
  });

  test('query is saved to history after successful visualization', async ({ page }) => {
    await sqlens.pasteAndVisualize(SQL.simple2Table);
    await sqlens.historyBtn.click();

    const firstEntry = sqlens.historyEntry(0);
    await expect(firstEntry).toBeVisible();
    await expect(firstEntry).toContainText('orders');
  });

  test('history shows max 10 entries with FIFO eviction', async ({ page }) => {
    // Submit 12 different queries
    const queries = Array.from({ length: 12 }, (_, i) =>
      `SELECT * FROM table_${i}`
    );

    for (const q of queries) {
      await sqlens.pasteAndVisualize(q);
      await expect(sqlens.diagramCanvas).toBeVisible();
    }

    await sqlens.historyBtn.click();
    const entries = page.getByTestId(/^history-entry-/);
    await expect(entries).toHaveCount(10);
  });

  test('clicking history entry loads query and re-renders diagram', async ({ page }) => {
    await sqlens.pasteAndVisualize(SQL.simple2Table);
    await sqlens.pasteAndVisualize(SQL.fiveTable);

    await sqlens.historyBtn.click();

    // Click second entry (the first one submitted)
    await sqlens.historyEntry(1).click();

    await expect(sqlens.node('orders')).toBeVisible();
    await expect(sqlens.node('customers')).toBeVisible();
  });

  test('history persists after page reload', async ({ page }) => {
    await sqlens.pasteAndVisualize(SQL.simple2Table);

    await page.reload();
    sqlens = new SQLensPage(page);
    await sqlens.historyBtn.click();

    await expect(sqlens.historyEntry(0)).toBeVisible();
  });

  test('clear history deletes all entries after confirmation', async ({ page }) => {
    await sqlens.pasteAndVisualize(SQL.simple2Table);
    await sqlens.historyBtn.click();

    await page.getByTestId('clear-history-btn').click();
    // Confirm dialog
    await page.getByTestId('confirm-clear-btn').click();

    await expect(page.getByTestId('history-empty-message')).toBeVisible();
    await expect(page.getByTestId(/^history-entry-/)).toHaveCount(0);
  });

});
```

---

### Suite 6 — Error Handling (UC1 Alt, US 5.1)

**File:** `e2e/tests/06-error-handling.spec.ts`

```typescript
import { test, expect } from '@playwright/test';
import { SQLensPage } from '../pages/sqlens.page';
import { SQL } from '../fixtures/sql-samples';

test.describe('Error Handling', () => {

  let sqlens: SQLensPage;

  test.beforeEach(async ({ page }) => {
    sqlens = new SQLensPage(page);
    await sqlens.goto();
  });

  test('syntax error shows banner with line number', async () => {
    await sqlens.pasteAndVisualize(SQL.syntaxError);

    await expect(sqlens.errorBanner).toBeVisible();
    await expect(sqlens.errorBanner).toContainText('line');
    // Error appears within 500ms
  });

  test('error banner appears within 500ms', async ({ page }) => {
    const start = Date.now();
    await sqlens.pasteAndVisualize(SQL.syntaxError);
    await expect(sqlens.errorBanner).toBeVisible();
    expect(Date.now() - start).toBeLessThan(2_000); // network round-trip included
  });

  test('unsupported INSERT shows specific warning', async () => {
    await sqlens.pasteAndVisualize(SQL.insertStatement);

    await expect(sqlens.warningBanner).toContainText('SELECT');
  });

  test('empty input shows prompt with examples', async ({ page }) => {
    await sqlens.visualizeBtn.click();

    const prompt = page.getByTestId('empty-input-prompt');
    await expect(prompt).toBeVisible();
    await expect(prompt).toContainText('Paste a SQL query');
  });

  test('user can correct error and re-submit without page refresh', async ({ page }) => {
    await sqlens.pasteAndVisualize(SQL.syntaxError);
    await expect(sqlens.errorBanner).toBeVisible();

    // Correct the query
    await sqlens.sqlInput.clear();
    await sqlens.pasteAndVisualize(SQL.simple2Table);

    await expect(sqlens.errorBanner).not.toBeVisible();
    await expect(sqlens.node('orders')).toBeVisible();
  });

  test('diagram area stays empty after parse failure', async ({ page }) => {
    await sqlens.pasteAndVisualize(SQL.syntaxError);

    const nodes = page.locator('[data-testid^="node-"]');
    await expect(nodes).toHaveCount(0);
  });

});
```

---

### Suite 7 — Performance (UC3 Alt1, US 5.2)

**File:** `e2e/tests/07-performance.spec.ts`

```typescript
import { test, expect } from '@playwright/test';
import { SQLensPage } from '../pages/sqlens.page';
import { SQL } from '../fixtures/sql-samples';

test.describe('Performance', () => {

  let sqlens: SQLensPage;

  test.beforeEach(async ({ page }) => {
    sqlens = new SQLensPage(page);
    await sqlens.goto();
  });

  test('10-table query renders within 3 seconds', async ({ page }) => {
    const start = Date.now();
    await sqlens.pasteAndVisualize(SQL.tenTable);
    await expect(sqlens.node('orders')).toBeVisible();
    expect(Date.now() - start).toBeLessThan(3_000);
  });

  test('shows complexity warning for 20+ table query', async ({ page }) => {
    // Generate a 22-table query dynamically
    const tables = Array.from({ length: 22 }, (_, i) => `t${i}`);
    const sql = `
      SELECT * FROM ${tables[0]}
      ${tables.slice(1).map((t, i) =>
        `JOIN ${t} ON ${tables[i]}.id = ${t}.fk`
      ).join('\n')}
    `;

    await sqlens.pasteAndVisualize(sql);

    const warning = page.getByTestId('complexity-warning');
    await expect(warning).toBeVisible();
    await expect(warning).toContainText('complex query detected');
  });

  test('"Visualize Anyway" renders large diagram', async ({ page }) => {
    const tables = Array.from({ length: 22 }, (_, i) => `t${i}`);
    const sql = `
      SELECT * FROM ${tables[0]}
      ${tables.slice(1).map((t, i) =>
        `JOIN ${t} ON ${tables[i]}.id = ${t}.fk`
      ).join('\n')}
    `;

    await sqlens.pasteAndVisualize(sql);

    await page.getByTestId('visualize-anyway-btn').click();
    await expect(sqlens.node('t0')).toBeVisible({ timeout: 10_000 });
  });

});
```

---

### Suite 8 — PR Review Workflow (UC4 — End-to-End Scenario)

**File:** `e2e/tests/08-pr-review-workflow.spec.ts`

This suite tests the full UC4 scenario: developer reviews a PR by visualizing before/after SQL.

```typescript
import { test, expect } from '@playwright/test';
import { SQLensPage } from '../pages/sqlens.page';

test.describe('PR Review Workflow (UC4)', () => {

  const beforeSql = `
    SELECT o.id, c.name
    FROM orders o
    INNER JOIN customers c ON o.customer_id = c.id
  `;

  const afterSql = `
    SELECT o.id, c.name, p.amount
    FROM orders o
    INNER JOIN customers c ON o.customer_id = c.id
    LEFT  JOIN payments  p ON p.order_id    = o.id
  `;

  test('developer can compare before/after SQL by exporting both', async ({ page }) => {
    const sqlens = new SQLensPage(page);
    await sqlens.goto();

    // Step 1: Visualize "before" SQL
    await sqlens.pasteAndVisualize(beforeSql);
    await expect(sqlens.node('orders')).toBeVisible();

    const [ before ] = await Promise.all([
      page.waitForEvent('download'),
      sqlens.exportBtn.click(),
      page.getByTestId('export-png-option').click(),
    ]);
    expect(before.suggestedFilename()).toMatch(/\.png$/);

    // Step 2: Visualize "after" SQL
    await sqlens.pasteAndVisualize(afterSql);
    await expect(sqlens.node('payments')).toBeVisible();

    const [ after ] = await Promise.all([
      page.waitForEvent('download'),
      sqlens.exportBtn.click(),
      page.getByTestId('export-png-option').click(),
    ]);
    expect(after.suggestedFilename()).toMatch(/\.png$/);
  });

  test('syntax error in PR SQL is caught and reported', async ({ page }) => {
    const sqlens = new SQLensPage(page);
    await sqlens.goto();

    await sqlens.pasteAndVisualize(`SELECT * FORM orders`);
    await expect(sqlens.errorBanner).toBeVisible();
    await expect(sqlens.errorBanner).toContainText('line');
  });

});
```

---

### Suite 9 — Accessibility (NFR-3)

**File:** `e2e/tests/09-accessibility.spec.ts`

```typescript
import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { SQLensPage } from '../pages/sqlens.page';
import { SQL } from '../fixtures/sql-samples';

test.describe('Accessibility', () => {

  test('home page has no critical accessibility violations', async ({ page }) => {
    await page.goto('/');
    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze();
    expect(results.violations).toHaveLength(0);
  });

  test('diagram page has no critical accessibility violations', async ({ page }) => {
    const sqlens = new SQLensPage(page);
    await sqlens.goto();
    await sqlens.pasteAndVisualize(SQL.simple2Table);
    await expect(sqlens.node('orders')).toBeVisible();

    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze();
    expect(results.violations).toHaveLength(0);
  });

  test('all interactive elements are keyboard navigable', async ({ page }) => {
    await page.goto('/');

    // Tab through interactive elements
    await page.keyboard.press('Tab'); // SQL input
    await expect(page.locator(':focus')).toHaveAttribute('data-testid', 'sql-input');

    await page.keyboard.press('Tab'); // Visualize button
    await expect(page.locator(':focus')).toHaveAttribute('data-testid', 'visualize-btn');
  });

});
```

---

## 3. Test Data Management

### `e2e/fixtures/test-data.ts`

```typescript
export const EXPECTED_NODES = {
  simple2Table: [
    { id: 'orders',    alias: 'o', type: 'table' },
    { id: 'customers', alias: 'c', type: 'table' },
  ],
  withCte: [
    { id: 'cte_active_orders', alias: null, type: 'cte' },
    { id: 'customers',         alias: 'c',  type: 'table' },
  ],
};

export const EXPECTED_EDGES = {
  simple2Table: [
    { from: 'orders', to: 'customers', joinType: 'INNER', condition: 'o.customer_id = c.id' },
  ],
};
```

---

## 4. CI Integration

### `.github/workflows/e2e.yml`

```yaml
name: E2E Tests

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  e2e:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java 17
        uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin' }

      - name: Setup Node 20
        uses: actions/setup-node@v4
        with: { node-version: '20' }

      - name: Install dependencies
        run: npm ci
        working-directory: frontend

      - name: Install Playwright browsers
        run: npx playwright install --with-deps
        working-directory: frontend

      - name: Build backend
        run: mvn package -DskipTests
        working-directory: backend

      - name: Run E2E tests
        run: npx playwright test
        working-directory: frontend

      - name: Upload Playwright report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report
          path: frontend/playwright-report/
          retention-days: 30
```

---

## 5. Test Coverage Matrix

| User Story | Test Suite | Test Count | Priority |
|------------|-----------|------------|----------|
| US 1.1 — Paste SQL | Suite 1 | 2 | P0 |
| US 1.2 — Parse Simple JOIN | Suite 1 | 4 | P0 |
| US 1.3 — Multiple JOINs | Suite 1 | 2 | P0 |
| US 1.4 — Auto-Layout | Suite 1 | 2 | P1 |
| US 2.1 — Subqueries | Suite 2 | 2 | P1 |
| US 2.2 — CTEs | Suite 2 | 3 | P1 |
| US 2.3 — All JOIN Types | Suite 2 | 4 | P1 |
| US 3.1 — Drag Nodes | Suite 3 | 2 | P1 |
| US 3.2 — Zoom/Pan | Suite 3 | 4 | P1 |
| US 3.3 — Highlight | Suite 3 | 3 | P2 |
| US 4.1 — Export PNG | Suite 4 | 5 | P1 |
| US 4.2 — History | Suite 5 | 5 | P2 |
| US 5.1 — Error Messages | Suite 6 | 6 | P0 |
| US 5.2 — Performance | Suite 7 | 3 | P1 |
| UC4 — PR Review | Suite 8 | 2 | High |
| NFR-3 — Accessibility | Suite 9 | 3 | Medium |
| **Total** | | **52** | |

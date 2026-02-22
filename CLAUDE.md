# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SQLens is a SQL Query Structure Visualizer — a web app that parses SQL queries and renders them as interactive diagrams showing tables, joins, CTEs, and subqueries. It is packaged as a single fat JAR where Spring Boot serves the Angular frontend as static resources.

## Commands

### Full Project Build
```bash
mvn clean install                    # Build everything (frontend + backend)
mvn clean install -DskipTests        # Skip tests for faster build
```

### Backend Development
```bash
cd backend && mvn spring-boot:run    # Run backend only (port 8021)
mvn clean test -pl backend -am       # Run backend tests only
```

### Frontend Development
```bash
cd frontend && npm start             # Dev server at localhost:4200 (proxies /api → :8021)
cd frontend && npm test              # Run Karma/Jasmine tests
cd frontend && npm run build         # Production build
cd frontend && npm run lint          # Lint
```

### Docker
```bash
docker-compose up                    # Build and run (port 8022)
docker build -t sqlens:latest .      # Build image manually
```

### Running the Fat JAR
```bash
java -jar backend/target/sqlens-backend-0.0.1-SNAPSHOT.jar  # Serves both API and frontend
```

## Architecture

### Multi-Module Maven Build
The root `pom.xml` defines two modules: `frontend` and `backend`. The frontend module uses `frontend-maven-plugin` to run `npm ci` and `ng build`, then copies the Angular output from `dist/sqlens-frontend/browser/` into `META-INF/resources/` inside its JAR. The backend module depends on that JAR, so Spring Boot's auto-serving picks up the Angular files. A single fat JAR is produced at `backend/target/sqlens-backend-0.0.1-SNAPSHOT.jar`.

### Backend — Clean/Hexagonal Architecture

Package root: `org.buulean.sqlensbackend`

| Layer | Package | Responsibility |
|---|---|---|
| Presentation | `presentation/` | REST controllers, DTOs, mapper |
| Application | `application/` | Use cases, validation chain, graph builder |
| Domain | `domain/` | Models, port interfaces, ParseResult |
| Infrastructure | `infrastructure/` | JSQLParser adapter, node/edge extractors, Spring config |

**Key design patterns:**
- **Chain of Responsibility** — `QueryValidator` chains `EmptyQueryValidator` → `QueryLengthValidator` → `StatementTypeValidator`. The chain is wired in `BeanConfig`.
- **Pluggable Extractors (OCP)** — Spring auto-discovers all `NodeExtractor` and `EdgeExtractor` beans; adding a new extractor requires no changes to existing code.
- **ParseResult tagged union** — `ParseResult<T, E>` is `Success | Failure`; used throughout to avoid exceptions on expected errors.
- **Composite** — `SubqueryNode` contains an inner `QueryGraph`.
- **Adapter** — `JSQLParserAdapter` wraps the third-party `jsqlparser` library behind the `SqlParser` port interface.

**Main flow:**
`POST /api/sql/analyze` → `SqlAnalyzeController` → `AnalyzeQueryUseCase` → validate → parse → extract nodes/edges → build `QueryGraph` → map to `DiagramResponseDto`.

**Non-API routes** are caught by `SpaController`, which forwards them to `index.html` to support Angular client-side routing.

### Frontend — Angular 17 (Standalone Components)

```
src/app/
├── core/
│   ├── services/    SqlAnalysisService (HTTP), DiagramStateService (state)
│   └── models/      Request/response types, ApiError
└── features/diagram/
    ├── diagram-page/        Main page, orchestrates child components
    ├── components/
    │   ├── sql-input/       SQL textarea + submit form
    │   ├── diagram-canvas/  JointJS visualization
    │   └── error-display/   Error rendering
```

**Key patterns:**
- All components are standalone (no NgModules).
- `DiagramStateService` holds typed state (`idle | loading | success | error`) via RxJS Observables, consumed as Signals via `toSignal()`.
- All components use `ChangeDetectionStrategy.OnPush`.
- JointJS diagrams use a custom `sqlens.TableNode` shape rendered in `DiagramCanvasComponent`.
- Development requests to `/api` are proxied to `http://localhost:8021` via `proxy.conf.json`.

### API Contract

```
POST /api/sql/analyze
Content-Type: application/json
Body: { "sql": "<query>" }

200: { nodes: NodeDto[], edges: EdgeDto[], warning?: string }
400: { code: string, message: string, line?: number, column?: number }
```

## Ports

| Context | Port |
|---|---|
| Backend dev (`mvn spring-boot:run`) | 8021 |
| Frontend dev (`ng serve`) | 4200 |
| Docker / fat JAR | 8022 |

## Key Dependencies

- **jsqlparser 4.9** — SQL parsing (backend)
- **JointJS 3.7.7** — diagram rendering (frontend)
- **Lombok** — reduces boilerplate on domain/dto classes
- **Spring Boot Actuator** — health endpoint at `/actuator/health` (used by Docker health check)

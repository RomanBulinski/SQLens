# Development Setup Guide
## SQLens - SQL Query Structure Visualizer

**Version:** 1.1
**Date:** February 21, 2026
**Author:** Roman Bulinski
**Primary environment:** Windows + IntelliJ IDEA

---

## Prerequisites

| Tool | Required Version | How to check |
|------|-----------------|--------------|
| JDK | 17+ (Temurin recommended) | `java -version` in CMD/PowerShell |
| Maven | 3.9+ (or use IntelliJ's bundled) | `mvn -version` |
| Node.js | 20 LTS | `node -v` |
| npm | 10+ | `npm -v` |
| IntelliJ IDEA | 2023.x+ (Community or Ultimate) | Already installed |
| Git | any recent | `git --version` |

> All terminal commands in this guide use **PowerShell** unless noted otherwise.
> Open PowerShell: `Win + R` → type `powershell` → Enter.

---

## 1. Repository Structure

This is a **Maven multi-module project** — one root `pom.xml` manages both the Angular frontend and the Spring Boot backend. A single `mvn clean package` from the root produces one self-contained JAR.

```
sqlens\
├── pom.xml                          ← parent POM (all Maven commands run from here)
├── frontend\                        ← Maven module: Angular app
│   ├── pom.xml                      ← built by frontend-maven-plugin
│   ├── package.json                 ← Angular project files (created by ng new)
│   ├── angular.json
│   ├── src\
│   └── dist\                        ← ng build output (git-ignored)
├── backend\
│   └── sqlens-backend\              ← Maven module: Spring Boot app
│       ├── pom.xml                  ← depends on sqlens-frontend JAR
│       └── src\
└── doc\                             ← All documentation
```

**Production build (single JAR):**
```
mvn clean package           (from sqlens\ root)
  → frontend: Node downloaded locally → npm ci → ng build
  → backend:  Spring Boot fat JAR embeds Angular as static resources
  → result:   one JAR on port 8080 serving API + Angular UI
```

**Development (two servers with proxy):**
```
Backend  → mvn spring-boot:run   (port 8080, API only)
Frontend → ng serve              (port 4200, proxies /api/* → 8080)
```

---

## 2. Backend Setup (Spring Boot)

### 2.1 Install JDK 17

**Option A — Via IntelliJ IDEA (recommended, simplest):**

IntelliJ can download and manage JDKs for you:
1. Open IntelliJ IDEA
2. `File → Project Structure` (Ctrl+Alt+Shift+S)
3. `Platform Settings → SDKs → + → Download JDK`
4. Select **Version: 17**, **Vendor: Eclipse Temurin**
5. Click **Download** — IntelliJ installs it automatically

**Option B — Via winget (Windows Package Manager):**

```powershell
winget install EclipseAdoptium.Temurin.17.JDK
```

Restart PowerShell after installation, then verify:

```powershell
java -version
# openjdk version "17.x.x" ...
```

**Option C — Manual installer:**

1. Download the Windows `.msi` installer from https://adoptium.net
2. Select **Temurin 17 (LTS)** → **Windows x64 → .msi**
3. Run the installer — check the boxes:
   - ✅ Set JAVA_HOME variable
   - ✅ Add to PATH
4. Restart PowerShell and verify `java -version`

**Set JAVA_HOME manually (if not set by installer):**

```powershell
# Check if JAVA_HOME is already set
echo $env:JAVA_HOME

# If empty, find where Java was installed
# Default path for Temurin:
# C:\Program Files\Eclipse Adoptium\jdk-17.x.x.x-hotspot

# Set permanently via System Properties:
# Win + R → sysdm.cpl → Advanced → Environment Variables
# Add new System variable: JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.x.x.x-hotspot
```

### 2.2 Open the Whole Project in IntelliJ IDEA

Open the **root** `pom.xml` — not the backend one. IntelliJ will recognize all modules automatically.

1. Open IntelliJ IDEA
2. `File → Open` → navigate to `D:\02_IT\01_Aplication\15_SQLens\sqlens`
3. Select the **root `pom.xml`** (the one directly in `sqlens\`) → click **Open as Project**
4. IntelliJ detects it as a Maven project and imports dependencies automatically
5. Wait for the Maven import to complete (progress bar in bottom right)

> If IntelliJ asks which JDK to use, select the Temurin 17 you installed in step 2.1.

### 2.3 Configure the Backend

In IntelliJ's Project panel, navigate to:
`src/main/resources/application.properties`

Create it if it doesn't exist (copy from `application.properties.example`):

```properties
server.port=8080
spring.application.name=sqlens

# CORS — allow Angular dev server
sqlens.cors.allowed-origins=http://localhost:4200

# Logging
logging.level.com.sqlens=DEBUG
logging.level.net.sf.jsqlparser=WARN

# Actuator health endpoint (used by Playwright webServer config)
management.endpoints.web.exposure.include=health
```

### 2.4 Run the Backend from IntelliJ

**Recommended — IntelliJ Run Configuration:**

1. In the Project panel, find `src/main/java/com/sqlens/SqlensApplication.java`
2. Click the green ▶ button next to the `main` method
3. IntelliJ creates a Run Configuration automatically named **SqlensApplication**
4. The console shows: `Started SqlensApplication in 3.2 seconds (process running on port 8080)`

**Alternative — PowerShell (from project root):**

```powershell
cd D:\02_IT\01_Aplication\15_SQLens\sqlens\backend\sqlens-backend

# First run: download dependencies and compile
mvn clean install -DskipTests

# Start dev server
mvn spring-boot:run
```

Verify it's running:

```powershell
# In a new PowerShell window
Invoke-RestMethod http://localhost:8080/actuator/health
# Returns: @{status=UP}
```

Or open `http://localhost:8080/actuator/health` in your browser — you should see `{"status":"UP"}`.

### 2.5 Run Backend Tests from IntelliJ

**Unit tests:**
- Right-click on `src/test/java` → **Run All Tests**
- Or right-click a specific test class → **Run**

**All tests with coverage:**
- Right-click on `src/test/java` → **Run All Tests with Coverage**
- Coverage report appears in IntelliJ's Coverage panel

**Via PowerShell:**

```powershell
cd D:\02_IT\01_Aplication\15_SQLens\sqlens\backend\sqlens-backend

# Unit tests only (fast)
mvn test -Dtest="*Test"

# Integration tests only
mvn test -Dtest="*IT"

# All tests + coverage report
mvn verify

# Open coverage report in browser
start target\site\jacoco\index.html
```

---

## 3. Frontend Setup (Angular)

### 3.1 Install Node.js 20 LTS

**Option A — Via winget:**

```powershell
winget install OpenJS.NodeJS.LTS
```

Restart PowerShell after installation.

**Option B — Via nvm-windows (recommended if you work with multiple Node versions):**

1. Download nvm-windows from https://github.com/coreybutler/nvm-windows/releases
2. Run `nvm-setup.exe`
3. After installation, open a **new** PowerShell as Administrator:

```powershell
nvm install 20
nvm use 20
node -v    # v20.x.x
npm -v     # 10.x.x
```

**Option C — Manual installer:**

1. Go to https://nodejs.org → download **LTS (20.x)** Windows Installer (`.msi`)
2. Run the installer with default options — it adds Node and npm to PATH
3. Restart PowerShell and verify `node -v`

### 3.2 Install Angular CLI

```powershell
npm install -g @angular/cli@17
ng version
# Angular CLI: 17.x.x
```

> If PowerShell blocks the script with an execution policy error:
> ```powershell
> Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
> ```

### 3.3 Create the Angular Project inside the frontend module

The `frontend\` folder already exists with its `pom.xml`. Now scaffold the Angular project **inside** it:

```powershell
cd D:\02_IT\01_Aplication\15_SQLens\sqlens\frontend

ng new sqlens-frontend --directory . --skip-git --style=scss --routing=true
```

Flag explanations:
| Flag | Why |
|------|-----|
| `--directory .` | Create Angular files in the current folder (alongside `pom.xml`) |
| `--skip-git` | Don't create a nested `.git` repo — we use the root git repo |
| `--style=scss` | SCSS for Angular Material theming |
| `--routing=true` | Angular Router needed for diagram navigation |

After `ng new`, install additional libraries:

```powershell
# JointJS — diagram rendering
npm install jointjs

# Angular Material — UI components
ng add @angular/material

# RxJS is already included with Angular
```

### 3.4 Configure the Frontend

**`src/environments/environment.ts`:**
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
};
```

**`src/environments/environment.prod.ts`:**
```typescript
export const environment = {
  production: true,
  apiUrl: '/api',          // relative — served from same JAR in production
};
```

**`proxy.conf.json`** (create in `frontend\` root) — forwards `/api/*` to backend in dev:

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```

**`angular.json`** — add proxy config to the `serve` target:

```json
"serve": {
  "builder": "@angular-devkit/build-angular:dev-server",
  "options": {
    "proxyConfig": "proxy.conf.json"
  }
}
```

**`package.json`** — verify the `build` script uses production config (required by `frontend-maven-plugin`):

```json
"scripts": {
  "build": "ng build --configuration=production",
  "start": "ng serve"
}
```

> **Note on Angular output path:** With Angular 17's application builder, `ng build` outputs to `dist/sqlens-frontend/browser/`. This matches what `frontend/pom.xml` expects. If IntelliJ or `ng new` generated a different name, update the `<directory>` path in `frontend/pom.xml` accordingly.

### 3.5 Install Dependencies and Run the Frontend

```powershell
cd D:\02_IT\01_Aplication\15_SQLens\sqlens\frontend
ng serve
```

Expected output:

```
✔ Browser application bundle generation complete.
** Angular Live Development Server is listening on localhost:4200 **
  → Local:   http://localhost:4200/
```

Open `http://localhost:4200` in your browser (Chrome recommended).

### 3.6 Run Frontend Tests

```powershell
cd D:\02_IT\01_Aplication\15_SQLens\sqlens\frontend

# Unit tests (Jest, watch mode — reruns on file changes)
npm test

# Unit tests (single run with coverage)
npm run test:ci

# Open coverage report
start coverage\index.html
```

---

## 4. Running the Application

### Mode A — Development (two servers, recommended for daily work)

Backend and frontend run separately. Angular dev server proxies API calls to Spring Boot.

**IntelliJ Compound Run Configuration (start both with one click):**

1. `Run → Edit Configurations → + → Compound`
2. Name it: `SQLens Dev`
3. Add **SqlensBackendApplication** (Spring Boot)
4. Add **npm** config:
   - Package.json: `D:\02_IT\01_Aplication\15_SQLens\sqlens\frontend\package.json`
   - Command: `run` / Scripts: `start`
5. Click OK → select `SQLens Dev` → ▶

**Or two PowerShell windows:**

```powershell
# Window 1 — Backend (port 8080)
cd D:\02_IT\01_Aplication\15_SQLens\sqlens\backend\sqlens-backend
mvn spring-boot:run

# Window 2 — Frontend (port 4200)
cd D:\02_IT\01_Aplication\15_SQLens\sqlens\frontend
ng serve
```

Open `http://localhost:4200` — Angular proxies `/api/*` → `http://localhost:8080`.

> **Tip:** Use **Windows Terminal** (`winget install Microsoft.WindowsTerminal`) for tabs.

---

### Mode B — Production Build (single JAR, everything on port 8080)

```powershell
# From the ROOT — runs both modules in order
cd D:\02_IT\01_Aplication\15_SQLens\sqlens
mvn clean package

# Run the single self-contained JAR
java -jar backend\sqlens-backend\target\sqlens-backend-0.0.1-SNAPSHOT.jar
```

Open `http://localhost:8080` — Spring Boot serves both the API and the Angular app.

> **First time is slow** — Maven downloads Node 20 locally into `frontend\target\`. Subsequent builds reuse it.

---

### Mode C — Backend only (skip frontend build, fastest for API dev)

```powershell
cd D:\02_IT\01_Aplication\15_SQLens\sqlens
mvn clean package -pl backend/sqlens-backend -am -DskipTests
```

---

## 5. E2E Tests (Playwright)

### 5.1 Install Playwright Browsers

```powershell
cd D:\02_IT\01_Aplication\15_SQLens\sqlens\frontend
npx playwright install --with-deps chromium firefox webkit
```

> On Windows, Playwright downloads browser binaries to `%USERPROFILE%\AppData\Local\ms-playwright`.

### 5.2 Run E2E Tests

Both servers must be running (see section 4), or Playwright starts them automatically via `webServer` config:

```powershell
cd D:\02_IT\01_Aplication\15_SQLens\sqlens\frontend

# Run all E2E tests
npx playwright test

# Run a specific suite
npx playwright test e2e\tests\01-core-visualization.spec.ts

# Visual UI mode (great for debugging)
npx playwright test --ui

# Show HTML report after run
npx playwright show-report
```

### 5.3 Playwright in IntelliJ

Install the **Playwright** plugin for IntelliJ:
1. `File → Settings → Plugins → Marketplace`
2. Search **Playwright** → Install → Restart IntelliJ
3. Right-click any `.spec.ts` file → **Run with Playwright**

---

## 6. IntelliJ IDEA Configuration (Windows)

### 6.1 Project SDK

1. `File → Project Structure` (Ctrl+Alt+Shift+S)
2. `Project → SDK` → select **Temurin 17**
3. `Project → Language level` → **17**

### 6.2 Run Configurations

**Recommended saved configurations:**

| Name | Type | Details |
|------|------|---------|
| `Backend` | Spring Boot | Main class: `com.sqlens.SqlensApplication` |
| `Backend Tests` | JUnit | All in `src/test/java` |
| `Backend IT Tests` | JUnit | Pattern: `**/*IT` |
| `Frontend` | npm | Script: `start`, package.json: `frontend\package.json` |
| `SQLens Full Stack` | Compound | Backend + Frontend |

To create Spring Boot run config:
1. `Run → Edit Configurations → + → Spring Boot`
2. Main class: `com.sqlens.SqlensApplication`
3. Active profiles: (leave empty for default)
4. JRE: Temurin 17

### 6.3 Plugins to Install

`File → Settings → Plugins → Marketplace`:

| Plugin | Why |
|--------|-----|
| **Lombok** | Required if DTOs use `@Data`, `@Builder` annotations |
| **Angular and AngularJS** | Syntax support for `.html` templates |
| **Prettier** | Auto-format TypeScript/HTML on save |
| **Playwright** | Run/debug Playwright tests from IDE |

### 6.4 Code Style Settings

```
File → Settings → Editor → Code Style
├── Java: GoogleStyle or IntelliJ default
└── TypeScript: Prettier (delegate to Prettier plugin)
```

Enable annotation processing (required for Lombok):
`File → Settings → Build → Compiler → Annotation Processors → Enable`

---

## 7. VS Code (Optional — for Angular frontend)

If you prefer VS Code for Angular work alongside IntelliJ for Java:

**Install VS Code:** https://code.visualstudio.com

**Open frontend folder:**
```powershell
cd D:\02_IT\01_Aplication\15_SQLens\sqlens\frontend
code .
```

**Recommended extensions** (`.vscode\extensions.json`):

```json
{
  "recommendations": [
    "angular.ng-template",
    "dbaeumer.vscode-eslint",
    "esbenp.prettier-vscode",
    "firsttris.vscode-jest-runner",
    "ms-playwright.playwright"
  ]
}
```

**Workspace settings** (`.vscode\settings.json`):

```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "typescript.preferences.importModuleSpecifier": "relative",
  "playwright.reuseBrowser": true
}
```

---

## 8. Project Scripts Reference

### Backend (`sqlens\backend\sqlens-backend`)

| Command (PowerShell) | Description |
|----------------------|-------------|
| `mvn spring-boot:run` | Start dev server (port 8080) |
| `mvn test -Dtest="*Test"` | Unit tests only |
| `mvn test -Dtest="*IT"` | Integration tests only |
| `mvn verify` | All tests + coverage report |
| `mvn clean install -DskipTests` | Build jar without tests |
| `start target\site\jacoco\index.html` | Open coverage report in browser |

### Frontend (`sqlens\frontend`)

| Command (PowerShell) | Description |
|----------------------|-------------|
| `ng serve` | Dev server (port 4200, hot reload) |
| `npm test` | Jest unit tests (watch mode) |
| `npm run test:ci` | Jest (single run, with coverage) |
| `ng build` | Production build to `dist\` |
| `ng lint` | Run ESLint |
| `npx playwright test` | All E2E tests |
| `npx playwright test --ui` | E2E with visual debugger |
| `npx playwright show-report` | Open last HTML report |
| `start coverage\index.html` | Open Jest coverage in browser |

---

## 9. Ports and Services

| Service | Port | URL |
|---------|------|-----|
| Angular dev server | 4200 | `http://localhost:4200` |
| Spring Boot API | 8080 | `http://localhost:8080` |
| Spring Actuator health | 8080 | `http://localhost:8080/actuator/health` |
| Swagger UI | 8080 | `http://localhost:8080/swagger-ui.html` |

---

## 10. Environment Profiles

### Spring Profiles

| Profile | Usage | How to activate in IntelliJ |
|---------|-------|-----------------------------|
| `default` | Local dev, DEBUG logging | Leave Active Profiles blank |
| `test` | Integration tests | Set in Run Config: Active Profiles = `test` |
| `prod` | Production, INFO logging, HTTPS | `Active Profiles = prod` |

To set profile in IntelliJ Run Configuration:
`Run → Edit Configurations → SqlensApplication → Spring Boot tab → Active Profiles`

### Frontend Environments

| File | Used when |
|------|-----------|
| `environment.ts` | `ng serve` (local dev) |
| `environment.prod.ts` | `ng build --configuration=production` |

---

## 11. Common Issues & Fixes (Windows)

### Port 8080 already in use

```powershell
# Find which process is using port 8080
netstat -ano | findstr :8080

# Note the PID from the last column, then kill it
taskkill /PID <PID> /F
```

Or in IntelliJ: check the **Run** panel for a running **SqlensApplication** → click the red ■ Stop button.

### Port 4200 already in use

```powershell
netstat -ano | findstr :4200
taskkill /PID <PID> /F
```

### Angular CORS error in browser

- Confirm backend is running: open `http://localhost:8080/actuator/health`
- Confirm `proxy.conf.json` exists in `frontend\` root
- Confirm `angular.json` references the proxy: `"proxyConfig": "proxy.conf.json"`

### `ng serve` — PowerShell execution policy error

```powershell
# Run once as Administrator
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

### `mvn` is not recognized

Maven may not be on PATH. Options:
- **Use IntelliJ's bundled Maven**: `File → Settings → Build → Build Tools → Maven → Maven home path` → select **Bundled (Maven 3)**
- **Install Maven manually**: download from https://maven.apache.org/download.cgi, extract to `C:\tools\maven`, add `C:\tools\maven\bin` to System PATH
- **Alternatively**, use the Maven wrapper inside the project: `.\mvnw spring-boot:run` (works without global Maven install)

### `ng` is not recognized after installing Angular CLI

Angular CLI installs globally but npm global bin may not be on PATH:

```powershell
# Find npm global bin path
npm config get prefix
# e.g.: C:\Users\roman\AppData\Roaming\npm

# Add it to PATH:
# Win + R → sysdm.cpl → Advanced → Environment Variables
# Edit Path → Add: C:\Users\roman\AppData\Roaming\npm
```

Restart PowerShell and retry `ng version`.

### Playwright can't find browser executables

```powershell
cd D:\02_IT\01_Aplication\15_SQLens\sqlens\frontend
npx playwright install --with-deps
```

### Maven can't find JDK 17 (uses wrong Java version)

```powershell
# Check which Java Maven finds
mvn -version

# If it shows a different version, set JAVA_HOME explicitly:
# Win + R → sysdm.cpl → Advanced → Environment Variables
# Set JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.x.x.x-hotspot
# Also ensure %JAVA_HOME%\bin is first in Path
```

Or configure in IntelliJ's Maven settings:
`File → Settings → Build → Build Tools → Maven → Runner → JRE → select Temurin 17`

### `npm ci` fails — wrong Node version

```powershell
node -v   # must be 20.x

# If using nvm-windows:
nvm use 20

# Re-run
npm ci
```

### IntelliJ shows "Cannot resolve symbol" for Spring annotations

Wait for Maven import to complete, or force it:
`Maven panel (right sidebar) → ↻ Reload All Maven Projects`

---

## 12. First Run Checklist

**One-time setup:**
```
□ JDK 17 installed (java -version shows 17.x)
□ JAVA_HOME set correctly (mvn -version shows Java 17)
□ Node.js 20 LTS installed (node -v shows 20.x)
□ Angular CLI 17 installed (ng version shows 17.x)
□ Root pom.xml opened in IntelliJ (sqlens\pom.xml → Open as Project)
□ IntelliJ shows 3 modules in Maven panel: sqlens, sqlens-frontend, sqlens-backend
```

**Angular project setup (first time only):**
```
□ ng new sqlens-frontend --directory . --skip-git --style=scss --routing=true
  (run from sqlens\frontend\)
□ npm install jointjs
□ ng add @angular/material
□ proxy.conf.json created in frontend\
□ angular.json updated with proxyConfig
□ package.json build script uses --configuration=production
```

**Development workflow verification:**
```
□ Backend starts: mvn spring-boot:run → http://localhost:8080/actuator/health → UP
□ Frontend starts: ng serve → http://localhost:4200 shows Angular app
□ API proxy works: Angular → /api/sql/analyze → Spring Boot responds
□ Paste a test SQL query → diagram renders correctly
□ Backend unit tests pass (mvn test in backend\sqlens-backend\)
□ Frontend unit tests pass (npm run test:ci in frontend\)
```

**Production build verification:**
```
□ Full build works: mvn clean package (from sqlens\ root)
□ Single JAR runs: java -jar backend\sqlens-backend\target\sqlens-backend-*.jar
□ http://localhost:8080 serves Angular UI (not just API)
□ http://localhost:8080/api/sql/analyze returns 400 for empty body
```

**E2E:**
```
□ Playwright browsers installed (npx playwright install --with-deps)
□ At least one E2E suite passes (npx playwright test e2e\tests\01-core-visualization.spec.ts)
```

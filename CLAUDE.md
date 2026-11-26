# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Working rules (TaskFlow – learning project)

- The owner is learning DevOps. Explain WHY before changing anything.
- Never edit files without showing the plan first and getting approval.
- Never put secrets (JWT secret, LDAP passwords, tokens) in tracked files.
  Use environment variables or an untracked file
  (e.g. `.env` / `application-local.yaml`, listed in `.gitignore`).

## Learning workflow — `plan.md` is the roadmap

- **`plan.md` (repo root, git-ignored — the owner's private copy) is the single source of truth** for what to build next. It holds the full step-by-step guide (Phases 1–11 = Stage 1; Stages 2–3 outlined), a **Progress** checklist and a **Differences in this repo** table. The owner may edit it at any time, so re-read the relevant phase (and the Differences table) at the start of each phase instead of relying on memory.
- Work phase by phase, in order. Every phase uses the loop **Learn → Build → Verify → Break it → Commit**: explain the concept/analogy first; the owner types the code; run the phase's "Done when" check; suggest the "Break it" exercise; one commit per phase.
- Claude's role is pair programmer and reviewer, not autopilot: explain line by line, review diffs (bugs, leaked secrets), explain errors before fixing them, and quiz the owner on files. Write whole files only when the owner explicitly asks.
- When a phase's "Done when" check passes, tick it in `plan.md` → Progress (after approval, like any edit). If the repo deviates from the guide, record it in the Differences table rather than silently diverging.
- The guide was written for `C:\dev\TaskFlow` and PowerShell; this repo lives at `D:\TaskFlow\TaskFlow`.
- Uncommitted work is lost by a forced branch checkout/rollback (happened 2026-11-26). Suggest committing at the end of each step.

## Stack

**Target:** Java 21, Spring Boot 4.x, Spring Security + LDAP + JWT,
React + Vite frontend, Docker, K3s, GitHub Actions CI/CD.

Technology by phase (details in `plan.md`):

| Phase | Technology |
|---|---|
| 1 | Git monorepo, Spring Boot (Web, Actuator), React + Vite |
| 2 | Spring Security, LDAP (embedded UnboundID in dev/tests), JWT HS256 via OAuth2 Resource Server, Spring profiles |
| 3–5 | Multi-stage Dockerfiles, Docker Compose, lldap, Nginx (static hosting + `/api` reverse proxy) |
| 6–7 | GitHub Actions CI, JaCoCo, SonarQube Cloud, Docker Hub (SHA-tagged images) |
| 8 | Kubernetes (Docker Desktop): Deployments, Services, ConfigMaps, Secrets, probes, PVC, Kustomize |
| 9 | Ubuntu VM, SSH keys, ufw |
| 10 | K3s, Traefik Ingress, cert-manager + Let's Encrypt |
| 11 | CD: GitHub Actions → SSH → `scripts/deploy.sh` → `kubectl rollout` |

**Current state: Phase 2 in progress.** Phase 1 done except committing the Phase 1 work and `chmod +x` on `mvnw` in git (Step 1.5; the GitHub remote `origin` already exists). Phase 1 code: `api/PingController` (`GET /api/public/ping` → `{"status":"ok","service":"taskflow-backend"}`) + `PingControllerTest`, Actuator `health,info` + probes in `application.yaml`, `App.jsx` shows "Backend says: …". No LDAP, JWT, Dockerfile, K8s manifests or CI workflows yet.

**Known state until Step 2.4:** Spring Security is on the classpath with no custom `SecurityFilterChain`, so `/api/public/ping` returns 401 and the React page shows "backend unreachable". Phase 2's `SecurityConfig` fixes it (permit `/api/public/**`, `/api/auth/login`, `/actuator/health/**`).

## Repo layout (monorepo)

Two independent apps in one repo (one CI pipeline, one history; separate Docker images / K8s Deployments later). Open the **repo root** in IntelliJ; `backend/pom.xml` is the Maven project.

- `backend/` — Spring Boot 4.2.0 (webmvc + security + actuator), Java 21, Lombok, Maven wrapper. Package `com.taskflow.backend`, entry point `BackendApplication`; `api/PingController` (`GET /api/public/ping`); tests: `@SpringBootTest` context-load + plain unit `api/PingControllerTest`.
- `frontend/` — React 19 + Vite 8 (JavaScript, created by `create-vite`), linted with oxlint. `src/App.jsx` fetches `/api/public/ping` and shows "Backend says: …" (guide Step 1.4).
- Dev wiring: `frontend/vite.config.js` proxies `/api/*` → `http://localhost:8080`, so backend REST endpoints should live under `/api` and the frontend should call relative `/api/...` URLs (no CORS config needed in dev).
- Root `.gitattributes` forces LF for `mvnw`/`*.sh` (CRLF breaks them in Linux containers); root `.gitignore` covers both apps and secrets (`.env`, `*.local`).

## Commands

Both apps together — run from the repo root (root `package.json` uses `concurrently`; Windows-only `dev:backend` script since it relies on cmd.exe resolving `mvnw.cmd`):

```bash
npm run setup      # once after cloning: installs root + frontend dependencies
npm run dev        # backend :8080 + frontend :5173 in one terminal, prefixed logs; Ctrl+C stops both
```

Backend — run from `backend/`, always via the wrapper (`./mvnw` in Git Bash, `mvnw.cmd` in cmd/PowerShell):

```bash
./mvnw clean package                       # build + run tests, produces target/backend-0.0.1-SNAPSHOT.jar
./mvnw compile                             # compile only
./mvnw test                                # all tests
./mvnw test -Dtest=BackendApplicationTests             # single test class
./mvnw test -Dtest=BackendApplicationTests#contextLoads # single test method
./mvnw spring-boot:run                     # run on http://localhost:8080
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081   # alternate port
```

No Java linter/formatter is configured.

Frontend — run from `frontend/`:

```bash
npm install        # after cloning / when package.json changes
npm run dev        # dev server on http://localhost:5173 (with /api proxy)
npm run build      # production build into dist/
npm run lint       # oxlint
npm run preview    # serve dist/ locally
```

No frontend test runner is configured yet.

## Build configuration gotchas

- **Maven wrapper is pinned to 3.9.16** (`backend/.mvn/wrapper/maven-wrapper.properties`). Do not bump it to 3.10.x: IntelliJ IDEA 2025.2's Maven importer fails on 3.10 (`Unable to create injector ... ArtifactHandlerManager`), which leaves the IntelliJ module with no dependencies and language level 1.5, producing `java: error: release version 5 not supported`. Command-line builds are unaffected either way.
- **Toolchains are required.** `maven-toolchains-plugin` selects a JDK matching `${java.version}` (21) from `~/.m2/toolchains.xml`, independent of `JAVA_HOME`. A build on a machine without that file entry fails at the toolchain step.
- Lombok is wired explicitly as an `annotationProcessorPaths` entry in both `default-compile` and `default-testCompile` executions of `maven-compiler-plugin`; keep both in sync if adding other annotation processors.
- The Java version comes from the `java.version` property, which the Spring Boot parent maps to `maven.compiler.release`.
- `spring-boot-docker-compose` is commented out in `pom.xml` and `compose.yaml` has no services. The `HELP.md` warning that the app "won't start" without compose services only applies if that dependency is re-enabled.
- The empty `<licenses>`, `<developers>`, `<scm>` elements in `pom.xml` are intentional overrides to stop inheritance from the Spring Boot parent.

## Runtime behavior

- Spring Security auto-configuration is active with no custom `SecurityFilterChain`: every endpoint requires HTTP Basic/form login with user `user` and a random password printed at startup (`Using generated security password: ...`). Unauthenticated requests return 401 (including `/api/public/ping` until Phase 2's `SecurityConfig`); with the generated credentials `/api/public/ping` works and unknown paths return 404. Exception: `/actuator/health` is reachable without auth by default (returns `{"groups":["liveness","readiness"],"status":"UP"}`), so it can already back K8s liveness/readiness probes — keep it permitted when adding a custom `SecurityFilterChain`.
- "Port 8080 was already in use" usually means a previous `spring-boot:run` instance (often from IntelliJ) is still running. `spring-boot:run` forks a separate `java.exe` (`com.taskflow.backend.BackendApplication`) that can outlive the Maven process — find it with `Get-NetTCPConnection -LocalPort 8080` and stop it.

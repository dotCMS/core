# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Architecture

dotCMS is a hybrid Java/JavaScript headless CMS with the following structure:

- **Backend**: Java 21 runtime (Java 11 compatible code) with Tomcat 9 webapp, Maven build system and OSGi plugins
- **Frontend**: Angular 18.2+ with Nx monorepo architecture and TypeScript
- **Database**: PostgreSQL with pgvector extension for AI features
- **Search**: OpenSearch/Elasticsearch integration
- **Development**: Docker-first with `justfile` command runner

### Key Directories

**Java Backend:**
- `/dotCMS/` - Main Java application module
- `/dotCMS/src/main/java/com/dotcms/` - Core dotCMS Java codebase
- `/dotCMS/src/main/java
- /com/dotmarketing/` - Legacy Liferay-based code
- `/dotcms-integration/` - Integration test suite
- `/tools/dotcms-cli/` - Command line interface for content management

**Frontend:**
- `/core-web/apps/dotcms-ui/` - Main Angular admin UI application
- `/core-web/libs/dotcms/` - TypeScript SDK for dotCMS REST APIs
- `/core-web/libs/` - Shared Angular libraries and components
- `/examples/` - Framework integration examples (Next.js, Angular, Vue, Astro)

## Essential Commands

### Prerequisites Setup
```bash
# Install all macOS dependencies (Java, Docker, Git)
just install-all-mac-deps

# Or manually: brew install just docker
# Then: just check-docker-mac
```

### Build Commands
```bash
# Quick build without tests
just build

# Build without Docker image creation
just build-no-docker

# Production build
just build-prod

# Build specific module (e.g., core)
just build-select-module dotcms-core

# Full build with all tests
just build-test-full
```

### Development Server
```bash
# Start backend on port 8080
just dev-start-on-port 8080

# Start with debug (port 5005)
just dev-run-debug-suspend 8082

# Stop development server
just dev-stop

# Frontend development (separate terminal)
cd core-web && yarn install && nx serve dotcms-ui
# Access at http://localhost:4200/dotAdmin
```

### Testing
```bash
# Integration tests
just test-integration

# E2E tests
just test-e2e-java
just test-e2e-node

# Postman API tests
just test-postman

# Frontend tests
cd core-web && nx test dotcms-ui
cd core-web && yarn test:dotcms
```

### Frontend Development
```bash
cd core-web

# Install dependencies
yarn install

# Start dev server (proxies to backend on :8080)
nx serve dotcms-ui

# Build for production
nx run dotcms-ui:build:production

# Run tests
nx test dotcms-ui
yarn test:dotcms

# Lint and format
yarn lint:dotcms
nx format:write
```

## Development Workflow

1. **Backend changes**: Use `just build-core-only` for quick rebuilds during development
2. **Frontend changes**: Use `nx serve dotcms-ui` with hot reloading
3. **Integration testing**: Use `just test-integration-ide` to prepare environment for IDE debugging
4. **Docker development**: All backend development uses Docker containers by default

## Key Configuration Files

- `/justfile` - Command runner with development shortcuts
- `/core-web/proxy-dev.conf.mjs` - Frontend-to-backend proxy configuration  
- `/core-web/nx.json` - Nx workspace configuration
- `/pom.xml` - Root Maven configuration
- `/dotCMS/src/main/resources/dotmarketing-config.properties` - Backend configuration

## Architecture Notes

- **Hybrid monolith**: Java backend serves REST APIs, Angular frontend consumes them
- **OSGi plugins**: Backend uses OSGi for modular plugin architecture
- **Nx monorepo**: Frontend uses Nx for efficient builds and shared libraries
- **Docker-first**: All development and deployment uses containerization
- **TypeScript SDK**: Strongly-typed client library for dotCMS APIs located in `/core-web/libs/dotcms/`
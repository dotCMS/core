[![Publish to NPM](https://github.com/dotCMS/core-web/actions/workflows/publish.yml/badge.svg)](https://github.com/dotCMS/core-web/actions/workflows/publish.yml)
[![DotCMS/core-web Tests](https://github.com/dotCMS/core-web/actions/workflows/main.yml/badge.svg)](https://github.com/dotCMS/core-web/actions/workflows/main.yml)

# DotCMS Frontend
This repository contains the frontend infrastructure for the DotCMS admin system, includes the main app and all the internal libraries.

## Content

| Name                                                                                               | Type | Location                     | Build with | Description                                  |
|----------------------------------------------------------------------------------------------------|------|------------------------------|------------|----------------------------------------------|
| [dotcms-ui](https://github.com/dotCMS/core-web/tree/master/apps/dotcms-ui)                         | app  | `apps/dotcms-ui`             | Angular    | Main UI application                          |
| [dotcms](https://github.com/dotCMS/core-web/tree/master/libs/dotcms)                               | lib  | `libs/dotcms`                | Typescript | JavaScript SDK for DotCMS API's              |
| [dot-layout-grid](https://github.com/dotCMS/core-web/tree/master/libs/dot-layout-grid)             | lib  | `libs/dot-layout-grid`       | Angular    | Components for layout editor                 |
| [dot-rules](https://github.com/dotCMS/core-web/tree/master/libs/dot-rules)                         | lib  | `libs/dot-rules`             | Angular    | Components and services for rules            |
| [dotcms-field-elements](https://github.com/dotCMS/core-web/tree/master/libs/dotcms-field-elements) | lib  | `libs/dotcms-field-elements` | Stenciljs  | Web components for form builder (deprecated) |
| [dotcms-js](https://github.com/dotCMS/core-web/tree/master/libs/dotcms-js)                         | lib  | `libs/dotcms-js`             | Angular    | Angular @injectables for DotCMS API          |
| [dotcms-models](https://github.com/dotCMS/core-web/tree/master/libs/dotcms-models)                 | lib  | `libs/dotcms-models`         | Typescript | DotCMS interfaces and types                  |


## NX Monorepo
This project was generated using [Nx](https://nx.dev).

🔎 **Nx is a set of Extensible Dev Tools for Monorepos.**

### Quick Start & Documentation

[Nx Documentation](https://nx.dev/angular)

[10-minute video showing all Nx features](https://nx.dev/angular/getting-started/what-is-nx)

[Interactive Tutorial](https://nx.dev/angular/tutorial/01-create-application)

### Adding capabilities to your workspace

Nx supports many plugins which add capabilities for developing different types of applications and different tools.

These capabilities include generating applications, libraries, etc as well as the devtools to test, and build projects as well.

Below are our core plugins:

- [Angular](https://angular.io)
  - `ng add @nrwl/angular`
- [React](https://reactjs.org)
  - `ng add @nrwl/react`
- Web (no framework frontends)
  - `ng add @nrwl/web`
- [Nest](https://nestjs.com)
  - `ng add @nrwl/nest`
- [Express](https://expressjs.com)
  - `ng add @nrwl/express`
- [Node](https://nodejs.org)
  - `ng add @nrwl/node`

There are also many [community plugins](https://nx.dev/nx-community) you could add.

### Generate an application

Run `ng g @nrwl/angular:app my-app` to generate an application.

> You can use any of the plugins above to generate applications as well.

When using Nx, you can create multiple applications and libraries in the same workspace.

### Generate a library

Run `ng g @nrwl/angular:lib my-lib` to generate a library.

> You can also use any of the plugins above to generate libraries as well.

Libraries are shareable across libraries and applications. They can be imported from `@dotcms/mylib`.

### Development server

Run `ng serve my-app` for a dev server. Navigate to http://localhost:4200/. The app will automatically reload if you change any of the source files.

### Code scaffolding

Run `ng g component my-component --project=my-app` to generate a new component.

### Build

Run `ng build my-app` to build the project. The build artifacts will be stored in the `dist/` directory. Use the `--prod` flag for a production build.

### Running unit tests

Run `ng test my-app` to execute the unit tests via [Jest](https://jestjs.io).

Run `nx affected:test` to execute the unit tests affected by a change.

### Running end-to-end tests

Run `ng e2e my-app` to execute the end-to-end tests via [Cypress](https://www.cypress.io).

Run `nx affected:e2e` to execute the end-to-end tests affected by a change.

### Understand your workspace

Run `nx dep-graph` to see a diagram of the dependencies of your projects.

### Further help

Visit the [Nx Documentation](https://nx.dev/angular) to learn more.

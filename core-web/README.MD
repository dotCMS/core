# DotCMS Frontend

This folder contains the frontend infrastructure for the DotCMS admin system, includes the main app and all the internal libraries.

## Content

| Name                                                                                               | Type | Location                     | Build with | Description                                                 |
| -------------------------------------------------------------------------------------------------- | ---- | ---------------------------- | ---------- | ----------------------------------------------------------- |
| [dotcms-ui](https://github.com/dotCMS/core-web/tree/main/apps/dotcms-ui)                         | app  | `apps/dotcms-ui`             | Angular    | Main UI application                                         |
| [dotcdn](https://github.com/dotCMS/core-web/tree/main/apps/dotcdn)                               | app  | `apps/dotcdn`                | Angular    | App for the dotCDN plugin                                   |
| [dotcms-block-editor](https://github.com/dotCMS/core-web/tree/main/apps/dotcms-block-editor)     | app  | `apps/dotcms-block-editor`   | Angular    | App to build de block-editor as a Web Component             |
| [dotcms](https://github.com/dotCMS/core-web/tree/main/libs/dotcms)                               | lib  | `libs/dotcms`                | Typescript | JavaScript SDK for DotCMS API's                             |
| [dot-layout-grid](https://github.com/dotCMS/core-web/tree/main/libs/dot-layout-grid)             | lib  | `libs/dot-layout-grid`       | Angular    | Components for layout editor                                |
| [block-editor](https://github.com/dotCMS/core-web/tree/main/libs/block-editor)                   | lib  | `libs/block-editor`          | TitTap     | Block editor components                                     |
| [dot-rules](https://github.com/dotCMS/core-web/tree/main/libs/dot-rules)                         | lib  | `libs/dot-rules`             | Angular    | Components and services for rules                           |
| [dotcms-js](https://github.com/dotCMS/core-web/tree/main/libs/dotcms-js)                         | lib  | `libs/dotcms-js`             | Angular    | Angular @injectables for DotCMS API                         |
| [dotcms-models](https://github.com/dotCMS/core-web/tree/main/libs/dotcms-models)                 | lib  | `libs/dotcms-models`         | Typescript | DotCMS interfaces and types                                 |
| [dotcms-scss](https://github.com/dotCMS/core-web/tree/main/libs/dotcms-scss)                     | lib  | `libs/dotcms-scss`           | SCSS       | SCSS shared files for theme Angular PrimeNG and Dijit Theme |

## dotCMS core-web Monorepo

This monorepo, powered by [Nx](https://nx.dev), contains everything related to the frontend development for dotCMS.

### Installing Dependencies

Run `yarn install` to install all project dependencies.

### Development Server

To start the development server, run `nx serve dotcms-ui`. Visit [http://localhost:4200/](http://localhost:4200/). The app will automatically reload if you make any changes to the source files. We have set up a proxy to the backend server (on port 8080), allowing you to run both the backend server (by building the main branch or using Docker) and the frontend server simultaneously.

### Building the Project

We have three main applications to build:
- **dotcms-ui**: Run `nx build dotcms-ui`.
- **dotcms-block-editor**: Run `nx build dotcms-block-editor`.
- **dotcms-binary-field-builder**: Run `nx build dotcms-binary-field-builder`.

### Running Unit Tests

To run all unit tests for projects currently testable, use `yarn run test:dotcms`. To test a specific app or library, run `nx test {name-of-the-app-or-lib}`.

Use `nx affected:test` to execute the unit tests affected by recent changes.

### Understanding Your Workspace

Run `nx dep-graph` to see a diagram of your projects' dependencies. This will help you better understand the structure and relationships in your monorepo.

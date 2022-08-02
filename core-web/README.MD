[![DotCMS/core-web Tests](https://github.com/dotCMS/core/actions/workflows/main.yml/badge.svg)](https://github.com/dotCMS/core/actions/workflows/main.yml)

# DotCMS Frontend

This repository contains the frontend infrastructure for the DotCMS admin system, includes the main app and all the internal libraries.

## Apps

| Name                                                                                      | Type | Location                   | Build with | Description                 |
|-------------------------------------------------------------------------------------------| ---- |----------------------------| ---------- |-----------------------------|
| [dotcms-ui](https://github.com/dotCMS/core/tree/master/core-web/apps/dotcms-ui)           | app  | `apps/dotcms-ui`           | Angular    | Main UI application         |
| [dotcms-block-editor](https://github.com/dotCMS/core/tree/master/core-web/apps/dotcms-block-editor) | app  | `apps/dotcms-block-editor` | Angular    | Block Editor application |

## Libraries

| Name                       | Type | Location                        | Build with | Description                                |
|----------------------------| ---- |---------------------------------| ---------- |--------------------------------------------|
| [block-editor](https://github.com/dotCMS/core/tree/master/core-web/libs/block-editor)          | lib  | `libs/block-editor`             | Angular | DotCMS's Block Editor lib                  |
| [dot-layout-grid](https://github.com/dotCMS/core/tree/master/core-web/libs/dot-layout-grid)       | lib  | `libs/dot-layout-grid`          | Angular    | Components for layout editor               |
| [dot-primeng-theme-styles](https://github.com/dotCMS/core/tree/master/core-web/libs/dot-primeng-theme-styles) | lib  | `libs/dot-primeng-theme-styles` | Angular    | PrimeNG's Styles                           |
| [dot-rules](https://github.com/dotCMS/core/tree/master/core-web/libs/dot-rules)             | lib  | `libs/dot-rules`                | Angular  | Components and services for rules          |
| [dotcms](https://github.com/dotCMS/core/tree/master/core-web/libs/dotcms)                | lib  | `libs/dotcms`                   | Typescript    | JavaScript SDK for DotCMS API's            |
| [dotcms-field-elements](https://github.com/dotCMS/core/tree/master/core-web/libs/dotcms-field-elements) | lib  | `libs/dotcms-field-elements`    | Stenciljs | Web components for form builder (deprecated) |
| [dotcms-js](https://github.com/dotCMS/core/tree/master/core-web/libs/dotcms-js)             | lib  | `libs/dotcms-js`                | Angular | Angular @injectables for DotCMS API        |
| [dotcms-models](https://github.com/dotCMS/core/tree/master/core-web/libs/dotcms-models)         | lib  | `libs/dotcms-models`            | Typescript | DotCMS interfaces and types                |
| [dotcms-webcomponents](https://github.com/dotCMS/core/tree/master/core-web/libs/dotcms-webcomponents)  | lib  | `libs/dotcms-webcomponents`     | Stenciljs | DotCMS Web components                      |
| [ui](https://github.com/dotCMS/core/tree/master/core-web/libs/ui)                    | lib  | `libs/ui`                       | Angular | Angular Components                         |
| [utils](https://github.com/dotCMS/core/tree/master/core-web/libs/utils)                 | lib  | `libs/utils`                    | Angular | Angular @injectables for DotCMS API        |

## Requirements
**We recommend using [nvm](https://github.com/nvm-sh/nvm) to manage the NodeJS versions**.
```bash
$ node -v
v16.14.2
$ nvm -v
0.39.1
```

## Installation
We use `npm` to install all the js libraries

```bash
$ npm install
```

## Development
We use [nx](https://github.com/nrwl/nx) to manage the monorepo please take a look to the [documentation](https://nx.dev/getting-started/intro).


### Run dotcms-ui

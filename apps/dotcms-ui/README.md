# DotCMS UI

Main UI application for DotCMS admin.

[![Publish to NPM](https://github.com/dotCMS/core-web/actions/workflows/publish.yml/badge.svg)](https://github.com/dotCMS/core-web/actions/workflows/publish.yml)
[![DotCMS/core-web Tests](https://github.com/dotCMS/core-web/actions/workflows/main.yml/badge.svg)](https://github.com/dotCMS/core-web/actions/workflows/main.yml)

## Requirements

[Nodejs and npm](https://nodejs.org/en/)

Don't forget to run `npm install`

## Build and run in local DotCMS instance

```sh
npm run build:dev -- --output-path your/path/to/dotadmin
```

Example path: `~/dev/dotcms/tomcat9/webapps/ROOT/dotAdmin`

## Run unit tests

Running headless browser
`npm run test dotcms-ui`

Or you can pass the browser
`npm run test dotcms-ui -- --browsers=Chrome`

# File structure

## Portlet or feature module

A significant functionality, probably a portlet, typically will contain components, services, and other modules.

The folder of the portlet or feature should encapsulate all the components, modules, directives, services, etc.

```
module-name/
├── components/
│   ├── index.ts
│   ├── component-one
│   │   ├── components/
│   │   ├── services/
│   │   ├── models/
│   │   ├── utils/
│   │   ├── component.one.module.ts
│   │   ├── component.one.component.html
│   │   ├── component.one.component.scss
│   │   ├── component.one.component.ts
│   │   ├── component.one.component.spec.ts
│   ├── component-two
│   │   └── ...
├── services/
│   ├── index.ts
│   ├── service.one.service.ts
│   ├── service.one.service.spec.ts
│   ├── service.two.service.ts
│   └── service.two.service.spec.ts
├── models/
│   ├── index.ts
│   ├── model-one.ts
│   ├── model-two.ts
│   └── model-xxx.ts
├── utils/
│   ├── index.ts
│   ├── util-one.ts
│   ├── util-two.ts
│   └── util-xxx.ts
├── component-name.module.ts
├── component-name.component.ts
├── component-name.component.scss
├── component-name.component.html
├── component-name.component.spec.ts
├── component-name.module..ts
└── component-name-routing.module..ts
```

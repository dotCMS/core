# DotCMS UI

This project was created using [angular-cli](https://cli.angular.io/) it means you can use `ng` with all the commands and params.

## Before start

1. Install node and npm
2. Install angular-cli: `npm i @angular/cli`
3. `npm install`

## Build and run in local DotCMS instance

Watch and compile your files in development mode and copy it into `dotAdmin` folder in your `tomcat` folder.

1. `npm run build:dev -- --output-path your/path/to/dotadmin` (ex. ~/dev/dotcms/tomcat8/webapps/ROOT/dotAdmin)
2. Open http://localhost:8080/dotAdmin

## Run unit tests

To test `dotcms-ui` (main app):

`ng test dotcms-ui --browsers=Chrome`

To run tests for any internal library:

`ng test {library-name} --browsers=Chrome`

## Publish to NPM

**You need the credentials for dotcms npm account**

### Publish a dev verison

```
$ npm run publish:dev
```

This will build a production ready app, update the `package.json` version using a timestamp (ex: 5.0.1-1537889821825) and publish that as latest version to npm.

### Publish a release

```
$ npm run build:prod
```

This will build a production ready app inside `dist` folder, then:

```
$ cd dist
```

Edit the `package.json` to update manually the version of the module and finally run:

```
$ npm publish
```

## StorybookJS

> “Storybook is a powerful frontend workshop environment tool that allows teams to design, build, and organize UI components (and even full screens!) without getting tripped up over business logic and plumbing.” Brad Frost

We use [StorybookJS](https://storybook.js.org/) to showcase our internal design system and tests our components in isolation: https://dotcms.github.io/core-web/design/

## Internal Libraries

### [dotcms](https://github.com/dotCMS/core-web/tree/master/projects/dotcms)

A collection of Angular Services to connect our FE with the DotCMS APIs. [Full docs](https://dotcms.github.io/core-web/docs/dotcms/globals.html)

### [dotcms-models](https://github.com/dotCMS/core-web/tree/master/projects/dotcms-models)

A collection of DotCMS interfaces and types to be reused in all our libraries and main app. [Full docs](https://dotcms.github.io/core-web/docs/dotcms-models/globals.html)

### [dotcms-field-elements](https://github.com/dotCMS/core-web/tree/master/projects/dotcms-field-elements)

A [StencilJS](https://stenciljs.com/) Web Elements library of DotCMS form fields.

### [dot-layout-grid](https://github.com/dotCMS/core-web/tree/master/projects/dot-layout-grid)

For our page Layout Editor, allow us to create rows and columns on a page layout.

### [dot-rules](https://github.com/dotCMS/core-web/tree/master/projects/dot-rules)

Our DotCMS Rules portlet.

### [dotcms-js](https://github.com/dotCMS/core-web/tree/master/projects/dotcms-js) _(deprecated)_

This in a inherit collection of Angular Services.

---

# File structure

## Modules

Normally is a big funcionality, probably a portlet, typically will contain: components, services and maybe even other modules, but regarthelss, make sure that everything that is contain in the folder of the module is used by the module and only by this module, if a service or component or anything is used by thismodule but also by other module in the app the files for those should not live inside this current module but in the global of the app.

### Services, models and utils

They should live in the module if they are shared by more than one component or directive.

```
module-name/
├── components/
│   ├── index.ts
│   ├── component-one
│   │   ├── components/
│   │   ├── services/
│   │   ├── models/
│   │   ├── utils/
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
├── component-name.component.ts
├── component-name.component.scss
├── component-name.component.html
├── component-name.component.spec.ts
├── component-name.module..ts
└── component-name-routing.module..ts
```

## Components and or directives

Services, models and utils are _JUST_ for this component and should be only use it for this component, if any of this needs to be shared between components in the module, it should live at module level

### Services, models and utils

They live in the component if they are use ONLY by this component and/or directive, if they are use in other components/directive they should live in the module level.

```
component-name/
├── components/
│   ├── index.ts
│   ├── components-one
│   │   ├── components.one.service.ts
│   │   └── components.one.service.spec.ts
│   ├── components-two
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
├── component-name.component.ts
├── component-name.component.scss
├── component-name.component.html
└── component-name.component.spec.ts
```

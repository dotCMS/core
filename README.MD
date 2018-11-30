# DotCMS UI
This project was created using [angular-cli](https://cli.angular.io/) it means you can use `ng` with all the commands and params.

## Minimum Requirements
1. NodeJS v8.11.3
2. NPM 6.3.0
3. Angular CLI 6.1.2

## Before start
Build the interal libraries (until we publish it to npm). `dotcms-js` needs to be build first, because is used by other libraries.

```
ng build dotcms-js
ng build dot-layout-grid
ng build dot-rules
```

You should see now the folders:

```
dist-lib/dot-layout-grid
dist-lib/dot-rules
dist-lib/dotcms-js
```

## Run for DotCMS
Watch and compile your files in development mode and copy it into ```dotAdmin``` folder in core.

1. ```npm run build:dev -- --output-path your/path/to/dotadmin``` (ex. ~/dev/dotcms/tomcat8/webapps/ROOT/dotAdmin)
2. Open http://localhost:8080/dotAdmin

## Run tests
`ng test`

## Publish to NPM
**You need the credentials for dotcms npm account**

### Publish a dev verison

```
$ npm run publish:dev
```

This will build a production ready app, update the ```package.json``` version using a timestamp (ex: 5.0.1-1537889821825) and publish that as latest version to npm.

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
------------------------

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
Services, models and utils are *JUST* for this component and should be only use it for this component, if any of this needs to be shared between components in the module, it should live at module level

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

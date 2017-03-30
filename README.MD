# DotCMS Angular 2 UI App

This project was created using as base the "[Angular2 Webpack Starter](https://github.com/AngularClass/angular2-webpack-starter/blob/master/README.md)" project from AngularClass.

## In summary:

* [Webpack 2](http://webpack.github.io/) for building
* [Protractor](http://www.protractortest.org/#/) for our end-to-end story
* [Jasmine](https://jasmine.github.io/) and [Karma](https://karma-runner.github.io/1.0/index.html) for our unit testing.

## Features:

* Best practices in file and application organization for Angular 2.
* Ready to go build system using Webpack for working with TypeScript.
* Ahead of Time (AoT) compile for rapid page loads of your production builds.
* Tree shaking to automatically remove unused code from your production bundle.
* [Webpack DLLs](https://robertknight.github.io/posts/webpack-dll-plugins/) dramatically speed your development builds.
* Testing Angular 2 code with Jasmine and Karma.
* Coverage with Istanbul and Karma
* End-to-end Angular 2 code using Protractor.
* Type manager with @types
* Hot Module Replacement with Webpack and [@angularclass/hmr](https://github.com/angularclass/angular2-hmr) and [@angularclass/hmr-loader](https://github.com/angularclass/angular2-hmr-loader)
* Material Design with [angular/material2](https://github.com/angular/material2)
* Angular 4 support via changing package.json and any future Angular versions

## Quick start
**Make sure you have Node version >= 5.0 and NPM >= 3**

```bash
# clone the repo
git clone https://github.com/dotCMS/core-web.git

# change directory to our repo
cd core-web

# install the repo with npm
npm install

# start the server
npm start

# use Hot Module Replacement
npm run server:dev:hmr
```
Go to [http://0.0.0.0:3000](http://0.0.0.0:3000) or [http://localhost:3000](http://localhost:3000) in your browser

# Table of Contents
* [File Structure](#file-structure)
* [Getting Started](#getting-started)
    * [Dependencies](#dependencies)
    * [Installing](#installing)
    * [Running the app](#running-the-app)
* [Configuration](#configuration)
* [AoT Don'ts](#aot-donts)
* [External Stylesheets](#external-stylesheets)
* [Contributing](#contributing)
* [TypeScript](#typescript)
* [@Types](#types)
* [Frequently asked questions](#frequently-asked-questions)
* [Support, Questions, or Feedback](#support-questions-or-feedback)
* [License](#license)


## File Structure
We use the component approach in our starter. This is the new standard for developing Angular apps and a great way to ensure maintainable code by encapsulation of our behavior logic. A component is basically a self contained app usually in a single file or a folder with each concern as a file: style, template, specs, e2e, and component class. Here's how it looks:
```
angular2-webpack-starter/
 ├──config/                        * our configuration
 │   ├──helpers.js                 * helper functions for our configuration files
 │   ├──spec-bundle.js             * ignore this magic that sets up our angular 2 testing environment
 │   ├──karma.conf.js              * karma config for our unit tests
 │   ├──protractor.conf.js         * protractor config for our end-to-end tests
 │   ├──webpack.dev.js             * our development webpack config
 │   ├──webpack.prod.js            * our production webpack config
 │   └──webpack.test.js            * our testing webpack config
 │
 ├──src/                           * our source files that will be compiled to javascript
 │   ├──app/                       * WebApp: folder
 │   │   ├──api/                   * services, pipes and utilities
 │   │   │
 │   │   ├──e2e/                   * TBR
 │   │   │
 │   │   ├──portlets/              * all angular modules for porlets
 │   │   │
 │   │   ├──views/
 │   │   │  ├──components/
 │   │   │  │  ├──_common/         * common reusable components
 │   │   │  │  └──**/              * all other specifics components
 │   │   │  │
 │   │   │  └──directives/         * app directives
 │   │   │
 │   │   ├──app.component.ts       * a simple version of our App component components
 │   │   ├──app.component.spec.ts  * a simple test of components in app.component.ts
 │   │   ├──app.e2e.ts             * a simple end-to-end test for /
 │   │   ├──app.html               * angular template for app.component
 │   │   ├──app.module.ts          * app module imports
 │   │   ├──app.resolver.ts        * not being use at the time
 │   │   ├──app.rotuing.ts         * app routing
 │   │   ├──app.scss               * styling for app.component
 │   │   ├──app.service.ts         * not being use at the time
 │   │   ├──components.ts          * group all components, pipes and directives
 │   │   ├──environment.ts         * TBD
 │   │   └──index.ts               * entry file
 │   │
 │   ├──assets/                    * static assets are served here
 │   │   └──img                    * images use in the project
 │   │
 │   ├──custom-typings.d.ts        * custom type definition file
 │   ├──index.html                 * Index.html: where we generate our index page
 │   ├──main.browser.ts            * our entry file for our browser environment
 │   └──polyfills.browser.ts       * our polyfills file
 │
 │
 ├──tslint.json                    * typescript lint config
 ├──typedoc.json                   * typescript documentation generator
 ├──tsconfig.json                  * typescript config used outside webpack
 ├──tsconfig.webpack.json          * config that webpack uses for typescript
 ├──package.json                   * what npm uses to manage it's dependencies
 └──webpack.config.js              * webpack main configuration file

```

# Getting Started
## Dependencies
What you need to run this app:
* `node` and `npm` (`brew install node`) or go to [https://nodejs.org/en/download/](https://nodejs.org/en/download/)
* Ensure you're running the latest versions Node `v4.x.x`+ (or `v5.x.x`) and NPM `3.x.x`+

> If you have `nvm` installed, which is highly recommended (`brew install nvm`) you can do a `nvm install --lts && nvm use` in `$` to run with the latest Node LTS. You can also have this `zsh` done for you [automatically](https://github.com/creationix/nvm#calling-nvm-use-automatically-in-a-directory-with-a-nvmrc-file)

Once you have those, you should install these globals with `npm install --global`:
* `webpack` (`npm install --global webpack`)
* `webpack-dev-server` (`npm install --global webpack-dev-server`)
* `karma` (`npm install --global karma-cli`)
* `protractor` (`npm install --global protractor`)
* `typescript` (`npm install --global typescript`)

## Installing
* `npm install webpack-dev-server rimraf webpack -g` to install required global dependencies
* `npm install` to install all dependencies or `yarn`
* `npm run server` to start the dev server in another tab

## Running the app
After you have installed all dependencies you can now run the app. Run `npm run server` to start a local server using `webpack-dev-server` which will watch, build (in-memory), and reload for you. The port will be displayed to you as `http://0.0.0.0:3000` (or if you prefer IPv6, if you're using `express` server, then it's `http://[::1]:3000/`).

### server
```bash
# development
npm run server
# production
npm run build:prod
npm run server:prod
```

## Other commands

### build files
```bash
# development
npm run build:dev
# production (jit)
npm run build:prod
# AoT
npm run build:aot
```

### hot module replacement
```bash
npm run server:dev:hmr
```

### watch and build files
```bash
npm run watch
```

### run unit tests
```bash
npm run test
```

### watch and run our tests
```bash
npm run watch:test
```

### run end-to-end tests
```bash
# update Webdriver (optional, done automatically by postinstall script)
npm run webdriver:update
# this will start a test server and launch Protractor
npm run e2e
```

### continuous integration (run unit tests and e2e tests together)
```bash
# this will test both your JIT and AoT builds
npm run ci
```

### run Protractor's elementExplorer (for end-to-end)
```bash
npm run e2e:live
```

### build Docker
```bash
npm run build:docker
```

# Configuration
Configuration files live in `config/` we are currently using webpack, karma, and protractor for different stages of your application

# AoT Don'ts
The following are some things that will make AoT compile fail.

- Don’t use require statements for your templates or styles, use styleUrls and templateUrls, the angular2-template-loader plugin will change it to require at build time.
- Don’t use default exports.
- Don’t use `form.controls.controlName`, use `form.get(‘controlName’)`
- Don’t use `control.errors?.someError`, use `control.hasError(‘someError’)`
- Don’t use functions in your providers, routes or declarations, export a function and then reference that function name
- @Inputs, @Outputs, View or Content Child(ren), Hostbindings, and any field you use from the template or annotate for Angular should be public

# External Stylesheets
Any stylesheets (Sass or CSS) placed in the `src/styles` directory and imported into your project will automatically be compiled into an external `.css` and embedded in your production builds.

For example to use Bootstrap as an external stylesheet:
1) Create a `styles.scss` file (name doesn't matter) in the `src/styles` directory.
2) `npm install` the version of Boostrap you want.
3) In `styles.scss` add `@import 'bootstrap/scss/bootstrap.scss';`
4) In `src/app/app.module.ts` add underneath the other import statements: `import '../styles/styles.scss';`

# TypeScript
> To take full advantage of TypeScript with autocomplete you would have to install it globally and use an editor with the correct TypeScript plugins.

## Use latest TypeScript compiler
TypeScript 2.1.x includes everything you need. Make sure to upgrade, even if you installed TypeScript previously.

```
npm install --global typescript
```

## Use a TypeScript-aware editor
We have good experience using these editors:

* [Visual Studio Code](https://code.visualstudio.com/)
* [Webstorm 10](https://www.jetbrains.com/webstorm/download/)
* [Atom](https://atom.io/) with [TypeScript plugin](https://atom.io/packages/atom-typescript)
* [Sublime Text](http://www.sublimetext.com/3) with [Typescript-Sublime-Plugin](https://github.com/Microsoft/Typescript-Sublime-plugin#installation)

### Visual Studio Code + Debugger for Chrome
> Install [Debugger for Chrome](https://marketplace.visualstudio.com/items?itemName=msjsdiag.debugger-for-chrome) and see docs for instructions to launch Chrome

The included `.vscode` automatically connects to the webpack development server on port `3000`.

# Types
> When you include a module that doesn't include Type Definitions inside of the module you can include external Type Definitions with @types

i.e, to have youtube api support, run this command in terminal:
```shell
npm i @types/youtube @types/gapi @types/gapi.youtube
```
In some cases where your code editor doesn't support Typescript 2 yet or these types weren't listed in ```tsconfig.json```, add these to **"src/custom-typings.d.ts"** to make peace with the compile check:
```es6
import '@types/gapi.youtube';
import '@types/gapi';
import '@types/youtube';
```

## Custom Type Definitions
When including 3rd party modules you also need to include the type definition for the module
if they don't provide one within the module. You can try to install it with @types

```
npm install @types/node
npm install @types/lodash
```

If you can't find the type definition in the registry we can make an ambient definition in
this file for now. For example

```typescript
declare module "my-module" {
  export function doesSomething(value: string): string;
}
```


If you're prototyping and you will fix the types later you can also declare it as type any

```typescript
declare var assert: any;
declare var _: any;
declare var $: any;
```

If you're importing a module that uses Node.js modules which are CommonJS you need to import as

```typescript
import * as _ from 'lodash';
```

# dotCMS

dotCMS is a scalable, java based, open source content management system (CMS) that has been designed to manage and deliver personalized, permission based content experiences across multiple channels.  dotCMS can serve as the plaform for sites, mobile apps, mini-sites, portals, intranets or as a headless CMS (content is consumed via RESTful APIs).  dotCMS is used everywhere, from running small sites to powering multi-node installations for governemnts, Fortune 100 companies, Universities and Global Brands. A dotCMS environment can scale to support hundreds of editors managing thousands of sites with millions of content objects.  

dotCMS is available for download or as a cloud based product.


- Download dotCMS: [https://dotcms.com/download](https://dotcms.com/download)
- dotCMS Enterprise Cloud: [https://dotcms.com/cms-platform/cloud-cms/](https://dotcms.com/cms-platform/cloud-cms/)

## Licensing
dotCMS comes in multiple editions and as such is dual licensed.  The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization and deployment for use within organizations of all stripes.  dotCMS Enterprise Editions (EE) adds a number of enterprise features and is available via a supported, indemnified commercial license from dotCMS.  For the differences between the editions, see [the feature page](http://dotcms.com/cms-platform/features).

## Contributing

GitHub pull requests are the preferred method to contribute code to dotCMS.  Before any pull requests can be accepted, an automated tool will ask you to agree to the [dotCMS Contributor's Agreement](https://gist.github.com/wezell/85ef45298c48494b90d92755b583acb3).

## Requirements

dotCMS requires a Java 8 Runtime Environment (JRE) or a Java 7 Development Kit (JDK). For a complete list of requirements, see [this page](http://dotcms.com/docs/latest/dotcms-technology-requirements).

## Getting Help

| Source        | Location                                               |
| ------------- |--------------------------------------------------------|
| Installation | [Installation](https://dotcms.com/docs/latest/quick-start-guide)                  |
| Documentation | [Documentation](http://dotcms.com/docs/)                  |
| Videos | [Helpful Videos](http://dotcms.com/videos/)                  |
| Code Examples | [Codeshare](http://dotcms.com/codeshare)                  |
| Forums/Listserv        | [via Google Groups](https://groups.google.com/forum/#!forum/dotCMS) |
| Twitter       | @dotCMS                         |
| Main Site | [dotCMS.com](http://dotcms.com/)                  |
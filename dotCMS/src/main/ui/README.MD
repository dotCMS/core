# Core-Web
DotCMS Core-Web is a Typescript based Angular 2 application. The project is divided into two primary layers: an API 
that provides convenience methods for accessing the DotCMS Core ReST API, and views. 

As of this writing the view layer uses [Semantic UI](http://semantic-ui.com/) to provide widgets and input components,
and Angular 2 alpha 44 for framework and scaffolding (templating, dependency injection, etc).   

## Running this project:

### Requirements
  * [NodeJS](https://nodejs.org/en/download/) (min. v4.4.5)
  * [npm](https://nodejs.org/en/download/) (min. v3.9.6)
  * [TypeScript](https://www.typescriptlang.org/#download-links) (min. v2.0.0)
  * [Typings](https://www.npmjs.com/package/typings) (min. v1.4.0)

### Before run it for the first time running:
  * Run ```npm install```
  * Run ```npm run typings install```

### DotCMS instance
To run this project locally you need a DotCMS instance running in ```http://localhost:8080```.

Also edit file ```plugins/com.dotcms.config/conf/dotmarketing-config-ext.properties``` and add the following line: 

```java
dotcms.websocket.baseurl=localhost:8080
```

After that your start your DotCMS instance normally.

### Run `dev` mode:
Make sure you have your [DotCMS instance](#dotcms-instance) running and...
  * Run ```npm rum dev```

This will compile scss and ts files, copy dependecies and start the server in ```http://localhost:9000``` from the ```/build/``` folder. **Give it a couple of minutes** while the whole procces is finish, it will open the browser before it's done. 

This will also be on ```watch``` mode, all the changes in ```scss```, ```html``` and ```ts``` will update automatically, you'll only need to refresh the page to see your changes.

### Run `dev` mode with integration test in watch mode

To run for development purposes you should execute the `npm run itest` target. This will open your browser to the integration tests, and will start a server on http://localhost:9000 that by default will proxy all ReST requests to http://localhost:8080, which is where you should have an up-to-date instance of dotCMS running.

The `itest` target will also maintain a watch task that will compile your files when changes are detected. You will need
to refresh the test page manually to re-run the integration tests.

## Deploy to artifactory

  * Edit ```deploy-config.local.js``` file with your artifactory credentials.
  * Run ```npm run publish-snapshot```

## The Build System

  * Gulp with NPM script targets for convenience.
  * Jasmine + Karma
    
## API Layer

  * WhatWG for HTTP requests. 
  * Angular2 for DI, EventEmitter
  * Reactive, using [ReactiveX/RxJS](https://github.com/ReactiveX/RxJS), which is included with Angular2. Currently Beta. 

## View Layer
  
  * Angular2: 
    * Templating, WebComponents, DI
  * Angular Material Styles: 
    * For the convenient Flex Layout styles. See [https://material.angularjs.org/HEAD/layout/container]()
  * Semantic-UI:
    * Input elements and styling

### Components
 
#### 'Widget' Components versus 'Feature' Components 

Widgets are bits of frequently reused functionality, such as input components, layout helpers/templates, etc. Feature
components are blocks of functionality. A Card element is a Widget Component. A Person-Card would be a Feature Component.
 
Verbiage subject to change. 

#### Widgets - Input elements and layout templates

##### List of Widgets (links to demo pages)
  * Semantic UI Wrappers [http://semantic-ui.com/]() 
    * [Input Toggle](http://localhost:9000/build/view/components/input/toggle.html)
    * [Input Dropdown](http://localhost:9000/build/view/components/semantic/modules/dropdown.html)
  
  For all demos, make sure to open the browser's console in order to view log messages.

#### Feature Components
  * [Rules Engine](http://localhost:9000/index-dev.html)

## Developing
 
### IDE Support
 
This project contains the IntelliJ/WebStorm project files which JetBrains recommends be shared among developers. 
Rather than using 'create new project', simply open the project with IntelliJ >= 14.1.2 or WebStorm >= 10.0.0.3.

For the most part, these files only change when there are legitimate updates to the project. Sometimes, particularly 
as version updates occur, there are additions to some of the files. 

### Clean often

If you update and something breaks, try updating the project dependencies before anything else. Especially if 
`{projectRoot}/package.json` has been modified by the update. If that doesn't work and it makes sense (or you don't 
know what else to try) you might consider deleting your `{projectRoot}/node_modules` directory and rerunning `npm install`.

### Patterns and Practices

#### Top Down

A child should never know about its parent component. The only exception to this is for Model objects. A child model may 
need to maintain a reference to its parent model type, such as for an 'is owned by' relationship. However, the related 
child Service should not be aware of the parent Service. 
 
Similarly, in the view layer, a component that represents the child should avoid directly modifying any service 
related to the parent, or modifying the parent model object.  Use Observers and EventEmitters to communicate changes
that parent components should be aware of.

A good way to think of this style of development is to consider a bunch of newborn puppies. They know exactly nothing about 
the world around them. The are utterly unaware even of the concept of 'mother'. But when they cry (emit an event), things happen.
Warmth, food, cleaning.
 
View components and API Services are puppies. They are blind and naive. They know nothing about the world around
them, only the small bit of the world under their control. If they need to change something that is not encompassed 
within that world then their only hope is to emit an event and hope something larger than them knows what to do with it.

#### Design for off-line first
Each View component should have a demo page that uses fake/mocked models to demonstrate functionality. This forces a 
development model that allows testing of view components without the need of a backend server, or even a 
mock persistence layer. 
 
### Troubleshooting
When all else fails, try running `gulp clean` and deleting the node_modules directory, and then re-run `npm install`. 

### Some useful links

#### Angular2
  * [Angular](https://angular.io/)
  * [Github Page](https://github.com/angular/angular)
  * [Is angular 2 ready?](http://splintercode.github.io/is-angular-2-ready/)
  * [Gitter Page](https://gitter.im/angular/angular)
  * [AngularU - June 2015 conference](https://angularu.com/ng/videos)
  * [Requisite John Lindquist intro video](https://egghead.io/lessons/angularjs-angular-2-template-syntax)
  * [Angular 2 + Flux](http://victorsavkin.com/post/99998937651/building-angular-apps-using-flux-architecture)
  * [Some background on Functional programming with Ng2](http://victorsavkin.com/post/108837493941/better-support-for-functional-programming-in)
  * [Pascal Precht on dependency injection](https://www.youtube.com/watch?v=8c-qv9TisVE&list=PL9w_03cWjMXOkPgcoOnLaPJBoa2GJWxvD&index=9)

#### Layout
  * [Flex layouts by Angular Material CSS](https://material.angularjs.org/HEAD/layout)
  
#### General
  * [Jasmine - Unit testing framework](http://jasmine.github.io/2.2/introduction.html)
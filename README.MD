# Core-Web

## Demo

Check out the demo at [http://dotcms.github.io/core-web/](http://dotcms.github.io/core-web/). 

If you have an up to date instance of the [Rules Engine branch](https://github.com/dotCMS/core/tree/issue-7391-rules-engine) 
of dotCMS Core running locally you can point to it by setting the Base URL value at the top of the page. 
Otherwise the demo will default to using LocalStorage for persistence.

## About

Note: JSPM/SystemJS/TypeScript1.5 are in a extreme flux at the moment, as efforts being made to align with the 
relevant standards which are all approaching their final form. As part of this, packaging of TypeScript via JSPM is not
yet working, so we're pre-compiling the Typescript into commonjs using the npm:typescript compiler.

## To Run:

  * Checkout this repo
  * Update Node to at least v0.12.x and NPM to 2.14.x
  * Install global NPM hooks
  * Run NPM Install
  * Run JSPM Install
  * Run a server
  
```Shell
git clone git@github.com:dotCMS/core-web.git
npm -g install typescript@^1.5.3
npm install
jspm install
gulp serve --open=true
```

You can also run karma tests in the background as you develop:

```
./node_modules/karma/bin/karma start
```


#### Component Demo Pages 

The Rules Engine currently owns the front page(s), but it will eventually be delegated back to plain-old-component status. For the time being, you can find the Rules Engine app at `/index-dev.html` (unbundled, slow). Once you've built the project using `gulp bundle-dist', `/index.html` will present the JSPM-packaged version of the app, which is much faster to load than the development version.   
 
Other components have their own demonstration pages, which can be found at `http://localhost:9000/build/view/components/{component-path ...}/`, where 'component path' matches the project file structure.

For all demos, make sure to open the browsers console in order to view log messages.


## To Develop
 
This project contains the IntelliJ project files which JetBrains recommends be shared among developers. Rather than using 'create new project', simply open
 the project with IntelliJ >= 14.1.2 or WebStorm >= 10.0.0.3.

For the most part, these files only change when there are legitimate updates to the project. However, there is a bug in the handling of 
'JavaScript -> Libraries' that requires changes (the addition of new libraries) made by one dev to be made manually by other devs.

For faster load times in the browser (at the cost of needing to remember not to commit the changes this makes to config.js!), 
run the `gulp bundle-deps` task. It uses the JSPM API to bundle ('pre-compile and concatenate') a handful of the larger dependencies into a single script file, drastically reducing the number of requests being made. Run 'gulp unbundle' or just revert the config.js file before committing. 

### Patterns and Practices

Component first development! See request-header-conditionlet.ts and the associated demo.ts for the current high bar in best practices within this project.  
 
### Troubleshooting

When all else fails, try running `gulp clean` and deleting the jspm_packages and node_modules directories, and then re-run `npm install` and `jspm install`. 

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

  
#### JSPM
  * [Javascript Package Manager - Home Page](http://jspm.io/)
  * [Github page](https://github.com/jspm/jspm-cli/)
  * [About bundling](https://github.com/jspm/jspm-cli/blob/master/docs/production-workflows.md)
  * [Gitter Page- irc,kinda](https://gitter.im/jspm/jspm)
  
#### Layout
  * [Flex layouts by Angular Material CSS](https://material.angularjs.org/HEAD/layout)
  
#### General
  * [Jasmine - Unit testing framework](http://jasmine.github.io/2.2/introduction.html)

  


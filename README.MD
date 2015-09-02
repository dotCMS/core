# Core-Web

## Demo

Check out the demo at [http://dotcms.github.io/core-web/](http://dotcms.github.io/core-web/). 

(Note: JSPM/SystemJS/TypeScript1.5 are in a extreme flux at the moment, as efforts being made to align with the 
relevant standards which are all approaching their final form. As part of this, packaging of TypeScript via JSPM is not
yet working, and the build hasn't been updated to pre-compile the typescript yet. TLDR; the demo doesn't work at the 
moment, you'll need to run the dev version. See 'To Run' or 'To Develop', below.)

If you have an up to date instance of the [Rules Engine branch](https://github.com/dotCMS/core/tree/issue-7391-rules-engine) 
of dotCMS Core  running locally you can point to it by setting the Base URL value at the top of the page. 
Otherwise the demo will default to using LocalStorage for persistence.


## To Run:

  * Checkout this repo
  * Run NPM Install
  * Run JSPM Install
  * Run a server
  
```Shell
git clone git@github.com:dotCMS/core-web.git
npm install
jspm install
gulp serve --open=true
```

You can also run karma tests in the background as you develop:

```
./node_modules/karma/bin/karma start
```

Navigate to one the index file of your choice; either index-dev.html (unbundled, slow) or index.html (bundled via JSPM, fast-ish). Make sure to open the browsers console
in order to view log messages.

## To Develop

This project contains the IntelliJ project files which JetBrains recommends be shared among developers. Rather than using 'create new project', simply open
 the project with IntelliJ >= 14.1.2 or WebStorm >= 10.0.0.3.

For the most part, these files only change when there are legitimate updates to the project. However, there is a bug in the handling of 
'JavaScript -> Libraries' that requires changes (the addition of new libraries) made by one dev to be made manually by other devs.

For faster load times in the browser (at the cost of needing to remember not to commit the changes this makes to config.js!), 
run the `gulp bundle-deps` task. It uses the JSPM API to bundle ('pre-compile and concatenate') a handful of the larger dependencies into a single script file, drastically reducing the number of requests being made. Run 'gulp unbundle' or just revert the config.js file before committing. 


 
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
  

  
#### JSPM
  * [Javascript Package Manager - Home Page](http://jspm.io/)
  * [Github page](https://github.com/jspm/jspm-cli/)
  * [About bundling](https://github.com/jspm/jspm-cli/blob/master/docs/production-workflows.md)
  * [Gitter Page- irc,kinda](https://gitter.im/jspm/jspm)
  
  
  
#### General
  * [Jasmine - Unit testing framework](http://jasmine.github.io/2.2/introduction.html)

  


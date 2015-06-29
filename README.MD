# Core-Web

## Demo

Check out the demo at [http://dotcms.github.io/core-web/](http://dotcms.github.io/core-web/). 

If you have an up to date instance of the [Rules Engine branch](https://github.com/dotCMS/core/tree/issue-7391-rules-engine) of dotCMS Core  running locally you can point to it by setting the Base URL value at the top of the page. Otherwise the demo will  (eventually) default to using LocalStorage for persistence.


## To Run:

  * Checkout this repo
  * Run NPM Install
  * Run JSPM Install
  * Run a server
  
```Shell
git clone git@github.com/dotCMS/core-web.git
npm install
jspm install
gulp play --open=true
```

You can also run karma tests in the background as you develop:

```
./node_modules/karma/bin/karma start
```

Navigate to one the index file of your choice; either index-dev.html (unbundled, slow) or index.html (bundled via JSPM, fast). Make sure to open the browsers console
in order to view log messages.

## To Develop

This project contains the IntelliJ project files which JetBrains recommends be shared among developers. Rather than using 'create new project', simply open
 the project with IntelliJ >= 14.1.2 or WebStorm >= 10.0.0.3.

For the most part, these files only change when there are legitimate updates to the project. However, there is a bug in the handling of 
'JavaScript -> Libraries' that requires changes (the addition of new libraries) made by one dev to be made manually by other devs.

For faster load times in the browser (at the cost of needing to remember not to commit the changes this makes to config.js!), 
run the './bundle-dependencies.sh' script. It pre-compiles a handful of the larger dependencies. Run 'jspm unbundle' before committing, or move config.js 
to a change list you won't commit. ALWAYS run 'jspm unbundle' before a pull, or you'll run the risk of annoying everyone with a painful accidental merge-commit. 
 
### Some useful links

#### Angular2
  * [Angular](https://angular.io/)
  * [Requisite John Lindquist intro video](https://egghead.io/lessons/angularjs-angular-2-template-syntax)
  * [Forms in Angular2](http://angularjs.blogspot.com/2015/03/forms-in-angular-2.html)
  * [Angular 2 + Flux](http://victorsavkin.com/post/99998937651/building-angular-apps-using-flux-architecture)
  * [Some background on Functional programming with Ng2](http://victorsavkin.com/post/108837493941/better-support-for-functional-programming-in)
  
  
#### General
  * [Jasmine - Unit testing framework](http://jasmine.github.io/2.2/introduction.html)

  


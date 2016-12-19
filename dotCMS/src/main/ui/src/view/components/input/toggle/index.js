import 'babel/polyfill'
import 'es6-shim'
//import "bootstrap/css/bootstrap.css!";
import "thirdparty/semantic/dist/semantic.min.css!"


import 'zone.js'
import 'reflect-metadata';


import * as App from './demo';
App.main().then(function () {
  console.log("Loaded Demo.")
});

console.log("Loading Demo.")

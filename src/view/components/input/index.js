import 'zone.js'
import 'reflect-metadata';
import 'es6-shim';

import * as App from './demo';

App.main().then(function () {
  console.log("Loaded Demo.")
});

console.log("Loading Demo.")

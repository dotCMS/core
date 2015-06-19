import 'zone.js'
import * as XDebug from 'debug'

import 'reflect-metadata';
import * as Core from './src/coreweb/index.js';
import * as RulesEngine from './src/rules-engine-ng2/index.js';
import * as logConfig from './src/rules-engine-ng2/log-config.js';
import {mocks} from './src/rules-engine/datamocks/rule.mocks.js';
export let dot = {
  XDebug: XDebug.default,
  Core,
  RulesEngine
}

var root = dot

root.XDebug.disable() // Clear LocalStorage so changes to log-config files 'take'
root.XDebug.enable("*, .*") // String of comma separated regex. Not glob patterns.
mocks.init().then(function () {
  root.RulesEngine.main().then(function () {
    console.log("Loaded rules-engine component.")
  });
});

console.log("Loading rules-engine component.")

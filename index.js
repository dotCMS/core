import 'zone.js'
import * as XDebug from 'debug'

import 'reflect-metadata';
import * as Core from './src/coreweb/index.js';
import * as RuleEngine from 'src/rule-engine-view/index.js';
import * as logConfig from 'src/rule-engine-view/log-config.js';
import {mocks} from 'src/rule-engine/datamocks/rule.mocks.js';
export let dot = {
  XDebug: XDebug.default,
  Core,
  RuleEngine
}

var root = dot

root.XDebug.disable() // Clear LocalStorage so changes to log-config files 'take'
root.XDebug.enable("*, .*") // String of comma separated regex. Not glob patterns.
mocks.init().then(function () {
  root.RuleEngine.main().then(function () {
    console.log("Loaded rule-engine component.")
  });
});

console.log("Loading rule-engine component.")

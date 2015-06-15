import 'zone.js'
import * as XDebug from 'debug'

import * as Core from 'src/dc/index.es6';
import * as RulesEngine from 'src/rules-engine-ng2/index.es6';
import * as logConfig from 'src/rules-engine-ng2/log-config.es6';
import {mocks} from 'src/rules-engine/datamocks/rule.mocks.es6';
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

RulesEngine.main();



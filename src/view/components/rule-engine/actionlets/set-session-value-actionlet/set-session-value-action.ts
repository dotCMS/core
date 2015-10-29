/// <reference path="../../../../../../jspm_packages/npm/angular2@2.0.0-alpha.44/angular2.d.ts" />


/**
 * Set a value on to the active session, using the supplied key.
 *
 *
 *
 * ## POSIX Utility Argument Syntax (http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap12.html)
 * ```
 * cw-set-session-value-action [-k session_key] [value]
 *   -k          The key under which the specified value will be made available on the Session.
 *   value       The value which will be set on the session. Currently only allows static string values. Dynamic lookup TBD.
 * ```
 *
 * ## UI Layout
 *
 * Conceptually the action type is made up of two fields:
 * ```
 * <div>
 *   <input name="session_key" required="true"/>
 *   <input name="value" required="true" />
 * </div>
 * ```
 * ### Fields
 *
 * #### `session_key`
 * The `session_key` is a simple text input field. The value specified must be a valid Session Key as specified by the Java Servlet specification.
 *
 * #### `value`
 * A static string value. May be a number, blank, or any value representable by text.
 *
 *
 * ------------------------ Discussion ------------------------
 *
 * --------------------------
 */

import {Component, View, Attribute, EventEmitter, NgFor, NgIf} from 'angular2/angular2';
import {ActionModel} from "api/rule-engine/Action";

@Component({
  selector: 'cw-set-session-value-action',
  properties: [
    "sessionKey", "sessionValue", "action"
  ],
  events: [
    "configChange"
  ]
})
@View({
  directives: [NgFor],
  template: `<div flex="grow" layout="row" layout-align="space-around-center">
  <input flex
         type="text"
         class="cw-action-value cw-input"
         [value]="action.getParameter('sessionKey')"
         placeholder="Enter a session key"
         (change)="updateParamValue('sessionKey', $event)"/>
  <input flex
         type="text"
         class="cw-action-value cw-input"
         [value]="action.getParameter('sessionValue')"
         placeholder="Enter a value"
         (change)="updateParamValue('sessionValue', $event)"/>
</div>
  `
})
export class SetSessionValueAction {

  _action:ActionModel;

  paramKeys:Array<String>

  configChange:EventEmitter;

  constructor(@Attribute('sessionKey') sessionKey:string = '',
              @Attribute('sessionValue') sessionValue:string = '') {
    this._action = new ActionModel()
    this.paramKeys = ["sessionKey", "sessionValue"]
    this.configChange = new EventEmitter();
  }

  set action(action:ActionModel){
    this._action = action
  }

  get action():ActionModel {
    return this._action
  }

  toParamValueObject(){
    let params = {}
    this.paramKeys.forEach((key)=>{
      params[key] = this._action.getParameter(key)
    })
    return params
  }

  updateParamValue(key:string, event:Event) {
    let value = event.target['value']
    this.configChange.next({type: 'actionParameterChanged', target: this, params: this.toParamValueObject()})
  }
}

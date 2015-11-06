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
import {ActionModel} from '../../../../../api/rule-engine/Action';
import {InputText, InputTextModel} from '../../../semantic/elements/input-text/input-text';

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
  directives: [NgFor, InputText],
  template: `<div flex="grow" layout="row" layout-align="space-around-center">
  <cw-input-text
      flex
      class="cw-input"
      (change)="updateParamValue('sessionKey', $event)"
      [model]="setSessionKeyInputTextModel">
  </cw-input-text>
  <cw-input-text
      flex
      class="cw-input"
      (change)="updateParamValue('sessionValue', $event)"
      [model]="setSessionValueInputTextModel">
  </cw-input-text>
</div>
  `
})
export class SetSessionValueAction {

  paramKeys:Array<String>
  params:{[key:string]: string}

  configChange:EventEmitter;

  private setSessionKeyInputTextModel:InputTextModel
  private setSessionValueInputTextModel:InputTextModel

  constructor(@Attribute('sessionKey') sessionKey:string = '',
              @Attribute('sessionValue') sessionValue:string = '') {
    this.paramKeys = ["sessionKey", "sessionValue"]
    this.params = {
      "sessionKey": sessionKey,
      "sessionValue": sessionValue
    }
    this.configChange = new EventEmitter();
    this.setSessionKeyInputTextModel = new InputTextModel();
    this.setSessionValueInputTextModel = new InputTextModel();
    this.setSessionKeyInputTextModel.placeholder = "Enter a session key"
    this.setSessionValueInputTextModel.placeholder = "Enter a value"
  }

  set action(action:ActionModel){
    this.params = {}
    this.paramKeys.forEach((key:string)=>{
      this.params[key] = action.getParameter(key)
    })

    this.setSessionKeyInputTextModel.value = action.getParameter('sessionKey')
    this.setSessionValueInputTextModel.value = action.getParameter('sessionValue')
  }

  updateParamValue(key:string, event:Event) {
    let value = event.target['value']
    this.params[key] = value
    this.configChange.next({type: 'actionParameterChanged', target: this, params: this.params})

    if(key == 'sessionKey') this.setSessionKeyInputTextModel.value = value
    if(key == 'sessionValue') this.setSessionValueInputTextModel.value = value
  }
}

/// <reference path="../../../../../../typings/angular2/angular2.d.ts" />


/**
 * Set a value on to the active session, using the supplied key.
 *
 * Express a condition based on two fields: a Header, and a Header Comparison
 * @see https://en.wikipedia.org/wiki/List_of_HTTP_header_fields#Request_fields
 * @see https://tools.ietf.org/html/rfc7231#section-5
 * @see http://www.iana.org/assignments/message-headers/message-headers.xml#perm-headers
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
import {SetSessionValueActionModel} from 'api/rule-engine/rule-action'

@Component({
  selector: 'cw-set-session-value-action',
  properties: [
    "sessionKey", "sessionValue"
  ],
  events: [
    "change"
  ]
})
@View({
  directives: [NgFor],
  template: `
  <div class="col-sm-3">
    <input type="text" class="form-control action-value" [value]="value.sessionKey"
           placeholder="Enter a session key" (change)="updateSessionKey($event)"/>
  </div>
  <div class="col-sm-3">
    <input type="text" class="form-control action-value" [value]="value.sessionValue" placeholder="Enter a value"
           (change)="updateSessionValue($event)"/>
  </div>
  `
})
export class SetSessionValueAction {

  value:SetSessionValueActionModel;

  change:EventEmitter;

  constructor(@Attribute('sessionKey') sessionKey:string = '',
              @Attribute('sessionValue') sessionValue:string = '') {
    this.value = new SetSessionValueActionModel()
    this.value.sessionKey = sessionKey
    this.value.sessionValue = sessionValue
    this.change = new EventEmitter();
  }

  _modifyEventForForwarding(event:Event, field, oldState:SetSessionValueActionModel):Event {
    Object.assign(event, {ngTarget: this, was: oldState, value: this.value, valueField: field})
    return event
  }

  set sessionKey(value:string) {
    this.value.sessionKey = value || ''
  }

  set sessionValue(value:string) {
    this.value.sessionValue = value || ''
  }

  updateSessionKey(event:Event) {
    let value = event.target['value']
    let e = this._modifyEventForForwarding(event, 'sessionKey', this.value.clone())
    this.value.sessionKey = value
    this.change.next(e)
  }

  updateSessionValue(event:Event) {
    let value = event.target['value']
    let e = this._modifyEventForForwarding(event, 'sessionValue', this.value.clone())
    this.value.sessionValue = value;
    this.change.next(e)
  }

}

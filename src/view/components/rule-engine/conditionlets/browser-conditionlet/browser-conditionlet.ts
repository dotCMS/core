/// <reference path="../../../../../thirdparty/angular2/bundles/typings/angular2/angular2.d.ts" />

import {Directive, Attribute, Host, SkipSelf, EventEmitter, NgFor, NgIf, Component, View} from 'angular2/angular2';


/**
 * ------------------------ Discussion ------------------------
 * @todo ggranum: Delete this file, probably. Or define what this conditionlet is meant to do more clearly.
 *   Specific issues:
 *     1) what do we really want the user to select? Firefox vs Chrome vs Edge? Mobile versus Desktop versus tablet?
 *   any or all of these things can be obtain through the UA header. There is certainly a good argument to be made for
 *   making this easier for a limited subset of browsers. Which brings us to
 *     2) User defined values would be just about impossible to implement for this conditionlet without basically
 *   recreating the RequestHeaderConditionlet.
 *
 *
 *   Potential solution:
 *     Make this a simple "is/is not" and a select list that contains only the most 5 to 10 most common browser names.
 *       - "Browser name [is, is not] [ Chrome, Firefox, MSIE, Opera, Safari, Unrecognized ]"
 *     Create a second conditionlet for browser type
 *       - "Browser type [is, is not] [ Desktop, Tablet, Phone, Crawler, Unrecognized]"
 *
 * --------------------------
 *
 * Express a condition based on the simple name of the web browser, as found in the User-Agent request header.
 * @see: http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.43
 * @see http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.8
 *
 * A list of UA strings can be found at http://techpatterns.com/forums/about304.html
 *
 * Conditionlet Definition:
 *   allowMultipleSelections: true,
 *   allowUserDefinedValues: true
 *
 * Pre-populated values:
 *  [ ]
 *
 * "UsersBrowserConditionlet":{
 *   "id":"UsersBrowserConditionlet",
 *   "comparisons":[
 *     {
 *       "id":"is",
 *     },
 *     {
 *       "id":"startsWith",
 *     },
 *     {
 *       "id":"endsWith",
 *     },
 *     {
 *       "id":"contains",
 *     },
 *     {
 *       "id":"regex",
 *     }
 *   ]
 * }
 */

@Component({
  selector: 'cw-browser-conditionlet'
})
@View({
  directives: [NgFor],
  template: `
    <div class="col-sm-5">
      <select class="form-control clause-selector" [value]="conditionletDir.condition.comparison" (change)="setComparison($event)">
        <option value="{{x.id}}" *ng-for="var x of conditionletDir.conditionlet.comparisons">{{x.label}}</option>
      </select>
    </div>
    <div class="col-sm-2">
      <h4 class="separator"></h4>
    </div>
    <div class="col-sm-5">
      <input type="text" class="form-control condition-value" [value]="conditionletDir.value" (input)="setValue($event)"/>
    </div>
  `
})
export class BrowserConditionlet {

  constructor( @Attribute('id') id:string) {

  }

}
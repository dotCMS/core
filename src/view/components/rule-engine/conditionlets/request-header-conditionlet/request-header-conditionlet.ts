/// <reference path="../../../../../../jspm_packages/npm/angular2@2.0.0-alpha.44/angular2.d.ts" />


/**
 * Check for the presence and value of a header on the user's request.
 *
 * Express a condition based on two fields: a Header, and a Header Comparison
 * @see https://en.wikipedia.org/wiki/List_of_HTTP_header_fields#Request_fields
 * @see https://tools.ietf.org/html/rfc7231#section-5
 * @see http://www.iana.org/assignments/message-headers/message-headers.xml#perm-headers
 *
 *
 * ## POSIX Utility Argument Syntax (http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap12.html)
 * ```
 * cw-request-header-conditionlet [-n][-h header_key] [-c comparator] [comparison_values...]
 *   -n                      Negate the match.
 *   -h                      The exact key to search for. Case sensitive.
 *   -e                      Exists. Only verify that the header exists on the Request. The value may be empty or nonsensical.
 *   -c                      The comparator id. One of [ exists, is, startsWith, endsWith, contains, regex]
 *   comparison_values       One or more values compare against the header value.
 * ```
 *
 * ## UI Layout
 *
 * Conceptually the conditionlet is made up of three fields:
 * ```
 * <div>
 *   <input-select-one name="header_key" required="true"/>
 *   <select-one name="comparator" />
 *   <input-select-many name="comparison_values" required="true" />
 * </div>
 * ```
 * ### Fields
 *
 * #### `header_key`
 * The `header_key` is a `select`, pre-populated with common Request header keys. See the Wikipedia.org or iana.org
 * links for more details. There is also an associated text input box that allows custom header keys to be specified.
 *
 * #### `comparator`
 * A standard `select` element containing the allowed comparator ids. One of [ exists, is, startsWith, endsWith, contains, regex].
 * When the selected value is 'exists' the `comparison_values` UI field will be hidden and its model value cleared.
 *
 * #### `comparison_values`
 * Multiple comparison values may be set. Each value shall be specified as per rfc7231#section-5. The UI will
 * **temporarily** add manually keyed inputs into the list of `select` options, negating the need to concern the
 * end user with character escape syntax etc.
 *
 *
 * ------------------------ Discussion ------------------------
 *
 * --------------------------
 */

import {Component, View, Attribute, EventEmitter, NgFor, NgIf} from 'angular2/angular2';
import {Dropdown, DropdownModel, DropdownOption} from 'view/components/semantic/modules/dropdown/dropdown'
import {Input, InputModel} from "view/components/semantic/elements/input/input";

/**
 * @todo: Consider populating these from the server
 * @type {string[]}
 */
let commonRequestHeaders = [
  "Accept",
  "Accept-Charset",
  "Accept-Datetime",
  "Accept-Encoding",
  "Accept-Language",
  "Authorization",
  "Cache-Control",
  "Connection",
  "Content-Length",
  "Content-MD5",
  "Content-Type",
  "Cookie",
  "Date",
  "Expect",
  "From",
  "Host",
  "If-Match",
  "If-Modified-Since",
  "If-None-Match",
  "If-Range",
  "If-Unmodified-Since",
  "Max-Forwards",
  "Origin",
  "Pragma",
  "Proxy-Authorization",
  "Range",
  "Referer",
  "TE",
  "Upgrade",
  "User-Agent",
  "Via",
  "Warning"
]


export class RequestHeaderConditionletModel {
  parameterKeys:Array<string> = ['headerKeyValue', 'compareTo']
  headerKeyValue:string
  comparatorValue:string
  compareTo:string

  constructor(headerKeyValue:string = null, comparatorValue:string = null, compareTo:string = '') {
    this.headerKeyValue = headerKeyValue || commonRequestHeaders[0]
    this.comparatorValue = comparatorValue
    this.compareTo = compareTo
  }

  clone():RequestHeaderConditionletModel {
    return new RequestHeaderConditionletModel(this.headerKeyValue, this.comparatorValue, this.compareTo)
  }
}

@Component({
  selector: 'cw-request-header-conditionlet',
  properties: [
    "headerKeyValue", "comparatorValue", "parameterValues"
  ],
  events: [
    "change"
  ]
})
@View({
  directives: [NgFor, Dropdown, Input],
  template: `<div flex layout="row" layout-align="start-center" class="cw-condition-component-body">
  <cw-input-dropdown flex="40"  class="cw-input" [model]="headerKeyDropdown" (change)="handleHeaderKeyChange($event)"></cw-input-dropdown>
  <cw-input-dropdown flex="initial" class="cw-input cw-comparator-selector" [model]="comparatorDropdown" (change)="handleComparatorChange($event)"></cw-input-dropdown>
  <cw-input flex="30"
      (change)="handleCompareToChange($event)"
      [model]="requestHeaderInputModel">
    </cw-input>
</div>`
})
export class RequestHeaderConditionlet {
  // @todo populate the comparisons options from the server.
  comparisonOptions:Array<DropdownOption> = [
    new DropdownOption("exists", "exists", "Exists"),
    new DropdownOption("is", "is", "Is"),
    new DropdownOption("is not", "is not", "Is Not"),
    new DropdownOption("startsWith", "startsWith", "Starts With"),
    new DropdownOption("endsWith", "endsWith", "Ends With"),
    new DropdownOption("contains", "contains", "Contains"),
    new DropdownOption("regex", "regex", "Regex")];

  value:RequestHeaderConditionletModel;

  change:EventEmitter;
  private headerKeyDropdown:DropdownModel
  private comparatorDropdown:DropdownModel

  private requestHeaderInputModel: InputModel

  constructor(@Attribute('header-key-value') headerKeyValue:string,
              @Attribute('comparatorValue') comparatorValue:string,
              @Attribute('parameterValues') parameterValues:Array<string>) {
    this.value = new RequestHeaderConditionletModel(headerKeyValue, comparatorValue)
    this.change = new EventEmitter();
    this.comparatorDropdown = new DropdownModel("comparator", "Comparison", ["is"], this.comparisonOptions)

    let headerKeyOptions = []
    commonRequestHeaders.forEach((name)=> {
      headerKeyOptions.push(new DropdownOption(name, name, name))
    })
    this.headerKeyDropdown = new DropdownModel("headerKey", "Header Key", [], headerKeyOptions)

    this.requestHeaderInputModel = new InputModel()
    this.requestHeaderInputModel.placeholder = "Enter a value"
  }

  set headerKeyValue(value:string) {
    this.value.headerKeyValue = value
    this.headerKeyDropdown.selected = [value]
  }

  set compareTo(value:string) {
    this.value.compareTo = value
    this.requestHeaderInputModel.value = value
  }

  set comparatorValue(value:string) {
    this.value.comparatorValue = value
    this.comparatorDropdown.selected = [value]
  }

  set parameterValues(value:any) {
    this.value.parameterKeys.forEach((paramKey)=> {
      let v = value[paramKey]
      v = v ? v.value : ''
      this[paramKey] = v
    })
  }

  private getEventValue():Array<any>{
    let eventValue = []
    this.value.parameterKeys.forEach((key)=>{
      eventValue.push({key: key, value:this.value[key]})
    })
    return eventValue
  }

  handleComparatorChange(event) {
    let value = event.value
    this.value.comparatorValue = value
    this.change.next({type:'comparisonChange', target:this, value:value})
  }

  handleHeaderKeyChange(event) {
    this.value.headerKeyValue = event.value
    this.change.next({type:'parameterValueChange', target: this, value: this.getEventValue()})
  }

  handleCompareToChange(event:Event) {
    this.value.compareTo = event.target.value
    this.requestHeaderInputModel.value = event.target.value
    this.change.next({type:'parameterValueChange', target:this, value:this.getEventValue()})
  }

}

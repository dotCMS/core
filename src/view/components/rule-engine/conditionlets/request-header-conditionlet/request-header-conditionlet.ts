/// <reference path="../../../../../../typings/angular2/angular2.d.ts" />


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
  parameterKeys: Array<string> = ['headerKeyValue', 'compareTo']
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
    "headerKeyValue", "comparatorValue", "comparisonValues"
  ],
  events: [
    "change"
  ]
})
@View({
  directives: [NgFor],
  template: `
      <div class="col-sm-2">
        <select class="form-control header-key" [value]="value.headerKeyValue" (change)="updateHeaderKey($event)">
          <option [selected]="hkOpt === value.headerKeyValue" value="{{hkOpt}}" *ng-for="var hkOpt of predefinedHeaderKeyOptions">{{hkOpt}}</option>
        </select>
      </div>
      <div class="col-sm-2">
        <select class="form-control comparator" [value]="value.comparatorValue" (change)="updateComparator($event)">
          <option [selected]="cOpt === value.comparatorValue" value="{{cOpt}}" *ng-for="var cOpt of comparisonOptions">{{cOpt}}</option>
        </select>
      </div>
      <div class="col-sm-3">
        <input type="text" class="form-control condition-value" [value]="value.compareTo" placeholder="Enter a value" (change)="updateCompareToValue($event)"/>
      </div>
  `
})
export class RequestHeaderConditionlet {
  // @todo populate the comparisons options from the server.
  comparisonOptions:Array<string> = ["exists", "is", "startsWith", "endsWith", "contains", "regex"];
  predefinedHeaderKeyOptions:Array<string> = commonRequestHeaders;

  value:RequestHeaderConditionletModel;

  change:EventEmitter;

  constructor(@Attribute('header-key-value') headerKeyValue:string,
              @Attribute('comparatorValue') comparatorValue:string,
              @Attribute('comparisonValues') comparisonValues:Array<string>) {
    this.value = new RequestHeaderConditionletModel(headerKeyValue, comparatorValue)
    this.change = new EventEmitter();
  }

  _modifyEventForForwarding(event:Event, field, oldState:RequestHeaderConditionletModel):Event {
    Object.assign(event, {ngTarget: this, was: oldState, value: this.value, valueField: field })
    return event
  }

  set headerKeyValue(value:string){
    this.value.headerKeyValue = value
  }

  set comparatorValue(value:string){
    this.value.comparatorValue = value
  }

  set comparisonValues(value:any) {
    this.value.parameterKeys.forEach((paramKey)=> {
      let v = value[paramKey]
      v = v ? v.value : ''
      this.value[paramKey] = v
    })
  }

  updateHeaderKey(event:Event) {
    let value = event.target['value']
    let e = this._modifyEventForForwarding(event, 'headerKeyValue', this.value.clone())
    this.value.headerKeyValue = value
    this.change.next(e)
  }

  updateComparator(event:Event) {
    let value = event.target['value']
    let e = this._modifyEventForForwarding(event, 'comparatorValue', this.value.clone())
    this.value.comparatorValue = value
    this.change.next(e)
  }

  updateCompareToValue(event:Event) {
    let value = event.target['value']
    let e = this._modifyEventForForwarding(event, 'compareTo', this.value.clone())
    this.value.compareTo = value
    this.change.next(e)
  }

}

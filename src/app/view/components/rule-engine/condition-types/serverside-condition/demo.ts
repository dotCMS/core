import {bootstrap} from '@angular/bootstrap'
import {Attribute, Component, View} from '@angular/core'
import {HTTP_PROVIDERS} from '@angular/http'

import {ServersideCondition} from './serverside-condition';
import {ServerSideTypeModel} from "../../../../../api/rule-engine/ServerSideFieldModel";
import {I18nService} from "../../../../../api/system/locale/I18n";
import {ApiRoot} from "../../../../../api/persistence/ApiRoot";
import {UserModel} from "../../../../../api/auth/UserModel";
import {ConditionModel} from "../../../../../api/rule-engine/Rule";

@Component({
  selector: 'demo',
  directives: [ServersideCondition],
  template: `<div flex layout="row" layout-wrap layout-align="start start" style="height:5em">
  <div flex="100" layout="row" layout-wrap >
    <div flex="100"> Dropdown, Comparisons, Text Input </div>
    <cw-serverside-condition flex="100" [model]="demo.one" [paramDefs]="paramDefs" (change)="demoOneChange($event)"></cw-serverside-condition>
  </div>
</div>
  `
})
class App {
  demo:any;
  paramDefs:any

  constructor(@Attribute('id') id:string) {
    this.paramDefs = MOCK_CONDITIONLET.parameterDefinitions
    this.demo = {
      'one': new ConditionModel({ id: "test2", _type: new ServerSideTypeModel("Demo1Condition", "demo1", MOCK_CONDITIONLET.parameterDefinitions)}),
      'two': {},
      'three': {}
    };
  }

  demoOneChange(model) {
  }
}

export function main() {
  let app = bootstrap(App, [
    ApiRoot,
    I18nService,
    UserModel,
    HTTP_PROVIDERS
  ])
  app.then((appRef) => {
    console.log("Bootstrapped App: ", appRef)
  }).catch((e) => {
    console.log("Error bootstrapping app: ", e)
    throw e;
  });
  return app
}

let MOCK_CONDITIONLET = {
  "id": "UsersBrowserHeaderConditionlet",
  "i18nKey": "api.system.ruleengine.conditionlet.RequestHeader",
  "parameterDefinitions": {
    "browser-header": {
      "key": "browser-header",
      "defaultValue": "Accept",
      "inputType": {
        "id": "dropdown",
        "dataType": {
          "id": "text",
          "minLength": 0,
          "maxLength": 255,
          "defaultValue": ""
        },
        "placeholder": "",
        "options": {
          "Accept": {
            "i18nKey": "Accept",
            "value": "Accept",
            "priority": 1
          },
          "Accept-Charset": {
            "i18nKey": "Accept-Charset",
            "value": "Accept-Charset",
            "priority": 2
          },
          "Accept-Encoding": {
            "i18nKey": "Accept-Encoding",
            "value": "Accept-Encoding",
            "priority": 3
          },
          "Accept-Language": {
            "i18nKey": "Accept-Language",
            "value": "Accept-Language",
            "priority": 4
          },
          "Accept-Datetime": {
            "i18nKey": "Accept-Datetime",
            "value": "Accept-Datetime",
            "priority": 5
          },
          "Authorization": {
            "i18nKey": "Authorization",
            "value": "Authorization",
            "priority": 6
          },
          "Cache-Control": {
            "i18nKey": "Cache-Control",
            "value": "Cache-Control",
            "priority": 7
          },
          "Connection": {
            "i18nKey": "Connection",
            "value": "Connection",
            "priority": 8
          },
          "Cookie": {
            "i18nKey": "Cookie",
            "value": "Cookie",
            "priority": 9
          },
          "Content-Length": {
            "i18nKey": "Content-Length",
            "value": "Content-Length",
            "priority": 10
          },
          "Content-MD5": {
            "i18nKey": "Content-MD5",
            "value": "Content-MD5",
            "priority": 11
          },
          "Content-Type": {
            "i18nKey": "Content-Type",
            "value": "Content-Type",
            "priority": 12
          },
          "Date": {
            "i18nKey": "Date",
            "value": "Date",
            "priority": 13
          },
          "Expect": {
            "i18nKey": "Expect",
            "value": "Expect",
            "priority": 14
          },
          "From": {
            "i18nKey": "From",
            "value": "From",
            "priority": 15
          },
          "Host": {
            "i18nKey": "Host",
            "value": "Host",
            "priority": 16
          },
          "If-Match": {
            "i18nKey": "If-Match",
            "value": "If-Match",
            "priority": 17
          },
          "If-Modified-Since": {
            "i18nKey": "If-Modified-Since",
            "value": "If-Modified-Since",
            "priority": 19
          },
          "If-None-Match": {
            "i18nKey": "If-None-Match",
            "value": "If-None-Match",
            "priority": 19
          },
          "If-Range": {
            "i18nKey": "If-Range",
            "value": "If-Range",
            "priority": 20
          },
          "If-Unmodified-Since": {
            "i18nKey": "If-Unmodified-Since",
            "value": "If-Unmodified-Since",
            "priority": 22
          },
          "Max-Forwards": {
            "i18nKey": "Max-Forwards",
            "value": "Max-Forwards",
            "priority": 22
          },
          "Origin": {
            "i18nKey": "Origin",
            "value": "Origin",
            "priority": 23
          },
          "Pragma": {
            "i18nKey": "Pragma",
            "value": "Pragma",
            "priority": 24
          },
          "Proxy-Authorization": {
            "i18nKey": "Proxy-Authorization",
            "value": "Proxy-Authorization",
            "priority": 26
          },
          "Range": {
            "i18nKey": "Range",
            "value": "Range",
            "priority": 26
          },
          "Referer": {
            "i18nKey": "Referer",
            "value": "Referer",
            "priority": 27
          },
          "TE": {
            "i18nKey": "TE",
            "value": "TE",
            "priority": 28
          },
          "User-Agent": {
            "i18nKey": "User-Agent",
            "value": "User-Agent",
            "priority": 29
          },
          "Upgrade": {
            "i18nKey": "Upgrade",
            "value": "Upgrade",
            "priority": 30
          },
          "Via": {
            "i18nKey": "Via",
            "value": "Via",
            "priority": 31
          },
          "Warning": {
            "i18nKey": "Warning",
            "value": "Warning",
            "priority": 32
          },
          "X-Requested-With": {
            "i18nKey": "X-Requested-With",
            "value": "X-Requested-With",
            "priority": 33
          },
          "DNT": {
            "i18nKey": "DNT",
            "value": "DNT",
            "priority": 34
          },
          "X-Forwarded-For": {
            "i18nKey": "X-Forwarded-For",
            "value": "X-Forwarded-For",
            "priority": 35
          },
          "X-Forwarded-Host": {
            "i18nKey": "X-Forwarded-Host",
            "value": "X-Forwarded-Host",
            "priority": 36
          },
          "Front-End-Https": {
            "i18nKey": "Front-End-Https",
            "value": "Front-End-Https",
            "priority": 37
          },
          "X-Http-Method-Override": {
            "i18nKey": "X-Http-Method-Override",
            "value": "X-Http-Method-Override",
            "priority": 39
          },
          "X-ATT-DeviceId": {
            "i18nKey": "X-ATT-DeviceId",
            "value": "X-ATT-DeviceId",
            "priority": 39
          },
          "X-Wap-Profile": {
            "i18nKey": "X-Wap-Profile",
            "value": "X-Wap-Profile",
            "priority": 40
          },
          "Proxy-Connection": {
            "i18nKey": "Proxy-Connection",
            "value": "Proxy-Connection",
            "priority": 41
          },
          "X-UIDH": {
            "i18nKey": "X-UIDH",
            "value": "X-UIDH",
            "priority": 42
          },
          "X-Csrf-Token": {
            "i18nKey": "X-Csrf-Token",
            "value": "X-Csrf-Token",
            "priority": 43
          }
        },
        "allowAdditions": true,
        "minSelections": 1,
        "maxSelections": 1
      },
      "priority": 1
    },
    "comparison": {
      "key": "comparison",
      "defaultValue": "",
      "inputType": {
        "id": "dropdown",
        "dataType": {
          "id": "text",
          "minLength": 0,
          "maxLength": 255,
          "defaultValue": ""
        },
        "placeholder": "",
        "options": {
          "exists": {
            "i18nKey": "exists",
            "value": "exists",
            "priority": 1
          },
          "is": {
            "i18nKey": "is",
            "value": "is",
            "priority": 2
          },
          "isNot": {
            "i18nKey": "isNot",
            "value": "isNot",
            "priority": 3
          },
          "startsWith": {
            "i18nKey": "startsWith",
            "value": "startsWith",
            "priority": 4
          },
          "endsWith": {
            "i18nKey": "endsWith",
            "value": "endsWith",
            "priority": 5
          },
          "contains": {
            "i18nKey": "contains",
            "value": "contains",
            "priority": 6
          },
          "regex": {
            "i18nKey": "regex",
            "value": "regex",
            "priority": 7
          }
        },
        "allowAdditions": false,
        "minSelections": 1,
        "maxSelections": 1
      },
      "priority": 2
    },
    "header-value": {
      "key": "header-value",
      "defaultValue": "",
      "inputType": {
        "id": "text",
        "dataType": {
          "id": "text",
          "minLength": 0,
          "maxLength": 255,
          "defaultValue": ""
        },
        "placeholder": ""
      },
      "priority": 2
    }
  }
};

/// <reference path="../../../typings/angular2/angular2.d.ts" />
import {Inject} from 'angular2/angular2';


import {ApiRoot} from 'api/persistence/ApiRoot';
import {ActionTypeModel} from "./rule-action";


export class ActionTypesProvider {
  actionsRef:EntityMeta
  ary:Array
  map:Map<string,ActionTypeModel>
  promise:Promise

  constructor(@Inject(ApiRoot) apiRoot) {
    this.map = new Map<string,ActionTypeModel>()
    this.ary = []
    this.actionsRef = apiRoot.root.child('system/ruleengine/actionlets')
    this.init();

  }

  init() {
    this.promise = new Promise((resolve, reject) => {
      this.actionsRef.once('value', (snap) => {
        let actionlets = snap['val']()
        let results = (Object.keys(actionlets).map((key) => {
          let actionType = actionlets[key]
          this.map.set(key, new ActionTypeModel(key, actionType.i18nKey))
          return actionlets[key]
        }))

        Array.prototype.push.apply(this.ary, results);
        resolve(this);
      })
    });
  }
}


/// <reference path="../../../typings/angular2/angular2.d.ts" />
import {Inject} from 'angular2/angular2';


import {ApiRoot} from 'api/persistence/ApiRoot';


export class ConditionTypesProvider {
  ref:EntityMeta
  ary:Array
  map:Map<string,any>
  promise:Promise

  constructor(@Inject(ApiRoot) apiRoot) {
    this.map = new Map<string,any>()
    this.ary = []
    this.ref = apiRoot.root.child('system/conditionlets')
    this.init();

  }

  init() {
    this.promise = new Promise((resolve, reject) => {
      this.ref.once('value', (snap) => {
        let types = snap['val']()
        let results = (Object.keys(types).map((key) => {
          let conditionType = types[key]
          this.map.set(key, {'key': key, 'i18nKey': conditionType.i18nKey})
          return types[key]
        }))

        Array.prototype.push.apply(this.ary, results);
        resolve(this);
      })
    });
  }
}


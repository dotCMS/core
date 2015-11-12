
import {Inject, EventEmitter} from 'angular2/angular2';
//import * as Rx from '../../../node_modules/angular2/node_modules/@reactivex/rxjs/src/Rx.KitchenSink'


import {EntitySnapshot} from "../../persistence/EntityBase";
import {EntityMeta} from "../../persistence/EntityBase";
import {ApiRoot} from "../../persistence/ApiRoot";
import {CwModel} from "../../util/CwModel";


export class I18nResourceModel {

  private _locale:string
  private _key:string
  private _messages:any



  constructor(locale:string, key:string = null, messaegs:any={}) {
    this._locale = locale
    this._key = key
    this._messages = messaegs
  }

  get locale():string {
    return this._locale;
  }

  get key():string {
    return this._key;
  }

  get messages():any {
    return this._messages;
  }


}

export class I18nService {
  ref:EntityMeta

  constructor(@Inject(ApiRoot) apiRoot:ApiRoot) {
    this.ref = apiRoot.root.child('system/i18n')
  }

  static fromSnapshot(locale, key, snapshot:EntitySnapshot):I18nResourceModel {
    return new I18nResourceModel(locale, key, snapshot.val())
  }

  get(locale:string, key:string, cb:Function) {
    let key = key.replace(/\./g, '/')
    this.ref.child(locale).child(key).once('value', (snap) => {
      let rsrcModel = I18nService.fromSnapshot(locale, key, snap)
      cb(rsrcModel)
    })
  }


}


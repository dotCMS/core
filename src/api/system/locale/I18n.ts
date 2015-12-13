
import {Injectable, EventEmitter} from 'angular2/angular2';
import * as Rx from 'rxjs/Rx.KitchenSink'


import {EntitySnapshot} from "../../persistence/EntityBase";
import {EntityMeta} from "../../persistence/EntityBase";
import {ApiRoot} from "../../persistence/ApiRoot";
import {CwModel} from "../../util/CwModel";

let noop = (...arg:any[])=> {
}

export interface Internationalized {
  getMessage(key:string):string
}

export class I18nResourceModel implements Internationalized {

  private _locale:string
  private _key:string
  private _messages:any

  constructor(locale:string, key:string = null, messages:any={}) {
    this._locale = locale
    this._key = key
    this._messages = messages
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

  getMessage(key:string):string {
    let parts:Array<string> = key.split(".");
    let val = this._messages
    let result = null
    if(parts && parts.length > 0){
      for(let i = 0, L = parts.length; i < L; ++i){
        val = val[parts[i]]
        if(!val){
          break
        }
      }
      result = val;
    }
    return result
  }


}
@Injectable()
export class I18nService {
  ref:EntityMeta

  constructor(apiRoot:ApiRoot) {
    this.ref = apiRoot.root.child('system/i18n')
  }

  static fromSnapshot(locale, key, snapshot:EntitySnapshot):I18nResourceModel {
    return new I18nResourceModel(locale, key, snapshot.val())
  }

  get(locale:string, key:string, cb:Function=noop) {
    key = key.replace(/\./g, '/')
    this.ref.child(locale).child(key).once('value', (snap) => {
      let rsrcModel = I18nService.fromSnapshot(locale, key, snap)
      cb(rsrcModel)
    }, (e)=> {
      throw e
    })
  }


}


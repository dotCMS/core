import {Inject, EventEmitter} from 'angular2/angular2';
//import * as Rx from '../../../node_modules/angular2/node_modules/@reactivex/rxjs/src/Rx.KitchenSink'
import {CwChangeEvent} from "./CwEvent";
import {I18nResourceModel, Internationalized} from "../system/locale/I18n";

export class CwModel {
  private _change:EventEmitter
  private _validityChange:EventEmitter
  private _key:string
  private _priority:number

  onChange:Rx.Observable<CwChangeEvent<any>>
  onValidityChange:Rx.Observable<CwChangeEvent<any>>
  valid:boolean


  constructor(key:string = null) {
    this._change = new EventEmitter()
    this._validityChange = new EventEmitter()
    this.onChange = Rx.Observable.from(this._change.toRx()).debounceTime(200).share()
    this.onValidityChange = Rx.Observable.from(this._validityChange.toRx()).debounceTime(200).share()
    this._key = key
    this.valid = this.isValid()
  }

  get key():string {
    return this._key;
  }

  set key(value:string) {
    this._key = value;
    this._changed('key');
  }
  get priority():number {
    return this._priority;
  }

  set priority(value:number) {
    this._priority = value;
    this._changed('priority')
  }

  isPersisted():boolean {
    return !!this.key
  }

  _changed(type:string) {
    //this._checkValid()
    this._change.next({ type: type, target: this})
  }

  _checkValid() {
    let valid = this.valid
    this.valid = this.isValid()
    if (valid !== this.valid) {
      this._validityChange.next({key: 'valid', target: this, valid: this.valid})
    }
  }

  /**
   * Override me.
   * @returns {boolean}
   */
  isValid() {
    return true
  }
}


export class CwI18nModel extends CwModel{

  rsrc: any
  private _i18nKey:string
  private _i18n:I18nResourceModel


  constructor(key:string = null,  i18nKey:string = null, defaultResources:any = null) {
    super(key)
    this._i18nKey = i18nKey
    this.rsrc = defaultResources ? defaultResources : {}
  }

  get i18nKey():string {
    return this._i18nKey;
  }

  get i18n():I18nResourceModel {
    return this._i18n;
  }

  set i18n(value:I18nResourceModel) {
    this._i18n = value;
    this._changed('i18n')
    Object.assign(this.rsrc, value.messages)
  }


}
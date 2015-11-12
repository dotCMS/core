import {Inject, EventEmitter} from 'angular2/angular2';
//import * as Rx from '../../../node_modules/angular2/node_modules/@reactivex/rxjs/src/Rx.KitchenSink'
import {CwChangeEvent} from "./CwEvent";


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
    this._checkValid()
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
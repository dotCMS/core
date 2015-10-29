import {Inject, EventEmitter} from 'angular2/angular2';


export class CwModel {
  private _change:EventEmitter
  private _validityChange:EventEmitter
  private _key:string
  private _priority:number

  onChange:Rx.Observable
  onValidityChange:Rx.Observable
  valid:boolean


  constructor(key:string = null) {
    this._change = new EventEmitter()
    this.onChange = this._change.toRx()
    this._validityChange = new EventEmitter()
    this.onValidityChange = this._validityChange.toRx()
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
import {Inject, EventEmitter} from 'angular2/angular2';


export class CwModel {
  change:EventEmitter
  validityChange:EventEmitter
  valid:boolean
  private _key:string
  private _priority:number


  constructor(key:string = null) {
    this.change = new EventEmitter()
    this.validityChange = new EventEmitter()
    this._key = key
    this.valid = this.isValid()
  }

  get key():string {
    return this._key;
  }

  set key(value:string) {
    this._key = value;
    this._changed();
  }
  get priority():number {
    return this._priority;
  }

  set priority(value:number) {
    this._priority = value;
    this._changed()
  }

  _changed() {
    this._checkValid()
    this.change.next(this)
  }

  _checkValid() {
    let valid = this.valid
    this.valid = this.isValid()
    if (valid !== this.valid) {
      this.validityChange.next({target: this, valid: this.valid})
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
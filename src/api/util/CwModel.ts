import {Inject, EventEmitter} from 'angular2/angular2';
import {CwChangeEvent} from "./CwEvent";
import {I18nResourceModel, Internationalized} from "../system/locale/I18n";



export class CwModel {

  key:string
  priority:number

  constructor(key:string = null) {
    this.key = key
  }

  isPersisted():boolean {
    return !!this.key
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
    Object.assign(this.rsrc, value.messages)
  }


}
import {Inject, EventEmitter} from 'angular2/core';
import {CwChangeEvent} from "./CwEvent";
import {TreeNode} from "../system/locale/I18n";



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
  isValid():boolean {
    return true
  }
}



export class CwI18nModel extends CwModel{

  rsrc: TreeNode | any
  private _i18nKey:string


  constructor(key:string = null,  i18nKey:string = null, defaultResources:any = null) {
    super(key)
    this._i18nKey = i18nKey
    this.rsrc = defaultResources ? defaultResources : {}
  }

  get i18nKey():string {
    return this._i18nKey;
  }

}
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


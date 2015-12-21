import {Injectable, EventEmitter} from 'angular2/angular2';
import {Observable} from 'rxjs/Rx.KitchenSink'


import {EntitySnapshot} from "../../persistence/EntityBase";
import {EntityMeta} from "../../persistence/EntityBase";
import {ApiRoot} from "../../persistence/ApiRoot";
import {CwModel} from "../../util/CwModel";
import {Verify} from "../../validation/Verify";

let noop = (...arg:any[])=> {
}

export interface TreeNode {
  [key:string]: TreeNode | any
  _p: TreeNode
  _k: string
}


@Injectable()
export class I18nService {
  ref:EntityMeta
  root:TreeNode
  private _apiRoot:ApiRoot

  constructor(apiRoot:ApiRoot) {
    this.ref = apiRoot.root.child('system/i18n')
    this._apiRoot = apiRoot
    this.root = {_p: null, _k: 'root'}
  }

  treeNodeFor(locale:string, dots:string):{node:TreeNode, isNew:boolean} {
    let t = dots.split('.')
    let isNew:boolean = false
    let node = this.root
    if (!node[locale]) {
      node[locale] = {_p: this.root, _k: locale}
      isNew = true
    }
    node = node[locale]
    t.forEach((child)=> {
      if (!node[child]) {
        node[child] = {_p: node, _k: child}
        isNew = true
      }
      node = node[child]
    })
    return {node: node, isNew: isNew}
  }

  /**
   * Does NOT create the node if it is absent.
   * @param locale
   * @param dots
   */
  getTreeNode(locale:string, dots:string):TreeNode {
    let t = dots.split('.')
    let node = this.root[locale]
    if (node) {
      for (var i = 0; i < t.length && node; i++) {
        var child = t[i];
        node = node[child]
      }
    }
    return node
  }

  addAll(node:TreeNode, rsrc:TreeNode|string, key:string = null):TreeNode|any {
    if (Verify.isString(rsrc)) {
      let key = node._k
      node._p[key] = rsrc
      node = node._p[key]
    } else {
      Object.keys(rsrc).forEach((key)=> {
        node[key] = rsrc[key]
      })
    }
    return node
  }

  get(key:string, cb:Function = noop):Observable<TreeNode|any> {
    return this.getForLocale(this._apiRoot.authUser.locale, key, cb)
  }

  getForLocale(locale:string, key:string, cb:Function = noop):Observable<TreeNode|any> {
    let nodeResult
    let error;
    try {
      nodeResult = this.treeNodeFor(locale, key)
    } catch (e) {
      error = e
    }
    return Observable.defer(()=> {

      return  Observable.create((obs)=> {
        if(error){
          console.log("Emitting provided key due to error for key: ", key)
          let idx = key.lastIndexOf('.')
          let resp = key
          if(idx > 0 ){
            resp = key.substring(idx + 1)
          }
          obs.next(resp)
        }
        else {
          let path = key.replace(/\./g, '/')
          let node = nodeResult.node
          if (!nodeResult.isNew) {
            try {
              cb(node)
              obs.next(node)
              console.log("Emitting cached node value '" + node + "' for key: ", key)
            } catch (e) {
              obs.error(e)
            }
          } else {
            this.ref.child(locale).child(path).once('value', (snap) => {
              node = this.addAll(node, snap.val(), key)
              cb(node)
              obs.next(node)
              console.log("Emitting node value '" + node + "' for key: ", key)
            }, (e)=> {
              console.log('Error reading resources for: ', key)
              obs.error(e)
            })
          }
        }
      })
    })
  }


}


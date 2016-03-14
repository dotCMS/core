import {Injectable} from 'angular2/core';
import {Observable, ConnectableObservable} from 'rxjs/Rx'

import {ApiRoot} from "../persistence/ApiRoot";
import {ServerSideTypeModel} from "./ServerSideFieldModel";
import {Subscriber} from "rxjs/Subscriber";
import {Http, Response} from "angular2/http";
import {Observer} from "rxjs/Observer";

let noop = (...arg:any[])=> {
}

// @todo : Adding this meanwhile we move CountRulesActionlet to OSGI. And improve Jenkins to install osgi.
var DISABLED_ACTION_TYPE_IDS = {
  CountRulesActionlet: true
}

@Injectable()
export class ActionTypeService {
  private _apiRoot:ApiRoot
  private _http:Http
  private _baseUrl:string
  private _cacheMap:{[key:string]: ServerSideTypeModel}
  private _shareObservable:ConnectableObservable<ServerSideTypeModel[]>


  constructor(apiRoot:ApiRoot, http:Http) {
    this._apiRoot = apiRoot
    this._http = http;
    this._baseUrl = apiRoot.baseUrl + 'api/v1/system/ruleengine/actionlets'
    this._cacheMap = {}
  }

  makeRequest():Observable<any> {
    let opts = this._apiRoot.getDefaultRequestOptions()
    return this._http.get(this._baseUrl, opts).map((res:Response) => {
      return res.json()
    })
  }

  list(cb:Function = noop):ConnectableObservable<ServerSideTypeModel[]> {
    if (!this._shareObservable) {
      this._shareObservable = this._list().publishReplay()
      this._shareObservable.connect()
    }
    return this._shareObservable
  }

  private _list(cb:Function = noop):ConnectableObservable<ServerSideTypeModel[]> {
    return ConnectableObservable.create((sub:Subscriber<ServerSideTypeModel[]>)=> {

      this.makeRequest().catch((err:any, source:Observable<any>)=> {
        if (err && err.status === 404) {
          console.log("Could not retrieve Action Types: URL not valid.")
        } else if (err) {
          console.log("Could not retrieve Action Types.", "response status code: ", err.status, 'error:', err)
        }
        return Observable.create(obs => {
          obs.next([])
        })
      }).subscribe(types => {
        let hydratedTypes = []
        let keys = Object.keys(types).filter(key => DISABLED_ACTION_TYPE_IDS[key] !== true);
        let count = 0
        keys.forEach((key) => {
          let json:any = types[key]
          json.key = key
          let model = ServerSideTypeModel.fromJson(json)
          this._cacheMap[model.key] = model
          hydratedTypes.push(model)
        })
        sub.next(hydratedTypes)
        cb(hydratedTypes)
      })
    })
  }

  get(key:string):Observable<ServerSideTypeModel> {
    let cachedValue = this._cacheMap[key]
    let result
    if (cachedValue) {
      result = Observable.defer(Observable.create((obs:Observer<ServerSideTypeModel>)=> {
        obs.next(cachedValue)
        obs.complete()
      }))
    } else {
      result = Observable.defer(Observable.create((obs:Observer<ServerSideTypeModel>)=> {
        /* There is no direct endpoint to get actions by key. So we'll hydrate all of them  */
        var sub = this.list().subscribe(()=> {
          sub.unsubscribe()
          obs.next(this._cacheMap[key])
          obs.complete()
        })
      }))
    }

    return result
  }

}
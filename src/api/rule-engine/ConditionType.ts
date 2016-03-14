import {Injectable} from 'angular2/core';
import {Observable, ConnectableObservable} from 'rxjs/Rx'

import {ApiRoot} from "../persistence/ApiRoot";
import {ServerSideTypeModel} from "./ServerSideFieldModel";
import {Subscriber} from "rxjs/Subscriber";
import {Http, Response} from "angular2/http";

@Injectable()
export class ConditionTypeService {
  private _apiRoot:ApiRoot
  private _http:Http
  private _baseUrl:string
  private _shareObservable:ConnectableObservable<ServerSideTypeModel>

  constructor(apiRoot:ApiRoot, http:Http) {
    this._apiRoot = apiRoot
    this._http = http;
    this._baseUrl = apiRoot.baseUrl + 'api/v1/system/ruleengine/conditionlets'
  }

  makeRequest():Observable<any> {
    let opts = this._apiRoot.getDefaultRequestOptions()
    return this._http.get(this._baseUrl, opts).map((res:Response) => {
      return res.json()
    }).catch((err:any, source:Observable<any>)=> {
      if (err && err.status === 404) {
        console.error("Could not retrieve Condition Types: URL not valid.")
      } else if (err) {
        console.error("Could not retrieve Condition Types.", "response status code: ", err.status, 'error:', err, 'source', source)
      }
      return Observable.empty()
    })
  }

  allAsArray():Observable<ServerSideTypeModel[]> {
    return this.all().reduce(( acc:ServerSideTypeModel[], item:ServerSideTypeModel ) => {
      acc.push(item)
      return acc
    }, [])
  }

  all():ConnectableObservable<ServerSideTypeModel> {
    if(!this._shareObservable){
      this._shareObservable = this._list().publishReplay()
      this._shareObservable.connect()
    }
    return this._shareObservable
  }

  private _list():ConnectableObservable<ServerSideTypeModel> {
    return ConnectableObservable.create((sub:Subscriber<ServerSideTypeModel>)=> {
      this.makeRequest().subscribe(types => {
        let keys = Object.keys(types);
        keys.forEach((key) => {
          let json:any = types[key]
          json.key = key
          let model = ServerSideTypeModel.fromJson(json)
          sub.next(model)
        })
        sub.complete()
      })
    })
  }

}
import {Injectable} from 'angular2/core';
import {Observable} from 'rxjs/Rx'
import {ApiRoot} from "../persistence/ApiRoot";
import {ActionTypeService} from "./ActionType";
import {ServerSideTypeModel} from "./ServerSideFieldModel";
import {Http, Response} from "angular2/http";
import {ActionModel} from "./Rule";

let noop = (...arg:any[])=> {
}


@Injectable()
export class ActionService {
  private _typeService:ActionTypeService

  private _typeName:string = "Action"

  private _apiRoot:ApiRoot
  private _http:Http
  private _actionsEndpointUrl:string

  constructor(apiRoot:ApiRoot, typeService:ActionTypeService, http:Http) {
    this._apiRoot = apiRoot
    this._typeService = typeService
    this._http = http;
    this._actionsEndpointUrl = `${apiRoot.baseUrl}api/v1/sites/${apiRoot.siteId}/ruleengine/actions/`
  }


  static fromJson(type:ServerSideTypeModel, json:any):ActionModel {
    let ra = new ActionModel(json.key, type, null, json.priority)
    Object.keys(json.parameters).forEach((key)=> {
      let param = json.parameters[key]
      ra.setParameter(key, param.value)
    })
    return ra
  }

  static toJson(action:ActionModel):any {
    let json:any = {}
    json.actionlet = action.type.key
    json.priority = action.priority
    json.parameters = action.parameters
    return json
  }

  makeRequest(childPath?:string):Observable<any> {
    let opts = this._apiRoot.getDefaultRequestOptions()
    let path = this._actionsEndpointUrl
    if (childPath) {
      path = `${path}${childPath}`
    }
    return this._http.get(path, opts).map((res:Response) => {
      return res.json()
    }).catch((err:any, source:Observable<any>)=> {
      if (err && err.status === 404) {
        console.error("Could not retrieve " + this._typeName + " : 404 path not valid.", path)
      } else if (err) {
        console.log("Could not retrieve" + this._typeName + ": Response status code: ", err.status, 'error:', err, path)
      }
      return Observable.empty()
    })
  }

  allAsArray(ruleKey:string, keys:string[], ruleActionTypes?:{[key:string]: ServerSideTypeModel}):Observable<ActionModel[]> {
    return this.all(ruleKey, keys, ruleActionTypes).reduce(( acc:ActionModel[], item:ActionModel ) => {
      acc.push(item)
      return acc
    }, [])
  }

  all(ruleKey:string, keys:string[], ruleActionTypes?:{[key:string]: ServerSideTypeModel}):Observable<ActionModel> {
    return Observable.fromArray(keys).flatMap(groupKey=> {
      return this.get(ruleKey, groupKey, ruleActionTypes)
    })
  }


  get(ruleKey:string, key:string, ruleActionTypes?:{[key:string]: ServerSideTypeModel}):Observable<ActionModel> {
    let result:Observable<ActionModel>
    return this.makeRequest(key).map( (json) => {
      json.id = key
      json.key = key
      return ActionService.fromJson(ruleActionTypes[json.actionlet], json)
    })
  }


  add(ruleId: string, model:ActionModel):Observable<any> {
    console.log("Action", "add", model)
    if (!model.isValid()) {
      throw new Error("This should be thrown from a checkValid function on the model, and should provide the info needed to make the user aware of the fix.")
    }
    let json = ActionService.toJson(model)
    json.owningRule = ruleId
    let opts = this._apiRoot.getDefaultRequestOptions()
    let path = this._getPath(ruleId)

    let add = this._http.post(path, JSON.stringify(json), opts).map((res:Response) => {
      let json = res.json()
      model.key = json.id
      return model
    })
    return add.catch(this._catchRequestError('add'))
  }

  private _getPath(ruleKey:string, key?:string) {
    let p = this._actionsEndpointUrl
    if(key){
      p = p + key
    }
    return p
  }

  save(ruleId:string, model:ActionModel) {
    console.log("actionService", "save")
    if (!model.isValid()) {
      throw new Error("This should be thrown from a checkValid function on the model, and should provide the info needed to make the user aware of the fix.")
    }
    if (!model.isPersisted()) {
      this.add(ruleId, model)
    } else {
      let json = ActionService.toJson(model)
      json.owningRule = ruleId
      let opts = this._apiRoot.getDefaultRequestOptions()
      let save = this._http.put(this._getPath(ruleId, model.key), JSON.stringify(json), opts).map((res:Response) => {
        return model
      })
      return save.catch(this._catchRequestError("save"))
    }
  }

  remove(ruleId, model:ActionModel) {
    let opts = this._apiRoot.getDefaultRequestOptions()
    let remove = this._http.delete(this._getPath(ruleId, model.key), opts).map((res:Response) => {
      return model
    })
    return remove.catch(this._catchRequestError('remove'))

  }

  private _catchRequestError(operation) {
    return (err:any) => {
      if (err && err.status === 404) {
        console.log("Could not " + operation + " Rule Action: URL not valid.")
      } else if (err) {
        console.log("Could not " + operation + " Rule Action.", "response status code: ", err.status, 'error:', err)
      }
      return Observable.empty()
    }
  }
}
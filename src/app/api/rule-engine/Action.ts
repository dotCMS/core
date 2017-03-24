import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Rx';
import {ApiRoot} from '../persistence/ApiRoot';
import {ServerSideTypeModel} from './ServerSideFieldModel';
import {Http, Response} from '@angular/http';
import {ActionModel} from './Rule';
import {
    UNKNOWN_RESPONSE_ERROR, CwError, SERVER_RESPONSE_ERROR,
    NETWORK_CONNECTION_ERROR, CLIENTS_ONLY_MESSAGES
} from '../system/http-response-util';
import {LoggerService} from '../services/logger.service';

let noop = (...arg: any[]) => {
};

@Injectable()
export class ActionService {

  private _typeName = 'Action';

  private _apiRoot: ApiRoot;
  private _http: Http;
  private _actionsEndpointUrl: string;

  static fromJson(type: ServerSideTypeModel, json: any): ActionModel {
    let ra = new ActionModel(json.key, type, json.priority);
    Object.keys(json.parameters).forEach((key) => {
      let param = json.parameters[key];
      ra.setParameter(key, param.value);
    });
    return ra;
  }

  static toJson(action: ActionModel): any {
    let json: any = {};
    json.actionlet = action.type.key;
    json.priority = action.priority;
    json.parameters = action.parameters;
    return json;
  }

  constructor(apiRoot: ApiRoot, http: Http, private loggerService: LoggerService) {
    this._apiRoot = apiRoot;
    this._http = http;
    this._actionsEndpointUrl = `${apiRoot.baseUrl}api/v1/sites/${apiRoot.siteId}/ruleengine/actions/`;
  }

  makeRequest(childPath?: string): Observable<any> {
    let opts = this._apiRoot.getDefaultRequestOptions();
    let path = this._actionsEndpointUrl;
    if (childPath) {
      path = `${path}${childPath}`;
    }
    return this._http.get(path, opts).map((res: Response) => {
      return res.json();
    }).catch((err: any, source: Observable<any>) => {
      if (err && err.status === 404) {
        this.loggerService.error('Could not retrieve ' + this._typeName + ' : 404 path not valid.', path);
      } else if (err) {
        this.loggerService.debug('Could not retrieve' + this._typeName + ': Response status code: ', err.status, 'error:', err, path);
      }
      return Observable.empty();
    });
  }

  allAsArray(ruleKey: string, keys: string[], ruleActionTypes?: {[key: string]: ServerSideTypeModel}): Observable<ActionModel[]> {
    return this.all(ruleKey, keys, ruleActionTypes).reduce(( acc: ActionModel[], item: ActionModel ) => {
      acc.push(item);
      return acc;
    }, []);
  }

  all(ruleKey: string, keys: string[], ruleActionTypes?: {[key: string]: ServerSideTypeModel}): Observable<ActionModel> {
    return Observable.from(keys).flatMap(groupKey => {
      return this.get(ruleKey, groupKey, ruleActionTypes);
    });
  }

  get(ruleKey: string, key: string, ruleActionTypes?: {[key: string]: ServerSideTypeModel}): Observable<ActionModel> {
    let result: Observable<ActionModel>;
    return this.makeRequest(key).map( (json) => {
      json.id = key;
      json.key = key;
      return ActionService.fromJson(ruleActionTypes[json.actionlet], json);
    });
  }

  createRuleAction(ruleId: string, model: ActionModel): Observable<any> {
    this.loggerService.debug('Action', 'add', model);
    if (!model.isValid()) {
      throw new Error('This should be thrown from a checkValid function on the model, and should provide the info needed to make the user aware of the fix.');
    }
    let json = ActionService.toJson(model);
    json.owningRule = ruleId;
    let opts = this._apiRoot.getDefaultRequestOptions();
    let path = this._getPath(ruleId);

    let add = this._http.post(path, JSON.stringify(json), opts).map((res: Response) => {
      let json = res.json();
      model.key = json.id;
      return model;
    });
    return add.catch(this._catchRequestError('add'));
  }

  updateRuleAction(ruleId: string, model: ActionModel): Observable<ActionModel> {
    this.loggerService.debug('actionService', 'save');
    if (!model.isValid()) {
      throw new Error('This should be thrown from a checkValid function on the model, and should provide the info needed to make the user aware of the fix.');
    }
    if (!model.isPersisted()) {
      this.createRuleAction(ruleId, model);
    } else {
      let json = ActionService.toJson(model);
      json.owningRule = ruleId;
      let opts = this._apiRoot.getDefaultRequestOptions();
      let save = this._http.put(this._getPath(ruleId, model.key), JSON.stringify(json), opts).map((res: Response) => {
        return model;
      });
      return save.catch(this._catchRequestError('save'));
    }
  }

  remove(ruleId, model: ActionModel): Observable<ActionModel> {
    let opts = this._apiRoot.getDefaultRequestOptions();
    let remove = this._http.delete(this._getPath(ruleId, model.key), opts).map((res: Response) => {
      return model;
    });
    return remove.catch(this._catchRequestError('remove'));

  }

  private _getPath(ruleKey: string, key?: string): string {
    let p = this._actionsEndpointUrl;
    if (key) {
      p = p + key;
    }
    return p;
  }

  private _catchRequestError(operation):  (response: Response, original: Observable<any>) => Observable<any>  {
    return (response: Response, original: Observable<any>): Observable<any> => {
      if (response) {
        if (response.status === 500) {
          if (response.text() && response.text().indexOf('ECONNREFUSED') >= 0) {
            throw new CwError(NETWORK_CONNECTION_ERROR, CLIENTS_ONLY_MESSAGES[NETWORK_CONNECTION_ERROR]);
          } else {
            throw new CwError(SERVER_RESPONSE_ERROR, response.headers.get('error-message'));
          }
        } else if (response.status === 404) {
          this.loggerService.error('Could not execute request: 404 path not valid.');
          throw new CwError(UNKNOWN_RESPONSE_ERROR, response.headers.get('error-message'));
        } else {
          this.loggerService.debug('Could not execute request: Response status code: ', response.status, 'error:', response);
          throw new CwError(UNKNOWN_RESPONSE_ERROR, response.headers.get('error-message'));
        }
      }
      return null;
    };
  }
}
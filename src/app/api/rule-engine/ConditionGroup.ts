import {Observable} from 'rxjs/Rx';

import {Http, Response} from '@angular/http';
import {Injectable} from '@angular/core';
import {ApiRoot} from '../persistence/ApiRoot';
import { ConditionGroupModel, IConditionGroup } from './Rule';
import { HttpCode } from '../util/http-code';
import { LoggerService } from '../services/logger.service';

@Injectable()
export class ConditionGroupService {

  private _typeName = 'Condition Group';

  private _apiRoot: ApiRoot;
  private _http: Http;
  private _baseUrl: string;

  static toJson(conditionGroup: ConditionGroupModel): any {
    let json: any = {};
    json.id = conditionGroup.key;
    json.operator = conditionGroup.operator;
    json.priority = conditionGroup.priority;
    json.conditions = conditionGroup.conditions;
    return json;
  }

  static toJsonList(models: {[key: string]: ConditionGroupModel}): any {
    let list = {};
    Object.keys(models).forEach((key) => {
      list[key] = ConditionGroupService.toJson(models[key]);
    });
    return list;
  }

  constructor(apiRoot: ApiRoot, http: Http, private loggerService: LoggerService) {
    this._apiRoot = apiRoot;
    this._http = http;
    this._baseUrl = apiRoot.baseUrl + 'api/v1/sites/' + apiRoot.siteId + '/ruleengine/rules';
  }

  makeRequest(path: string): Observable<any> {
    let opts = this._apiRoot.getDefaultRequestOptions();
    return this._http.get(path, opts).map((res: Response) => {
      let json = res.json();
      this.loggerService.info('ConditionGroupService', 'makeRequest-Response', json);
      return json;
    }).catch((err: any, source: Observable<any>) => {
      if (err && err.status === HttpCode.NOT_FOUND) {
        this.loggerService.error('Could not retrieve ' + this._typeName + ' : 404 path not valid.', path);
      } else if (err) {
        this.loggerService.info('Could not retrieve' + this._typeName + ': Response status code: ', err.status, 'error:', err, path);
      }
      return Observable.empty();
    });
  }

  all(ruleKey: string, keys: string[]): Observable<ConditionGroupModel> {
    return Observable.from(keys).flatMap(groupKey => {
      return this.get(ruleKey, groupKey);
    });
  }

  allAsArray(ruleKey: string, keys: string[]): Observable<ConditionGroupModel[]> {
    return this.all(ruleKey, keys).reduce(( acc: ConditionGroupModel[], group: ConditionGroupModel ) => {
      acc.push(group);
      return acc;
    }, []);
  }

  get(ruleKey: string, key: string): Observable<ConditionGroupModel> {
    let result: Observable<ConditionGroupModel>;
    result = this.makeRequest(this._getPath(ruleKey, key)).map((json: IConditionGroup) => {
      json.id = key;
      this.loggerService.info('ConditionGroupService', 'creatingConditionGroupFromJson≠≠');
      return new ConditionGroupModel(json);
    });

    return result;
  }

  createConditionGroup(ruleId: string, model: ConditionGroupModel): Observable<any> {
    this.loggerService.info('ConditionGroupService', 'add', model);
    if (!model.isValid()) {
      throw new Error(`This should be thrown from a checkValid function on the model,
                        and should provide the info needed to make the user aware of the fix`);
    }
    let json = ConditionGroupService.toJson(model);
    let opts = this._apiRoot.getDefaultRequestOptions();
    let path = this._getPath(ruleId);

    let add = this._http.post(path, JSON.stringify(json), opts).map((res: Response) => {
      let json = res.json();
      model.key = json.id;
      return model;
    });
    return add.catch(this._catchRequestError('add'));
  }

  updateConditionGroup(ruleId: string, model: ConditionGroupModel): Observable<ConditionGroupModel> {
    this.loggerService.info('ConditionGroupService', 'save');
    if (!model.isValid()) {
      throw new Error(`This should be thrown from a checkValid function on the model,
                        and should provide the info needed to make the user aware of the fix.`);
    }
    if (!model.isPersisted()) {
      this.createConditionGroup(ruleId, model);
    } else {
      let json = ConditionGroupService.toJson(model);
      let opts = this._apiRoot.getDefaultRequestOptions();
      let save = this._http.put(this._getPath(ruleId, model.key), JSON.stringify(json), opts).map((res: Response) => {
        return model;
      });
      return save.catch(this._catchRequestError('save'));
    }
  }

  remove(ruleId: string, model: ConditionGroupModel): Observable<ConditionGroupModel>  {
    let opts = this._apiRoot.getDefaultRequestOptions();
    let remove = this._http.delete(this._getPath(ruleId, model.key), opts).map((res: Response) => {
      return model;
    });
    return remove.catch(this._catchRequestError('remove'));

  }

  private _getPath(ruleKey: string, key?: string): string {
    let p = this._baseUrl + '/' + ruleKey + '/conditionGroups/';
    if (key) {
      p = p + key;
    }
    return p;
  }

  private _catchRequestError(operation): Func {
    return (err: any) => {
      if (err && err.status === HttpCode.NOT_FOUND) {
        this.loggerService.info('Could not ' + operation + ' Condition: URL not valid.');
      } else if (err) {
        this.loggerService.info('Could not ' + operation + ' Condition.', 'response status code: ', err.status, 'error:', err);
      }
      return Observable.empty();
    };
  }
}

interface Func {
  (any): Observable<any>;
}

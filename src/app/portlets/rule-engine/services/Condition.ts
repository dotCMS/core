import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Rx';

import {ApiRoot} from '../../../api/persistence/ApiRoot';
import {ServerSideTypeModel} from './ServerSideFieldModel';
import {Http, Response} from '@angular/http';
import { ConditionGroupModel, ConditionModel, ICondition } from './Rule';
import { HttpCode } from '../../../api/util/http-code';
import { LoggerService } from '../../../api/services/logger.service';

// tslint:disable-next-line:no-unused-variable
let noop = (...arg: any[]) => {
};

@Injectable()
export class ConditionService {
  private _apiRoot: ApiRoot;
  private _http: Http;
  private _baseUrl: string;

  static toJson(condition: ConditionModel): any {
    let json: any = {};
    json.id = condition.key;
    json.conditionlet = condition.type.key;
    json.priority = condition.priority;
    json.operator = condition.operator;
    json.values = condition.parameters;
    return json;
  }

  static fromServerConditionTransformFn(condition: ICondition): ConditionModel {
    let conditionModel: ConditionModel = null;
    try {
      conditionModel = new ConditionModel(condition);
      let values = condition['values'];

      Object.keys(values).forEach((key) => {
        let x = values[key];
        conditionModel.setParameter(key, x.value, x.priority);
        // tslint:disable-next-line:no-console
        console.log('ConditionService', 'setting parameter', key, x);
      });

    } catch (e) {
      // tslint:disable-next-line:no-console
      console.error('Error reading Condition.', e);
      throw e;
    }
    return conditionModel;
  }

  constructor(apiRoot: ApiRoot, http: Http, private loggerService: LoggerService) {
    this._apiRoot = apiRoot;
    this._http = http;
    this._baseUrl = `${apiRoot.baseUrl}api/v1/sites/${apiRoot.siteId}/ruleengine/conditions`;
  }

  makeRequest(childPath: string): Observable<any> {
    let opts = this._apiRoot.getDefaultRequestOptions();
    return this._http.get(this._baseUrl + '/' + childPath, opts).map((res: Response) => {
      return res.json();
    }).catch((err: any, source: Observable<any>) => {
      if (err && err.status === HttpCode.NOT_FOUND) {
        this.loggerService.info('Could not retrieve Condition Types: URL not valid.');
      } else if (err) {
        this.loggerService.info('Could not retrieve Condition Types.', 'response status code: ', err.status, 'error:', err);
      }
      return Observable.empty();
    });
  }

  listForGroup(group: ConditionGroupModel, conditionTypes?: {[key: string]: ServerSideTypeModel}): Observable<ConditionModel[]> {
    return Observable.from(Object.keys(group.conditions)).flatMap(conditionId => {
      return this.get(conditionId, conditionTypes);
    }).reduce(( acc: ConditionModel[], entity: ConditionModel ) => {
      acc.push(entity);
      return acc;
    }, []);
  }

  get(conditionId: string, conditionTypes?: {[key: string]: ServerSideTypeModel}): Observable<ConditionModel> {
    let conditionModelResult: Observable<ICondition>;
    conditionModelResult = this.makeRequest(conditionId);

    return conditionModelResult.map((entity) => {
      entity.id = conditionId;
      entity._type = conditionTypes ? conditionTypes[entity.conditionlet] : null;
      return ConditionService.fromServerConditionTransformFn(entity);
    });
  }

  add(groupId: string, model: ConditionModel): Observable<any> {
    // this.loggerService.info("api.rule-engine.ConditionService", "add", model)
    if (!model.isValid()) {
      throw new Error(`This should be thrown from a checkValid function on the model,
                        and should provide the info needed to make the user aware of the fix.`);
    }
    let json = ConditionService.toJson(model);
    json.owningGroup = groupId;
    let opts = this._apiRoot.getDefaultRequestOptions();
    let add = this._http.post(this._baseUrl + '/', JSON.stringify(json), opts).map((res: Response) => {
      let json = res.json();
      model.key = json.id;
      return model;
    });
    return add.catch(this._catchRequestError('add'));
  }

  save(groupId: string, model: ConditionModel): Observable<ConditionModel> {
    this.loggerService.info('api.rule-engine.ConditionService', 'save', model);
    if (!model.isValid()) {
      throw new Error(`This should be thrown from a checkValid function on the model,
                        and should provide the info needed to make the user aware of the fix.`);
    }
    if (!model.isPersisted()) {
      this.add(groupId, model);
    } else {
      let json = ConditionService.toJson(model);
      json.owningGroup = groupId;
      let opts = this._apiRoot.getDefaultRequestOptions();
      let body = JSON.stringify(json);
      let save = this._http.put(this._baseUrl + '/' + model.key, body, opts).map((res: Response) => {
        return model;
      });
      return save.catch(this._catchRequestError('save'));
    }
  }

  remove(model: ConditionModel): Observable<ConditionModel>  {
    let opts = this._apiRoot.getDefaultRequestOptions();
    let remove = this._http.delete(this._baseUrl + '/' + model.key, opts).map((res: Response) => {
      return model;
    });
    return remove.catch(this._catchRequestError('remove'));
  }

  private _catchRequestError(operation): (any) => Observable<any> {
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

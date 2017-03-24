import {Http, Response, Request, Headers, RequestOptionsArgs} from '@angular/http';
import {Observable} from 'rxjs/Rx';
import {Injectable} from '@angular/core';
import {Subject} from 'rxjs/Subject';
import {Observable} from 'rxjs/Rx';

import {
    hasContent, CwError, NETWORK_CONNECTION_ERROR, UNKNOWN_RESPONSE_ERROR,
    CLIENTS_ONLY_MESSAGES, SERVER_RESPONSE_ERROR
} from '../system/http-response-util';
import {ApiRoot} from '../persistence/ApiRoot';
import {ResponseView} from './response-view';
import {HttpErrorHandler} from './http-error-handler';
import {LoggerService} from './logger.service';

export const RULE_CREATE = 'RULE_CREATE';
export const RULE_DELETE = 'RULE_DELETE';
export const RULE_UPDATE_NAME = 'RULE_UPDATE_NAME';
export const RULE_UPDATE_ENABLED_STATE = 'RULE_UPDATE_ENABLED_STATE';

export const V_RULE_UPDATE_EXPANDED_STATE = 'V_RULE_UPDATE_EXPANDED_STATE';

export const RULE_UPDATE_FIRE_ON = 'RULE_UPDATE_FIRE_ON';

export const RULE_RULE_ACTION_CREATE = 'RULE_RULE_ACTION_CREATE';
export const RULE_RULE_ACTION_DELETE = 'RULE_RULE_ACTION_DELETE';
export const RULE_RULE_ACTION_UPDATE_TYPE = 'RULE_RULE_ACTION_UPDATE_TYPE';
export const RULE_RULE_ACTION_UPDATE_PARAMETER = 'RULE_RULE_ACTION_UPDATE_PARAMETER';

export const RULE_CONDITION_GROUP_UPDATE_OPERATOR = 'RULE_CONDITION_GROUP_UPDATE_OPERATOR';
export const RULE_CONDITION_GROUP_DELETE = 'RULE_CONDITION_GROUP_DELETE';
export const RULE_CONDITION_GROUP_CREATE = 'RULE_CONDITION_GROUP_CREATE';

export const RULE_CONDITION_CREATE = 'RULE_CONDITION_CREATE';
export const RULE_CONDITION_DELETE = 'RULE_CONDITION_DELETE';
export const RULE_CONDITION_UPDATE_TYPE = 'RULE_CONDITION_UPDATE_TYPE';
export const RULE_CONDITION_UPDATE_PARAMETER = 'RULE_CONDITION_UPDATE_PARAMETER';
export const RULE_CONDITION_UPDATE_OPERATOR = 'RULE_CONDITION_UPDATE_OPERATOR';

@Injectable()
export class CoreWebService {

  private httpErrosSubjects: Subject<any>[] = [];

  constructor(private _apiRoot: ApiRoot, private _http: Http, private loggerService: LoggerService) {}

  request(options: any): Observable<any> {
    let request = this.getRequestOpts( options );
    let source = options.body;

    return this._http.request(request)
        .map((resp: Response) => {
          return hasContent(resp) ? resp.json() : resp;
        })
        .catch((response: Response, original: Observable<any>): Observable<any> => {
          if (response) {
            this.handleHttpError(response);
            if (response.status === 500) {
              if (response.text() && response.text().indexOf('ECONNREFUSED') >= 0) {
                throw new CwError(NETWORK_CONNECTION_ERROR, CLIENTS_ONLY_MESSAGES[NETWORK_CONNECTION_ERROR], request, response, source);
              } else {
                throw new CwError(SERVER_RESPONSE_ERROR, response.headers.get('error-message'), request, response, source);
              }
            }  else if (response.status === 404) {
              this.loggerService.error('Could not execute request: 404 path not valid.', options.url);
              throw new CwError(UNKNOWN_RESPONSE_ERROR, response.headers.get('error-message'), request, response, source);
            }
          }
          return null;
        });
  }

  /**
   * Return a response adapted to the follow json format:
   *
   * <code>
   * {
   *   "errors":[],
   *   "entity":{},
   *   "messages":[],
   *   "i18nMessagesMap":{}
   * }
   * </code>
   *
   * @param options
   * @returns {DotCMSHttpResponse}
   */
  public requestView(options: RequestOptionsArgs ): Observable<ResponseView> {

    let request = this.getRequestOpts(options);

    return Observable.create(observer => {

      this._http.request(request).subscribe(
          resp => {
            if (resp._body.errors && resp._body.errors.length > 0) {
              observer.error(new ResponseView(resp));
            } else {
              observer.next(new ResponseView(resp));
            }
          },
          resp => {
            this.handleHttpError(resp);
            observer.error( new ResponseView( resp ) );
          }
      );
    });
  }

  public subscribeTo(httpErrorCode: number): Observable<any> {
    if (!this.httpErrosSubjects[httpErrorCode]) {
      this.httpErrosSubjects[httpErrorCode] = new Subject();
    }

    return this.httpErrosSubjects[httpErrorCode].asObservable();
  }

  private getRequestOpts(options: any): Request {
    let headers: Headers = this._apiRoot.getDefaultRequestHeaders();
    let tempHeaders = options.headers ? options.headers : {'Content-Type': 'application/json'};

    Object.keys(tempHeaders).forEach((key) => {
      headers.set(key, tempHeaders[key]);
    });

    // https://github.com/angular/angular/issues/10612#issuecomment-238712920
    options.body = options.body && typeof options.body !== 'string' ?  JSON.stringify(options.body) :
        options.body ? options.body : '';

    options.headers = headers;

    if (options.url.indexOf('://') === -1) {
      options.url = `${this._apiRoot.baseUrl}api/${options.url}`;
    }

    return new Request(options);
  }

  private handleHttpError(response): void {
    if (!this.httpErrosSubjects[response.status]) {
      this.httpErrosSubjects[response.status] = new Subject();
    }

    this.httpErrosSubjects[response.status].next(response);
  }
}
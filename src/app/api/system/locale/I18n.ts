import {Injectable} from '@angular/core';
import {Http} from '@angular/http';
import {Observable} from 'rxjs/Rx';

import {ApiRoot} from '../../persistence/ApiRoot';
import {Verify} from '../../validation/Verify';
import {Observer} from 'rxjs/Observer';
import {LoggerService} from '../../services/logger.service';

export class TreeNode {
  [key: string]: TreeNode | any
  _p: TreeNode;
  _k: string;
  _loading: Promise<TreeNode>;
  _loaded: boolean;
  _value: string;

  constructor(parent: TreeNode, key: string) {
    this._p = parent;
    this._k = key;
    this._loading = null;
    this._loaded = false;
  }

  $addAllFromJson(key: string, childJson: any): void {
    let cNode = this.$child(key);
    if (Verify.isString(childJson)) {
      cNode._value = childJson;
    } else {
      Object.keys(childJson).forEach(cKey => {
        cNode.$addAllFromJson(cKey, childJson[cKey]);
      });
    }
    cNode._loaded = true;
  }

  $isLeaf(): boolean {
    return this._value !== undefined;
  }

  $isLoaded(): boolean {
    return this._p == null ? false : this._loaded || this._p.$isLoaded();
  }

  $isLoading(): boolean {
    return this._p == null ? false : (this._loading != null) || this._p.$isLoading();
  }

  $markAsLoaded(): void {
    this._loaded = true;
    this.$children().forEach(child => child.$markAsLoaded());
  }

  $markAsLoading(promise: Promise<TreeNode>): void {
    this._loaded = false;
    this._loading = promise;
    this.$children().forEach(child => child.$markAsLoading(promise));
  }

  $children(): TreeNode[] {
    return Object.keys(this).filter(key => key[0] !== '_').map(cKey => this[cKey]);
  }

  $child(cKey: string): TreeNode {
    let child;
    child = this[cKey];
    if (child == null) {
      child = new TreeNode(this, cKey);
      child._loading = this._loading;
      this[cKey] = child;
    }
    return child;
  }

  $descendant(path: string[]): TreeNode {
    let cKey = path[0];
    let child = this.$child(cKey);
    if (path.length > 1) {
      child = child.$descendant(path.slice(1));
    }
    return child;
  }

  $isPathLoaded(path: string[]): boolean {
    return this.$descendant(path).$isLoaded();
  }
}

@Injectable()
export class I18nService {
  root: TreeNode;
  private _apiRoot: ApiRoot;
  private _http: Http;
  private _baseUrl;

  constructor(apiRoot: ApiRoot, http: Http, private loggerService: LoggerService) {
    this._http = http;
    this._apiRoot = apiRoot;
    this._baseUrl = apiRoot.baseUrl + 'api/v1/system/i18n';
    this.root = new TreeNode(null, 'root');
  }

  makeRequest(url): Observable<Response> {
    let opts = this._apiRoot.getDefaultRequestOptions();
    return this._http.get(this._baseUrl + '/' + url, opts).map((res) => {
      return res.json();
    });
  }

  get(msgKey: string, defaultValue: any= '-error loading resource-'): Observable<TreeNode|any> {
    return this.getForLocale(this._apiRoot.authUser.locale, msgKey, true, defaultValue);
  }

  getForLocale(locale: string, msgKey: string, forceText = true, defaultValue: any= '-error loading resource-'): Observable<TreeNode | any> {
    msgKey = locale + '.' + msgKey;
    let path = msgKey.split('.');
    let cNode = this.root.$descendant(path);
    if (!cNode.$isLoaded() && !cNode.$isLoading()) {
      let promise = new Promise((resolve, reject) => {
        this.makeRequest(path.join('/')).catch((err: any, source: Observable<any>) => {
          if (err && err.status === 404) {
            this.loggerService.debug('Missing Resource: \'' , msgKey, '\'');
          } else {
            this.loggerService.debug('I18n', 'Failed:: ', msgKey, '=', cNode, 'error:', err);
          }
          return Observable.create(obs => {
            obs.next(defaultValue);
          });
        }).subscribe(jsonVal => {
          cNode._p.$addAllFromJson(cNode._k, jsonVal);
          cNode.$markAsLoaded();
          resolve(cNode);
        });
      });
      cNode.$markAsLoading(promise);
    }

    return Observable.defer(() => {
      return Observable.create((obs: Observer<string>  ) => {
        if (cNode._loading == null) {
          this.loggerService.debug('I18n', 'Failed: ', msgKey, '=', cNode);
          obs.next('-I18nLoadFailed-');
          obs.complete();
        } else {
          cNode._loading.then(() => {
            let v;
            if (!cNode.$isLeaf() ) {
                if (forceText) {
                  v = defaultValue;
                } else {
                  v = cNode;
                }
            } else {
              v = cNode._value;
            }
            obs.next(v);
            obs.complete();
          });
        }
      });
    });
  }
}

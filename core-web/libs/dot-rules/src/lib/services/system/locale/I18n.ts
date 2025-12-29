import { defer as observableDefer, Observer, Observable } from 'rxjs';

import { HttpClient, HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError } from 'rxjs/operators';

import { ApiRoot, LoggerService } from '@dotcms/dotcms-js';

import { Verify } from '../../validation/Verify';

export class TreeNode {
    [key: string]: TreeNode | any;
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
        const cNode = this.$child(key);
        if (Verify.isString(childJson)) {
            cNode._value = childJson.toString();
        } else {
            Object.keys(childJson).forEach((cKey) => {
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
        return this._p == null ? false : this._loading != null || this._p.$isLoading();
    }

    $markAsLoaded(): void {
        this._loaded = true;
        this.$children().forEach((child) => child.$markAsLoaded());
    }

    $markAsLoading(promise: Promise<TreeNode>): void {
        this._loaded = false;
        this._loading = promise;
        this.$children().forEach((child) => child.$markAsLoading(promise));
    }

    $children(): TreeNode[] {
        return Object.keys(this)
            .filter((key) => key[0] !== '_')
            .map((cKey) => this[cKey]);
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
        const cKey = path[0];
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
    private http = inject(HttpClient);
    private loggerService = inject(LoggerService);

    root: TreeNode;
    private _apiRoot: ApiRoot;
    private _baseUrl;

    constructor() {
        const apiRoot = inject(ApiRoot);

        this._apiRoot = apiRoot;
        this._baseUrl = '/api/v1/system/i18n';
        this.root = new TreeNode(null, 'root');
    }

    makeRequest<T>(url: string): Observable<T> {
        return this.http.get<T>(`${this._baseUrl}/${url}`);
    }

    get(
        msgKey: string,
        defaultValue: any = '-error loading resource-'
    ): Observable<TreeNode | any> {
        return this.getForLocale(this._apiRoot.authUser.locale, msgKey, true, defaultValue);
    }

    getForLocale(
        locale: string,
        msgKey: string,
        forceText = true,
        defaultValue: any = '-error loading resource-'
    ): Observable<TreeNode | any> {
        msgKey = locale + '.' + msgKey;
        const path = msgKey.split('.');
        const cNode = this.root.$descendant(path);
        if (!cNode.$isLoaded() && !cNode.$isLoading()) {
            const promise = new Promise((resolve, _reject) => {
                this.makeRequest(path.join('/'))
                    .pipe(
                        catchError((err: HttpErrorResponse, _source: Observable<any>) => {
                            if (err && err.status === HttpStatusCode.NotFound) {
                                this.loggerService.debug("Missing Resource: '", msgKey, "'");
                            } else {
                                this.loggerService.debug(
                                    'I18n',
                                    'Failed:: ',
                                    msgKey,
                                    '=',
                                    cNode,
                                    'error:',
                                    err
                                );
                            }

                            return new Observable((obs) => {
                                obs.next(defaultValue);
                            });
                        })
                    )
                    .subscribe((jsonVal) => {
                        cNode._p.$addAllFromJson(cNode._k, jsonVal);
                        cNode.$markAsLoaded();
                        resolve(cNode);
                    });
            });
            cNode.$markAsLoading(<Promise<TreeNode>>promise);
        }

        return observableDefer(() => {
            return new Observable((obs: Observer<string>) => {
                if (cNode._loading == null) {
                    this.loggerService.debug('I18n', 'Failed: ', msgKey, '=', cNode);
                    obs.next('-I18nLoadFailed-');
                    obs.complete();
                } else {
                    cNode._loading.then(() => {
                        let v;
                        if (!cNode.$isLeaf()) {
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

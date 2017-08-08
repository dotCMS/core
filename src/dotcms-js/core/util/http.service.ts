import {Inject, Injectable} from '@angular/core';
import {Http, Headers, Response, RequestMethod, RequestOptions} from '@angular/http';
import {Observable} from 'rxjs';

import 'rxjs/add/operator/catch';
import 'rxjs/add/operator/toPromise';
import 'rxjs/add/operator/debounceTime';

import {SettingsStorageService} from './settings-storage.service';
import {LoggerService} from './logger.service';

/**
 * The HTTPClient will use the JWTToken and Host/Site set in the SettingsStorageService to connect dotCMS REST Endpoints
 *
 */
@Injectable()
@Inject('http')
@Inject('log')
@Inject('settingsStorageService')
export class HttpClient {
    public progress$: any;
    private progressObserver: any;
    private progress: number;
    constructor(
        private http: Http,
        private log: LoggerService,
        private settingsStorageService: SettingsStorageService

    ) {
        // this.http = http;
        // this.settingsService=settingsService;
        this.progress$ = Observable.create((observer: any) => {
            this.progressObserver = observer;
        }).share();
    }

    /**
     * Will append JWT Header to passed in Headers
     * @param headers
     */
    createAuthorizationHeader(headers: Headers): void {
        if (this.settingsStorageService.getSettings() != null && this.settingsStorageService.getSettings().jwt != null
            && this.settingsStorageService.getSettings().jwt.trim().length > 0 ) {
            headers.append('Authorization', 'Bearer ' + this.settingsStorageService.getSettings().jwt);
        }
    }

    /**
     * Currently uses a debouce time of 400 and distinctUntilChanged flags on a GET request.  This is intended to
     * limit unecessary requests to the dotCMS Endpoints. Will append needed dotCMS Host/Site and JWT AUth Token
     * @param path Endpoint path
     * @returns {Observable<Response>}
     */
    get(path: string): Observable<Response> {
        let headers = new Headers();
        this.createAuthorizationHeader(headers);
        let site: String = this.settingsStorageService.getSettings().site;
        return this.http.get( (site ? site : '') + path, {headers: headers})
            .debounceTime(400)
            .distinctUntilChanged();
    }

    /**
     * Currently uses a debouce time of 400 and distinctUntilChanged flags on a GET request.  This is intended to
     * limit unecessary requests to the dotCMS Endpoints. Will append needed dotCMS Host/Site and JWT AUth Token
     * @param path path Endpoint path
     * @param data Object to be PUT.  Will be converted to JSON String(JSON.stringify)
     * @returns {Observable<Response>}
     */
    put(path: String, data: Object): Observable<Response> {
        let opts: RequestOptions = new RequestOptions();
        opts.method = RequestMethod.Put;
        opts.headers = new Headers({'Content-Type': 'application/json'});
        this.createAuthorizationHeader(opts.headers);
        return this.http.put(this.settingsStorageService.getSettings().site + path.toString(), JSON.stringify(data), opts)
            .debounceTime(400)
            .distinctUntilChanged();
    }

    /**
     * Currently uses a debouce time of 400 and distinctUntilChanged flags on a GET request.  This is intended to
     * limit unecessary requests to the dotCMS Endpoints. Will append needed dotCMS Host/Site and JWT AUth Token
     * @param path path Endpoint path
     * @param data Object to be POSTed.  Will be converted to JSON String(JSON.stringify)
     * @returns {Observable<Response>}
     */
    post(path: String, data: Object): Observable<Response> {
        let opts: RequestOptions = new RequestOptions();
        opts.method = RequestMethod.Post;
        opts.headers = new Headers({'Content-Type': 'application/json'});
        this.createAuthorizationHeader(opts.headers);
        return this.http.post(this.settingsStorageService.getSettings().site + path, JSON.stringify(data), opts );
    }

    /**
     * Intended to simply saving FileAssets to dotCMS. Currently uses a debouce time of 400 and distinctUntilChanged flags on a GET request
     * This is intended to
     * limit unecessary requests to the dotCMS Endpoints. Will append needed dotCMS Host/Site and JWT AUth Token
     * @param path path Endpoint path
     * @param file Binary file to save
     * @param jsonData Object to be POSTed.  Will be converted to JSON String(JSON.stringify)
     * @returns {any}
     */
    filePut(path: String, file: File, data: Object): Observable<any> {
        return Observable.create((observer: any) => {
            let formData: FormData = new FormData(), xhr: XMLHttpRequest = new XMLHttpRequest();
            formData.append('json', JSON.stringify(data));

            this.log.debug('File to push is : ' + file.name);
            formData.append('fileAsset', file);
            xhr.onreadystatechange = () => {
                if (xhr.readyState === 4) {
                    if (xhr.status === 200) {
                        observer.next(JSON.parse(xhr.response));
                        observer.complete();
                    } else {
                        observer.error(xhr.response);
                    }
                }
            };

            xhr.upload.onprogress = (event) => {
                this.progress = Math.round(event.loaded / event.total * 100);

                this.progressObserver.next(this.progress);
            };
            let site: String = this.settingsStorageService.getSettings().site;
            xhr.open('PUT', (site ? site : '') + path.toString(), true);
            if (this.settingsStorageService.getSettings() != null && this.settingsStorageService.getSettings().jwt != null
                && this.settingsStorageService.getSettings().jwt.trim().length > 0 ) {
                xhr.setRequestHeader('Authorization', 'Bearer ' + this.settingsStorageService.getSettings().jwt);
            }
            this.log.debug('FormData is ' + formData);
            xhr.send(formData);
        });
    }
}

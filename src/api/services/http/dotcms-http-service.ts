import {Http, Response, Request, Headers, RequestOptionsArgs} from '@angular/http'
import {Observable} from 'rxjs/Rx'
import {ApiRoot} from "../../persistence/ApiRoot";
import {DotCMSHttpResponse} from "./dotcms-http-response";
import {Injectable} from '@angular/core';

@Injectable()
export class DotCMSHttpService{
    constructor( public _apiRoot: ApiRoot,public  _http:Http){

    }

    request(options:RequestOptionsArgs):Observable<any> {
        let headers:Headers = this._apiRoot.getDefaultRequestHeaders()
        let tempHeaders = options.headers ? options.headers : {"Content-Type": "application/json"}
        Object.keys(tempHeaders).forEach((key)=> {
            headers.set(key, tempHeaders[key])
        })
        var source = options.body
        if (options.body) {
            if (typeof options.body !== 'string') {
                options.body = JSON.stringify(options.body);
            }
        }
        options.headers = headers

        var request = new Request(options)
        return   Observable.create(observer => {
            this._http.request(request).subscribe(
                resp => {
                    console.log('RESP', resp);
                    if (resp._body.errors && resp._body.errors.length > 0){
                        observer.error(new DotCMSHttpResponse(resp));
                    }else {
                        observer.next(new DotCMSHttpResponse(resp));
                    }
                },
                resp => {
                    console.log('ERROR', resp);
                    observer.error( new DotCMSHttpResponse( resp ) )
                }
            );
        });
    }
}

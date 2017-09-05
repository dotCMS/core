import { Injectable } from '@angular/core';
import { Jsonp } from '@angular/http';
import { Observable } from 'rxjs';
import 'rxjs/Rx';

@Injectable()
export class GravatarService {

    constructor(private jsonp: Jsonp) {
    }

    loadGravatarProfile(hash): Observable<any> {
        return this.jsonp.get(`//www.gravatar.com/${hash}.json?callback=JSONP_CALLBACK`)
          .map(data => data.json());
    }
}

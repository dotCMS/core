import { map } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { Jsonp } from '@angular/http';
import { Observable } from 'rxjs';

@Injectable()
export class GravatarService {
    constructor(private jsonp: Jsonp) {}

    loadGravatarProfile(hash): Observable<any> {
        return this.jsonp
            .get(`//www.gravatar.com/${hash}.json?callback=JSONP_CALLBACK`)
            .pipe(map((data) => data.json()));
    }
}

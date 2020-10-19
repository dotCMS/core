import { map, pluck, catchError } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { HttpClient } from '@angular/common/http';

interface DotProfile {
    displayName: string;
    hash: string;
    id: string;
    name: string[];
    photos: {
        type: string;
        value: string;
    };
    preferredUsername: string;
    profileUrl: string;
    requestHash: string;
    thumbnailUrl: string;
    urls: string[];
}

/**
 * Connect with Gravatar API
 *
 * @export
 * @class DotGravatarService
 */
@Injectable()
export class DotGravatarService {
    constructor(private http: HttpClient) {}

    /**
     * Load the avatar url from a hash
     *
     * @param {string} hash
     * @returns {Observable<string>}
     * @memberof DotGravatarService
     */
    getPhoto(hash: string): Observable<string> {
        return this.http.jsonp(`//www.gravatar.com/${hash}.json?`, 'JSONP_CALLBACK')
            .pipe(
                pluck('_body'),
                pluck('entry'),
                map((profiles: DotProfile[]) => profiles[0].photos[0].value),
                catchError(() => {
                    return of(null);
                })
            );
    }
}

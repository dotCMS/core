import md5 from 'md5';
import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { map, pluck } from 'rxjs/operators';

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
     * @param {string} email
     * @returns {Observable<string>}
     * @memberof DotGravatarService
     */
    getPhoto(email: string): Observable<string> {
        const hash = md5(email);

        return this.http.jsonp(`//www.gravatar.com/${hash}.json?`, 'callback').pipe(
            pluck('entry'),
            map((profiles: DotProfile[]) => {
                return profiles[0].photos[0].value;
            })
        );
    }
}

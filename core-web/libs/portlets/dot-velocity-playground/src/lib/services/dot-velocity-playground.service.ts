import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import {
    DotVelocityPlaygroundForm,
    DotVelocityPlaygroundResponse,
    DotVelocityResponseContentType
} from '../models/dot-velocity-playground.models';

@Injectable({ providedIn: 'root' })
export class DotVelocityPlaygroundService {
    readonly #http = inject(HttpClient);

    runScript(form: DotVelocityPlaygroundForm): Observable<DotVelocityPlaygroundResponse> {
        const started = Date.now();

        return this.#http
            .post('/api/vtl/dynamic/', form, {
                observe: 'response',
                responseType: 'text'
            })
            .pipe(
                map((response) => ({
                    body: response.body ?? '',
                    contentType: this.#mapContentType(response.headers.get('content-type')),
                    elapsedMs: Date.now() - started
                }))
            );
    }

    #mapContentType(raw: string | null): DotVelocityResponseContentType {
        const ct = (raw ?? '').toLowerCase().split(';')[0].trim();
        if (ct.includes('json')) return 'json';
        if (ct.includes('xml')) return 'xml';

        return 'plaintext';
    }
}

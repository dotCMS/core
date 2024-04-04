import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

@Injectable()
export class DotScriptingApiService {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = '/api/vtl';

    get<T>(path: string): Observable<T> {
        return this.http.get<T>(`${this.baseUrl}/${path}`);
    }
}

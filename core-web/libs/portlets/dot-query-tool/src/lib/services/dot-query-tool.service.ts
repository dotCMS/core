import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';

import { map } from 'rxjs/operators';

import { QueryToolSearchForm, QueryToolSearchResponse } from '../models/dot-query-tool.models';

interface SearchEnvelope {
    entity: QueryToolSearchResponse;
}

@Service()
export class DotQueryToolService {
    readonly #http = inject(HttpClient);

    search(form: QueryToolSearchForm): Observable<QueryToolSearchResponse> {
        return this.#http
            .post<SearchEnvelope>('/api/v1/content/_search', form)
            .pipe(map((response) => response.entity));
    }
}

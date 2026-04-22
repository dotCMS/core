import { Observable } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotLanguage } from '@dotcms/dotcms-models';

import { DOT_CMS_AUTH_TOKEN, DOT_CMS_BASE_URL } from './dot-cms.config';

@Injectable({ providedIn: 'root' })
export class DotCmsLanguageService {
    private readonly http = inject(HttpClient);

    getById(id: number): Observable<DotLanguage> {
        const headers = new HttpHeaders({ Authorization: `Bearer ${DOT_CMS_AUTH_TOKEN}` });

        return this.http
            .get<{ entity: DotLanguage }>(`${DOT_CMS_BASE_URL}/api/v2/languages/id/${id}`, {
                headers
            })
            .pipe(map((res) => res.entity));
    }
}

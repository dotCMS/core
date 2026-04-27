import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotLanguage } from '@dotcms/dotcms-models';

@Injectable({ providedIn: 'root' })
export class DotLanguageService {
    private readonly http = inject(HttpClient);

    getById(id: number): Observable<DotLanguage> {
        return this.http
            .get<{ entity: DotLanguage }>(`/api/v2/languages/id/${id}`, {
                withCredentials: true
            })
            .pipe(map((res) => res.entity));
    }
}

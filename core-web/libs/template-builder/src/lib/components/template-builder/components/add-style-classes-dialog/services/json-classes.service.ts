import { Observable, of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError, map } from 'rxjs/operators';

export const STYLE_CLASSES_FILE_URL = '/application/templates/classes.json';

@Injectable()
export class JsonClassesService {
    readonly #http = inject(HttpClient);

    getClasses(): Observable<string[]> {
        return this.#http.get<{ classes: string[] }>(STYLE_CLASSES_FILE_URL).pipe(
            map((res) => res?.classes || []),
            catchError(() => of([]))
        );
    }
}

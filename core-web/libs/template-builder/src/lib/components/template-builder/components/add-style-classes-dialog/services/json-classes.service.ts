import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

export const STYLE_CLASSES_FILE_URL = '/application/templates/classes.json';

@Injectable()
export class JsonClassesService {
    constructor(private http: HttpClient) {}

    getClasses(): Observable<{ classes: string[] }> {
        return this.http.get<{ classes: string[] }>(STYLE_CLASSES_FILE_URL);
    }
}

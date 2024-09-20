import { of } from 'rxjs';
import { RawEditorOptions } from 'tinymce';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError } from 'rxjs/operators';

export const CONFIG_PATH = '/api/vtl';

@Injectable()
export class DotWysiwygTinymceService {
    #http = inject(HttpClient);

    getProps() {
        return this.#http
            .get<RawEditorOptions>(`${CONFIG_PATH}/tinymceprops`)
            .pipe(catchError(() => of(null)));
    }
}

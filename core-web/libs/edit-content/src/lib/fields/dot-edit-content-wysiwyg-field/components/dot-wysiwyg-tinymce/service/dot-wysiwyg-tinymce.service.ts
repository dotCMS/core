import { of } from 'rxjs';
import { RawEditorOptions } from 'tinymce';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError } from 'rxjs/operators';

const CONFIG_PATH = '/api/vtl/tinymceprops';

@Injectable()
export class DotWysiwygTinymceService {
    #http = inject(HttpClient);

    getProps() {
        return this.#http.get<RawEditorOptions>(CONFIG_PATH).pipe(catchError(() => of(null)));
    }
}

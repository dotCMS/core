import { pluck } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { DotLanguage } from '@models/dot-language/dot-language.model';
import { CoreWebService } from '@dotcms/dotcms-js';

/**
 * Provide util methods to get Languages available in the system.
 * @export
 * @class DotLanguagesService
 */
@Injectable()
export class DotLanguagesService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Return languages.
     * @returns Observable<DotLanguage[]>
     * @memberof DotLanguagesService
     */
    get(contentInode?: string): Observable<DotLanguage[]> {
        return this.coreWebService
            .requestView({
                url: !contentInode ? 'v2/languages' : `v2/languages?contentInode=${contentInode}`
            })
            .pipe(pluck('entity'));
    }
}

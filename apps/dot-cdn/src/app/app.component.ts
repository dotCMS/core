import { Component } from '@angular/core';
import { CoreWebService } from '@dotcms/dotcms-js';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Component({
    selector: 'dotcms-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss']
})
export class AppComponent {
    content$: Observable<{ label: string; name: string }[]>;
    cities = [
        { name: 'New York', code: 'NY' },
        { name: 'Rome', code: 'RM' },
        { name: 'London', code: 'LDN' },
        { name: 'Istanbul', code: 'IST' },
        { name: 'Paris', code: 'PRS' }
    ];

    constructor(private coreWebService: CoreWebService) {
        this.content$ = this.coreWebService
            .requestView({
                url: '/api/v1/contenttype/basetypes'
            })
            .pipe(map((res) => res.entity));
    }
}

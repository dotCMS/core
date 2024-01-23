import { Observable } from 'rxjs';

import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { EmaAppConfigurationService, EmaAppSecretValue } from '@dotcms/data-access';

@Component({
    selector: 'dot-pages-card',
    templateUrl: './dot-pages-card.component.html',
    styleUrls: ['./dot-pages-card.component.scss']
})
export class DotPagesCardComponent implements OnInit {
    @Input() actionButtonId: string;
    @Input() imageUri: string;
    @Input() title: string;
    @Input() url: string;
    @Input() ownerPage: boolean;
    @Output() edit = new EventEmitter<boolean>();
    @Output() goTo = new EventEmitter<boolean>();
    @Output() showActionMenu = new EventEmitter<MouseEvent>();

    private emaAppConfigurationService = inject(EmaAppConfigurationService);

    emaConfig$: Observable<EmaAppSecretValue | unknown>;

    ngOnInit(): void {
        this.emaConfig$ = this.emaAppConfigurationService
            .get(this.url.split('?')[0])
            .pipe(map((res) => res ?? {}));
    }
}

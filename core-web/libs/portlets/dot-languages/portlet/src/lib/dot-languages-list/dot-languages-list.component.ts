import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';

import { DotLanguage } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotLanguagesListStore, DotLanguagesListViewModel } from './store/dot-languages-list.store';

@Component({
    selector: 'dot-languages-list',
    standalone: true,
    imports: [CommonModule, InputTextModule, ButtonModule, DotMessagePipe, TableModule],
    templateUrl: './dot-languages-list.component.html',
    styleUrl: './dot-languages-list.component.scss',
    providers: [DotLanguagesListStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotLanguagesListComponent {
    languagesList: DotLanguage[] = [];
    vm$: Observable<DotLanguagesListViewModel> = this.dotLanguagesListStore.vm$;

    constructor(
        private readonly route: ActivatedRoute,
        private readonly dotLanguagesListStore: DotLanguagesListStore
    ) {}

    ngOnInit() {
        this.dotLanguagesListStore.setLanguages(this.route.snapshot.data['languages']);
        this.languagesList = this.route.snapshot.data['languages'];
    }
}

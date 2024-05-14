import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { DotActionMenuButtonComponent, DotMessagePipe } from '@dotcms/ui';

import { DotLanguagesListStore, DotLanguagesListViewModel } from './store/dot-languages-list.store';

@Component({
    selector: 'dot-languages-list',
    standalone: true,
    imports: [
        CommonModule,
        InputTextModule,
        ButtonModule,
        DotMessagePipe,
        TableModule,
        DotActionMenuButtonComponent,
        TagModule
    ],
    templateUrl: './dot-languages-list.component.html',
    styleUrl: './dot-languages-list.component.scss',
    providers: [DotLanguagesListStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotLanguagesListComponent implements OnInit {
    vm$: Observable<DotLanguagesListViewModel> = this.dotLanguagesListStore.vm$;

    constructor(
        private readonly route: ActivatedRoute,
        private readonly dotLanguagesListStore: DotLanguagesListStore
    ) {}

    ngOnInit() {
        this.dotLanguagesListStore.setLanguages(this.route.snapshot.data['languages']);
    }
}

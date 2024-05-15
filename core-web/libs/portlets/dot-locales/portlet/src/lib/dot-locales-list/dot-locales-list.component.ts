import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { DotActionMenuButtonComponent, DotMessagePipe } from '@dotcms/ui';

import { DotLocaleListViewModel, DotLocalesListStore } from './store/dot-locales-list.store';

@Component({
    selector: 'dot-locales-list',
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
    templateUrl: './dot-locales-list.component.html',
    styleUrl: './dot-locales-list.component.scss',
    providers: [DotLocalesListStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotLocalesListComponent implements OnInit {
    vm$: Observable<DotLocaleListViewModel> = this.dotLocalesListStore.vm$;

    constructor(
        private readonly route: ActivatedRoute,
        private readonly dotLocalesListStore: DotLocalesListStore
    ) {}

    ngOnInit() {
        this.dotLocalesListStore.setLocales(this.route.snapshot.data['locales']);
    }
}

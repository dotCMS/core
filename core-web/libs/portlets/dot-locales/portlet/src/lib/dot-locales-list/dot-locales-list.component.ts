import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogService } from 'primeng/dynamicdialog';
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
    providers: [DotLocalesListStore, DialogService, MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotLocalesListComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    store = inject(DotLocalesListStore);

    vm$: Observable<DotLocaleListViewModel> = this.store.vm$;

    ngOnInit() {
        const { data, pushPublishEnvironments, isEnterprise } = this.route.snapshot.data;
        this.store.setResolvedData({ data, pushPublishEnvironments, isEnterprise });
    }
}

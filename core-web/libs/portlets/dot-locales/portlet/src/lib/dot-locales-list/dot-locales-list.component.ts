import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { DialogService } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';

import { DotActionMenuButtonComponent, DotMessagePipe, DotStateRestoreDirective } from '@dotcms/ui';

import {
    DotLocaleListViewModel,
    DotLocalesListStore,
    LOCALE_CONFIRM_DIALOG_KEY
} from './store/dot-locales-list.store';

@Component({
    selector: 'dot-locales-list',
    host: { class: 'flex flex-1 flex-col' },
    imports: [
        CommonModule,
        ButtonModule,
        ConfirmDialogModule,
        ConfirmPopupModule,
        DotActionMenuButtonComponent,
        DotMessagePipe,
        DotStateRestoreDirective,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        TableModule,
        TagModule,
        ToastModule
    ],
    templateUrl: './dot-locales-list.component.html',
    styleUrl: './dot-locales-list.component.scss',
    providers: [DotLocalesListStore, DialogService, MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotLocalesListComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    store = inject(DotLocalesListStore);
    dialogKey = LOCALE_CONFIRM_DIALOG_KEY;
    vm$: Observable<DotLocaleListViewModel> = this.store.vm$;

    ngOnInit() {
        const { pushPublishEnvironments, isEnterprise } = this.route.snapshot.data;
        this.store.loadLocales({ pushPublishEnvironments, isEnterprise });
    }
}

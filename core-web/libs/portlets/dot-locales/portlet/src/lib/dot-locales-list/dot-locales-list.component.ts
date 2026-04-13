import { Observable } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, viewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { MenuItem, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { ContextMenu, ContextMenuModule } from 'primeng/contextmenu';
import { DialogService } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';

import { DotMessagePipe, DotStateRestoreDirective } from '@dotcms/ui';

import {
    DotLocaleListViewModel,
    DotLocaleRow,
    DotLocalesListStore,
    LOCALE_CONFIRM_DIALOG_KEY
} from './store/dot-locales-list.store';

@Component({
    selector: 'dot-locales-list',
    host: { class: 'flex flex-1 flex-col' },
    imports: [
        AsyncPipe,
        ButtonModule,
        ConfirmDialogModule,
        ConfirmPopupModule,
        ContextMenuModule,
        DotMessagePipe,
        DotStateRestoreDirective,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        TableModule,
        TagModule,
        ToolbarModule,
        ToastModule
    ],
    templateUrl: './dot-locales-list.component.html',
    providers: [DotLocalesListStore, DialogService, MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotLocalesListComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly contextMenu = viewChild.required<ContextMenu>('rowMenu');

    store = inject(DotLocalesListStore);
    dialogKey = LOCALE_CONFIRM_DIALOG_KEY;
    vm$: Observable<DotLocaleListViewModel> = this.store.vm$;
    rowMenuItems: MenuItem[] = [];

    ngOnInit() {
        const { pushPublishEnvironments, isEnterprise } = this.route.snapshot.data;
        this.store.loadLocales({ pushPublishEnvironments, isEnterprise });
    }

    openRowMenu(event: MouseEvent, locale: DotLocaleRow): void {
        this.rowMenuItems = locale.actions
            .filter((a) => !a.shouldShow || a.shouldShow())
            .map((a) => a.menuItem as MenuItem);
        this.contextMenu().show(event);
    }
}

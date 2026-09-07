import { Observable } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, viewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { MenuItem, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { ContextMenu, ContextMenuModule } from 'primeng/contextmenu';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';

import { filter, take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { DotLanguage } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotStateRestoreDirective } from '@dotcms/ui';

import { DotLocaleCreateEditComponent } from './components/dot-locale-create-edit/dot-locale-create-edit.component';
import {
    DotLocaleListViewModel,
    DotLocaleRow,
    DotLocalesListStore,
    LOCALE_CONFIRM_DIALOG_KEY
} from './store/dot-locales-list.store';

import { DotLocaleConfirmationDialogComponent } from '../share/ui/DotLocaleConfirmationDialog/DotLocaleConfirmationDialog.component';
import { getLocaleISOCode } from '../share/utils';

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
        ChipModule,
        TableModule,
        ToolbarModule,
        ToastModule
    ],
    templateUrl: './dot-locales-list.component.html',
    providers: [DotLocalesListStore, DialogService, MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotLocalesListComponent implements OnInit {
    readonly #route = inject(ActivatedRoute);
    private readonly contextMenu = viewChild.required<ContextMenu>('rowMenu');
    readonly #dialogService = inject(DialogService);
    readonly #dotPushPublishDialogService = inject(DotPushPublishDialogService);
    readonly #dotMessageService = inject(DotMessageService);

    store = inject(DotLocalesListStore);
    dialogKey = LOCALE_CONFIRM_DIALOG_KEY;
    vm$: Observable<DotLocaleListViewModel> = this.store.vm$;
    rowMenuItems: MenuItem[] = [];

    ngOnInit() {
        const { pushPublishEnvironments, isEnterprise } = this.#route.snapshot.data;
        this.store.loadLocales({ pushPublishEnvironments, isEnterprise });
    }

    openEditDialog(locale: DotLocaleRow | null, vm: DotLocaleListViewModel): void {
        const localeToEdit = locale ? vm.locales.find((l) => l.id === locale.id) : null;

        const dialogRef: DynamicDialogRef = this.#dialogService.open(DotLocaleCreateEditComponent, {
            closable: true,
            closeOnEscape: true,
            draggable: false,
            header: this.#dotMessageService.get(
                localeToEdit ? 'locales.edit.locale' : 'locales.add.locale'
            ),
            width: '700px',
            data: {
                languages: vm.languages,
                countries: vm.countries,
                locale: localeToEdit,
                localeList: vm.locales
            }
        });

        dialogRef.onClose
            .pipe(
                take(1),
                filter((result) => result)
            )
            .subscribe((result: DotLanguage) => {
                if (result.id) {
                    this.store.updateLocale({ ...result });
                } else {
                    this.store.addLocale(result);
                }
            });
    }

    openRowMenu(event: MouseEvent, locale: DotLocaleRow, vm: DotLocaleListViewModel): void {
        const isPushPublishEnabled = vm.isEnterprise && vm.pushPublishEnvironments.length > 0;
        const defaultLocale = vm.locales.find((l) => l.defaultLanguage);

        const items: MenuItem[] = [
            {
                label: this.#dotMessageService.get('locales.edit'),
                command: () => this.openEditDialog(locale, vm)
            }
        ];

        if (isPushPublishEnabled) {
            items.push({
                label: this.#dotMessageService.get('locales.push.publish'),
                command: () =>
                    this.#dotPushPublishDialogService.open({
                        assetIdentifier: locale.id.toString(),
                        title: this.#dotMessageService.get('contenttypes.content.push_publish')
                    })
            });
        }

        if (!locale.defaultLanguage && defaultLocale) {
            items.push(
                {
                    label: this.#dotMessageService.get('locales.set.as.default'),
                    command: () =>
                        this.#openConfirmDialog(
                            locale,
                            defaultLocale,
                            'locale.set.default.confirmation.title',
                            'locale.set.default.confirmation.message',
                            'locale.set.default.confirmation.accept.button',
                            () => this.store.makeDefaultLocale(locale.id)
                        )
                },
                { separator: true },
                {
                    label: this.#dotMessageService.get('locales.delete'),
                    command: () =>
                        this.#openConfirmDialog(
                            locale,
                            defaultLocale,
                            'locale.delete.confirmation.title',
                            'locale.delete.confirmation.message',
                            'delete',
                            () => this.store.deleteLocale(locale.id)
                        )
                }
            );
        }

        this.rowMenuItems = items;
        this.contextMenu().show(event);
    }

    #openConfirmDialog(
        locale: DotLanguage,
        defaultLocale: DotLanguage,
        headerLabel: string,
        messageLabel: string,
        acceptLabel: string,
        action: () => void
    ): void {
        const dialogRef: DynamicDialogRef = this.#dialogService.open(
            DotLocaleConfirmationDialogComponent,
            {
                width: '500px',
                header: this.#dotMessageService.get(
                    headerLabel,
                    `${locale.language} (${getLocaleISOCode(locale)})`
                ),
                data: {
                    acceptLabel: this.#dotMessageService.get(acceptLabel),
                    icon: 'warning',
                    ISOCode: getLocaleISOCode(locale),
                    locale,
                    message: this.#dotMessageService.get(
                        messageLabel,
                        `${defaultLocale.language} (${getLocaleISOCode(defaultLocale)})`,
                        `${locale.language} (${getLocaleISOCode(locale)})`
                    )
                }
            }
        );

        dialogRef.onClose
            .pipe(
                take(1),
                filter((isConfirmed) => isConfirmed)
            )
            .subscribe(action);
    }
}

import { Subject } from 'rxjs';

import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';

import { debounceTime, distinctUntilChanged, take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import {
    DOT_AUTH_SYSTEM_HOST,
    DotAuthConfigPayload,
    DotAuthProtocol,
    DotAuthSiteRow,
    DotAuthStatus
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAuthListStore } from './store/dot-auth-list.store';

import { DotAuthEditComponent } from '../dot-auth-edit/dot-auth-edit.component';

interface StatusTag {
    labelKey: string;
    severity: 'success' | 'info' | 'secondary';
}

@Component({
    selector: 'dot-auth-list',
    imports: [
        FormsModule,
        TableModule,
        ButtonModule,
        TagModule,
        InputTextModule,
        IconFieldModule,
        InputIconModule,
        ConfirmDialogModule,
        SkeletonModule,
        ToolbarModule,
        DotMessagePipe
    ],
    templateUrl: './dot-auth-list.component.html',
    providers: [DotAuthListStore, DialogService, ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0' }
})
export class DotAuthListComponent {
    readonly SYSTEM_HOST = DOT_AUTH_SYSTEM_HOST;
    readonly store = inject(DotAuthListStore);

    readonly #dialogService = inject(DialogService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #destroyRef = inject(DestroyRef);

    readonly #searchSubject = new Subject<string>();

    /** Status pill for the SYSTEM_HOST row. Severity encodes protocol when configured. */
    readonly $systemStatusTag = computed<StatusTag>(() => {
        const system = this.store.system();
        if (!system.configured) {
            return { labelKey: 'dotauth.status.not-configured', severity: 'secondary' };
        }
        const severity: 'success' | 'info' = system.protocol === 'OAUTH' ? 'success' : 'info';
        return { labelKey: 'dotauth.status.configured', severity };
    });

    constructor() {
        this.#searchSubject
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.#destroyRef))
            .subscribe((value) => this.store.setFilter(value));
    }

    onSearch(value: string): void {
        this.#searchSubject.next(value);
    }

    /**
     * Status pill for a per-site row. Severity encodes protocol (OAUTH →
     * `success`, SAML → `info`); NOT_CONFIGURED is always `secondary`.
     */
    statusTag(status: DotAuthStatus, protocol: DotAuthProtocol | null): StatusTag {
        if (status === 'NOT_CONFIGURED') {
            return { labelKey: 'dotauth.status.not-configured', severity: 'secondary' };
        }
        const severity: 'success' | 'info' = protocol === 'OAUTH' ? 'success' : 'info';
        const labelKey =
            status === 'SITE_OVERRIDE'
                ? 'dotauth.status.site-override'
                : 'dotauth.status.inherited';
        return { labelKey, severity };
    }

    /** i18n key for the Protocol column cell. Returns null when nothing is configured. */
    protocolLabelKey(protocol: DotAuthProtocol | null): string | null {
        if (!protocol) {
            return null;
        }
        return protocol === 'OAUTH' ? 'dotauth.protocol.oauth' : 'dotauth.protocol.saml';
    }

    openSystemDialog(): void {
        this.#openDialog(
            this.SYSTEM_HOST,
            this.#dotMessageService.get('dotauth.dialog.header.system')
        );
    }

    openSiteDialog(row: DotAuthSiteRow): void {
        this.#openDialog(
            row.hostId,
            this.#dotMessageService.get('dotauth.dialog.header.site', row.hostName)
        );
    }

    confirmClearSystem(): void {
        this.#confirmationService.confirm({
            header: this.#dotMessageService.get('dotauth.confirm.clear.system.header'),
            message: this.#dotMessageService.get('dotauth.confirm.clear.system.message'),
            acceptLabel: this.#dotMessageService.get('dotauth.action.clear'),
            rejectLabel: this.#dotMessageService.get('Cancel'),
            acceptButtonStyleClass: 'p-button-danger',
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            position: 'center',
            accept: () => this.store.clearSite(this.SYSTEM_HOST)
        });
    }

    confirmClearSite(row: DotAuthSiteRow): void {
        this.#confirmationService.confirm({
            header: this.#dotMessageService.get('dotauth.confirm.clear.site.header'),
            message: this.#dotMessageService.get('dotauth.confirm.clear.site.message'),
            acceptLabel: this.#dotMessageService.get('dotauth.action.clear'),
            rejectLabel: this.#dotMessageService.get('Cancel'),
            acceptButtonStyleClass: 'p-button-danger',
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            position: 'center',
            accept: () => this.store.clearSite(row.hostId)
        });
    }

    #openDialog(hostId: string, header: string): void {
        const ref = this.#dialogService.open(DotAuthEditComponent, {
            header,
            // Wider than the standard 700px form dialog — SAML configs carry
            // IDP metadata XML, full PEM certs, and a key/value editor for
            // custom attributes, so the extra width pays off.
            width: 'min(96vw, 1040px)',
            data: { hostId },
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });

        ref?.onClose
            .pipe(takeUntilDestroyed(this.#destroyRef), take(1))
            .subscribe((result: DotAuthConfigPayload | undefined) => {
                if (result) {
                    this.store.saveSite(hostId, result);
                }
            });
    }
}

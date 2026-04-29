import { Subject } from 'rxjs';

import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogService } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import {
    DOT_AUTH_SYSTEM_HOST,
    DotAuthCapabilityStatus,
    DotAuthListFilter,
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

interface DotAuthListStoreCompat {
    setQuery?: (query: string) => void;
    setFilter: (filter: string) => void;
    query?: () => string;
    filter: () => string;
    sites: () => DotAuthSiteRow[];
    stats?: () => {
        sites: number;
        ssoEnabled: number;
        headlessEnabled: number;
        overrides: number;
        spas: number;
    };
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
        SelectButtonModule,
        SkeletonModule,
        ToolbarModule,
        DotMessagePipe
    ],
    templateUrl: './dot-auth-list.component.html',
    providers: [DotAuthListStore],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0' }
})
export class DotAuthListComponent {
    readonly SYSTEM_HOST = DOT_AUTH_SYSTEM_HOST;
    readonly store = inject(DotAuthListStore);

    readonly #router = inject(Router, { optional: true });
    readonly #route = inject(ActivatedRoute, { optional: true });
    readonly #dialogService = inject(DialogService, { optional: true });
    readonly #confirmationService = inject(ConfirmationService, { optional: true });
    readonly #dotMessageService = inject(DotMessageService, { optional: true });
    readonly #destroyRef = inject(DestroyRef);

    readonly #searchSubject = new Subject<string>();

    readonly filterOptions: Array<{ label: string; value: DotAuthListFilter }> = [
        { label: 'All', value: 'all' },
        { label: 'Overrides', value: 'overrides' },
        { label: 'SSO on', value: 'sso-on' },
        { label: 'Headless on', value: 'headless-on' },
        { label: 'Disabled', value: 'disabled' }
    ];

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
            .subscribe((value) => {
                const store = this.store as unknown as DotAuthListStoreCompat;
                if (store.setQuery) {
                    store.setQuery(value);
                    return;
                }
                store.setFilter(value);
            });
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

    capabilityTag(status: DotAuthCapabilityStatus): StatusTag {
        if (status === 'enabled')
            return { labelKey: 'dotauth.status.enabled', severity: 'success' };
        if (status === 'override')
            return { labelKey: 'dotauth.status.overridden', severity: 'info' };
        if (status === 'inherits')
            return { labelKey: 'dotauth.status.inherits', severity: 'secondary' };
        return { labelKey: 'dotauth.status.disabled', severity: 'secondary' };
    }

    listStats(): {
        sites: number;
        ssoEnabled: number;
        headlessEnabled: number;
        overrides: number;
        spas: number;
    } {
        const store = this.store as unknown as DotAuthListStoreCompat;
        if (store.stats) {
            return store.stats();
        }
        const rows = store.sites();
        return {
            sites: rows.length,
            ssoEnabled: rows.filter((row) => row.status !== 'NOT_CONFIGURED').length,
            headlessEnabled: 0,
            overrides: rows.filter((row) => row.status === 'SITE_OVERRIDE').length,
            spas: 0
        };
    }

    query(): string {
        const store = this.store as unknown as DotAuthListStoreCompat;
        return store.query ? store.query() : store.filter();
    }

    /** i18n key for the Protocol column cell. Returns null when nothing is configured. */
    protocolLabelKey(protocol: DotAuthProtocol | null): string | null {
        if (!protocol) {
            return null;
        }
        return protocol === 'OAUTH' ? 'dotauth.protocol.oauth' : 'dotauth.protocol.saml';
    }

    openSystemConfig(): void {
        void this.#router?.navigate(['site', this.SYSTEM_HOST], {
            relativeTo: this.#route ?? undefined
        });
    }

    openSiteConfig(hostId: string): void {
        void this.#router?.navigate(['site', hostId], { relativeTo: this.#route ?? undefined });
    }

    openSystemDialog(): void {
        if (!this.#dialogService) {
            this.openSystemConfig();
            return;
        }
        const ref = this.#dialogService.open(DotAuthEditComponent, {
            header: this.#dotMessageService?.get('dotauth.dialog.header.system') ?? '',
            width: 'min(96vw, 1040px)',
            data: { hostId: this.SYSTEM_HOST },
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });
        ref?.onClose.pipe(takeUntilDestroyed(this.#destroyRef)).subscribe((result) => {
            if (result) this.store.saveSite(this.SYSTEM_HOST, result);
        });
    }

    openSiteDialog(row: { hostId: string; hostName: string }): void {
        if (!this.#dialogService) {
            this.openSiteConfig(row.hostId);
            return;
        }
        const ref = this.#dialogService.open(DotAuthEditComponent, {
            header: this.#dotMessageService?.get('dotauth.dialog.header.site', row.hostName) ?? '',
            width: 'min(96vw, 1040px)',
            data: { hostId: row.hostId },
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });
        ref?.onClose.pipe(takeUntilDestroyed(this.#destroyRef)).subscribe((result) => {
            if (result) this.store.saveSite(row.hostId, result);
        });
    }

    confirmClearSystem(): void {
        this.#confirmationService?.confirm({
            accept: () => this.store.clearSite(this.SYSTEM_HOST),
            reject: () => undefined
        });
    }

    confirmClearSite(row: { hostId: string }): void {
        this.#confirmationService?.confirm({
            accept: () => this.store.clearSite(row.hostId),
            reject: () => undefined
        });
    }
}

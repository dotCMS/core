import { Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    computed,
    inject,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { DotAuthService, DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import {
    DOT_AUTH_SYSTEM_HOST,
    DotAuthCapabilityStatus,
    DotAuthListFilter,
    DotAuthProtocol,
    DotAuthStatus
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAuthListStore } from './store/dot-auth-list.store';

interface StatusTag {
    labelKey: string;
    severity: 'success' | 'info' | 'secondary';
}

@Component({
    selector: 'dot-auth-list',
    imports: [
        CommonModule,
        FormsModule,
        TableModule,
        ButtonModule,
        DialogModule,
        TagModule,
        InputTextModule,
        IconFieldModule,
        InputIconModule,
        SelectButtonModule,
        SkeletonModule,
        ToastModule,
        ToolbarModule,
        DotMessagePipe
    ],
    templateUrl: './dot-auth-list.component.html',
    styleUrl: './dot-auth-list.component.scss',
    providers: [DotAuthListStore, MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAuthListComponent {
    readonly SYSTEM_HOST = DOT_AUTH_SYSTEM_HOST;
    readonly store = inject(DotAuthListStore);

    readonly #router = inject(Router);
    readonly #route = inject(ActivatedRoute);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dotAuthService = inject(DotAuthService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #messages = inject(MessageService);
    readonly #destroyRef = inject(DestroyRef);

    readonly #searchSubject = new Subject<string>();
    readonly exportDialogOpen = signal(false);
    readonly importDialogOpen = signal(false);
    readonly exportPassword = signal('');
    readonly importPassword = signal('');
    readonly importFile = signal<File | null>(null);
    readonly transferBusy = signal(false);

    readonly exportPasswordValid = computed(() =>
        this.isValidExportPassword(this.exportPassword())
    );
    readonly importFormValid = computed(
        () => this.isValidExportPassword(this.importPassword()) && this.importFile() !== null
    );

    readonly filterOptions = computed(() => {
        const m = (key: string) => this.#dotMessageService.get(key);
        return [
            { label: m('dotauth.filter.all'), value: 'all' as DotAuthListFilter },
            { label: m('dotauth.filter.overrides'), value: 'overrides' as DotAuthListFilter },
            { label: m('dotauth.filter.sso-on'), value: 'sso-on' as DotAuthListFilter },
            { label: m('dotauth.filter.headless-on'), value: 'headless-on' as DotAuthListFilter },
            { label: m('dotauth.filter.disabled'), value: 'disabled' as DotAuthListFilter }
        ];
    });

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
            .subscribe((value) => this.store.setQuery(value));
    }

    onSearch(value: string): void {
        this.#searchSubject.next(value);
    }

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

    protocolLabelKey(protocol: DotAuthProtocol | null): string | null {
        if (!protocol) {
            return null;
        }
        return protocol === 'OAUTH' ? 'dotauth.protocol.oauth' : 'dotauth.protocol.saml';
    }

    protocolColor(protocol: DotAuthProtocol | null): string {
        if (protocol === 'OAUTH') return '#426BF0';
        if (protocol === 'SAML') return '#8b5cf6';
        return '#94A3B8';
    }

    openSystemConfig(): void {
        void this.#router.navigate(['site', this.SYSTEM_HOST], { relativeTo: this.#route });
    }

    openHeadlessConfig(): void {
        void this.#router.navigate(['headless'], { relativeTo: this.#route });
    }

    openSiteConfig(hostId: string): void {
        void this.#router.navigate(['site', hostId], { relativeTo: this.#route });
    }

    openExportDialog(): void {
        this.exportPassword.set('');
        this.exportDialogOpen.set(true);
    }

    openImportDialog(): void {
        this.importPassword.set('');
        this.importFile.set(null);
        this.importDialogOpen.set(true);
    }

    exportBundle(): void {
        if (!this.exportPasswordValid() || this.transferBusy()) {
            return;
        }
        this.transferBusy.set(true);
        void this.#dotAuthService.exportBundle(this.exportPassword()).then((error) => {
            this.transferBusy.set(false);
            if (error) {
                this.#messages.add({
                    severity: 'error',
                    summary: this.#dotMessageService.get('dotauth.export.error'),
                    detail: error
                });
                return;
            }
            this.exportDialogOpen.set(false);
            this.#messages.add({
                severity: 'success',
                summary: this.#dotMessageService.get('dotauth.export.success')
            });
        });
    }

    importBundle(): void {
        const file = this.importFile();
        if (!this.importFormValid() || this.transferBusy() || file === null) {
            return;
        }
        this.transferBusy.set(true);
        this.#dotAuthService
            .importBundle(this.importPassword(), file)
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe({
                next: () => {
                    this.transferBusy.set(false);
                    this.importDialogOpen.set(false);
                    this.store.loadSites();
                    this.#messages.add({
                        severity: 'success',
                        summary: this.#dotMessageService.get('dotauth.import.success')
                    });
                },
                error: (error) => {
                    this.transferBusy.set(false);
                    this.#httpErrorManager.handle(error);
                }
            });
    }

    onImportFileChange(event: Event): void {
        const input = event.target as HTMLInputElement;
        this.importFile.set(input.files?.[0] ?? null);
    }

    confirmClearSystem(): void {
        this.#confirmationService.confirm({
            accept: () => this.store.clearSite(this.SYSTEM_HOST),
            reject: () => undefined
        });
    }

    confirmClearSite(row: { hostId: string }): void {
        this.#confirmationService.confirm({
            accept: () => this.store.clearSite(row.hostId),
            reject: () => undefined
        });
    }

    private isValidExportPassword(password: string): boolean {
        return password.length >= 14 && password.length <= 32;
    }
}

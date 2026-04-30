import { Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
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
    styleUrl: './dot-auth-list.component.scss',
    providers: [DotAuthListStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAuthListComponent {
    readonly SYSTEM_HOST = DOT_AUTH_SYSTEM_HOST;
    readonly store = inject(DotAuthListStore);

    readonly #router = inject(Router);
    readonly #route = inject(ActivatedRoute);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #destroyRef = inject(DestroyRef);

    readonly #searchSubject = new Subject<string>();

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
}

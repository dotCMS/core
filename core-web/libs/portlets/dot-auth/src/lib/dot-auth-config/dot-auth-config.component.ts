import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { ToastModule } from 'primeng/toast';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

import { DotMessageService } from '@dotcms/data-access';
import { DOT_AUTH_SYSTEM_HOST, DotAuthConfig, DotAuthUiProtocol } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAuthConfigStore } from './store/dot-auth-config.store';

interface SelectOption<T> {
    label: string;
    value: T;
}

@Component({
    selector: 'dot-auth-config',
    imports: [
        CommonModule,
        FormsModule,
        RouterLink,
        ButtonModule,
        ConfirmDialogModule,
        IconFieldModule,
        InputIconModule,
        InputNumberModule,
        InputTextModule,
        PasswordModule,
        SelectButtonModule,
        TagModule,
        TextareaModule,
        ToastModule,
        ToggleSwitchModule,
        DotMessagePipe
    ],
    templateUrl: './dot-auth-config.component.html',
    providers: [DotAuthConfigStore, ConfirmationService, MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 bg-[var(--surface-ground)]' }
})
export class DotAuthConfigComponent implements OnInit {
    readonly SYSTEM_HOST = DOT_AUTH_SYSTEM_HOST;
    readonly store = inject(DotAuthConfigStore);

    readonly #route = inject(ActivatedRoute);
    readonly #router = inject(Router);
    readonly #confirm = inject(ConfirmationService);
    readonly #messages = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);

    readonly activeTab = signal<'sso' | 'headless'>('sso');
    readonly expandedIdp = signal<number | null>(0);

    readonly protocolOptions: SelectOption<DotAuthUiProtocol>[] = [
        { label: 'None', value: 'none' },
        { label: 'OIDC', value: 'oidc' },
        { label: 'SAML', value: 'saml' }
    ];

    readonly tabOptions: SelectOption<'sso' | 'headless'>[] = [
        { label: 'SSO', value: 'sso' },
        { label: 'Headless', value: 'headless' }
    ];

    readonly roleBehaviorOptions = [
        { label: 'Replace', value: 'replace' },
        { label: 'Merge with defaults', value: 'merge' },
        { label: 'Add only', value: 'add-only' },
        { label: 'On first provision only', value: 'first-only' }
    ];

    ngOnInit(): void {
        this.store.load(this.#route.snapshot.paramMap.get('hostId') ?? this.SYSTEM_HOST);
    }

    siteLabel(): string {
        return this.store.isSystem()
            ? this.#dotMessageService.get('dotauth.config.system')
            : this.store.siteId();
    }

    update(path: string, value: unknown): void {
        this.store.update(path, value);
    }

    switchProtocol(protocol: DotAuthUiProtocol): void {
        if (protocol === this.store.draft().protocol) return;
        if (!this.store.dirty()) {
            this.store.setProtocol(protocol);
            return;
        }
        this.#confirm.confirm({
            header: this.#dotMessageService.get('dotauth.confirm.switch-protocol.header'),
            message: this.#dotMessageService.get('dotauth.confirm.switch-protocol.message.ui'),
            acceptLabel: this.#dotMessageService.get('Continue'),
            rejectLabel: this.#dotMessageService.get('Cancel'),
            accept: () => this.store.setProtocol(protocol)
        });
    }

    save(): void {
        this.store.save();
        if (this.store.errorCount() === 0) {
            this.#messages.add({
                severity: 'success',
                summary: this.#dotMessageService.get('dotauth.toast.saved')
            });
        }
    }

    discard(): void {
        if (!this.store.dirty()) {
            this.store.reset();
            return;
        }
        this.#confirm.confirm({
            header: this.#dotMessageService.get('dotauth.confirm.discard.header'),
            message: this.#dotMessageService.get('dotauth.confirm.discard.message'),
            acceptLabel: this.#dotMessageService.get('dotauth.action.discard'),
            rejectLabel: this.#dotMessageService.get('Cancel'),
            accept: () => this.store.reset()
        });
    }

    back(): void {
        if (!this.store.dirty()) {
            void this.#router.navigate(['../../'], { relativeTo: this.#route });
            return;
        }
        this.#confirm.confirm({
            header: this.#dotMessageService.get('dotauth.confirm.discard.header'),
            message: this.#dotMessageService.get('dotauth.confirm.leave.message'),
            acceptLabel: this.#dotMessageService.get('dotauth.action.leave'),
            rejectLabel: this.#dotMessageService.get('Cancel'),
            accept: () => void this.#router.navigate(['../../'], { relativeTo: this.#route })
        });
    }

    runDiscovery(path: string): void {
        this.store.runOidcDiscovery(path as 'oidc' | `headless.trustedIdps.${number}`);
    }

    addGroupMapping(path: string): void {
        const current = this.valueAt(`${path}.groupMappings`) as Array<{
            idpGroup: string;
            dotcmsRole: string;
        }>;
        this.store.update(`${path}.groupMappings`, [...current, { idpGroup: '', dotcmsRole: '' }]);
    }

    removeGroupMapping(path: string, index: number): void {
        const current = [
            ...(this.valueAt(`${path}.groupMappings`) as Array<{
                idpGroup: string;
                dotcmsRole: string;
            }>)
        ];
        current.splice(index, 1);
        this.store.update(`${path}.groupMappings`, current);
    }

    defaultRoles(path: string): string {
        return ((this.valueAt(`${path}.defaultRoles`) as string[]) ?? []).join(', ');
    }

    updateDefaultRoles(path: string, value: string): void {
        this.store.update(
            `${path}.defaultRoles`,
            value
                .split(',')
                .map((role) => role.trim())
                .filter(Boolean)
        );
    }

    addAlg(index: number, value: string): void {
        const idp = this.store.draft().headless.trustedIdps[index];
        this.store.update(
            `headless.trustedIdps.${index}.algs`,
            value
                .split(',')
                .map((alg) => alg.trim())
                .filter(Boolean)
        );
        if (!idp.enabled) {
            this.store.update(`headless.trustedIdps.${index}.enabled`, true);
        }
    }

    confirmRevoke(): void {
        this.#confirm.confirm({
            header: this.#dotMessageService.get('dotauth.confirm.revoke.header'),
            message: this.#dotMessageService.get('dotauth.confirm.revoke.message'),
            acceptLabel: this.#dotMessageService.get('dotauth.action.revoke-all'),
            rejectLabel: this.#dotMessageService.get('Cancel'),
            acceptButtonStyleClass: 'p-button-danger',
            accept: () => this.store.revokeAllSessionRefs()
        });
    }

    error(path: string): string | null {
        return this.store.errors()[path] ?? null;
    }

    valueAt(path: string): unknown {
        return path.split('.').reduce<unknown>((cursor, part) => {
            if (cursor == null) return undefined;
            return Array.isArray(cursor)
                ? cursor[Number(part)]
                : (cursor as Record<string, unknown>)[part];
        }, this.store.draft() as DotAuthConfig);
    }
}

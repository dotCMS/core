import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    OnInit,
    computed,
    effect,
    inject,
    signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { ToastModule } from 'primeng/toast';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import {
    DOT_AUTH_HIDDEN_SECRET_MASK,
    DOT_AUTH_SYSTEM_HOST,
    DotAuthConfig,
    DotAuthUiProtocol
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAuthConfigStore } from './store/dot-auth-config.store';

interface TocSection {
    id: string;
    label: string;
}

interface RoleBehaviorOption {
    label: string;
    value: string;
    description: string;
}

@Component({
    selector: 'dot-auth-config',
    imports: [
        CommonModule,
        FormsModule,
        RouterLink,
        ButtonModule,
        ConfirmDialogModule,
        DialogModule,
        IconFieldModule,
        InputIconModule,
        InputNumberModule,
        InputTextModule,
        MessageModule,
        SelectModule,
        SelectButtonModule,
        TagModule,
        TextareaModule,
        ToastModule,
        ToggleSwitchModule,
        TooltipModule,
        DotMessagePipe
    ],
    templateUrl: './dot-auth-config.component.html',
    styleUrl: './dot-auth-config.component.scss',
    providers: [DotAuthConfigStore, ConfirmationService, MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush
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
    readonly activeSection = signal<string>('sso-protocol');
    readonly expandedIdp = signal<number | null>(0);
    readonly showAdvancedOidc = signal(false);
    readonly isOverriding = signal(false);
    readonly showClearDialog = signal(false);
    readonly clearConfirmText = signal('');

    readonly responseTypeOptions = [
        { value: 'code', label: 'code (Authorization Code Flow)' },
        { value: 'id_token', label: 'id_token (Implicit)' },
        { value: 'code id_token', label: 'code id_token (Hybrid)' }
    ];

    readonly roleBehaviorOptions: RoleBehaviorOption[] = [
        {
            label: 'Replace',
            value: 'replace',
            description: "Set the user's roles to exactly the mapped roles + defaults each login."
        },
        {
            label: 'Merge with defaults',
            value: 'merge',
            description:
                'Add mapped roles + defaults; remove other IdP-derived roles no longer present.'
        },
        {
            label: 'Add only',
            value: 'add-only',
            description: 'Add mapped roles to the user; never remove roles.'
        },
        {
            label: 'On first provision only',
            value: 'first-only',
            description:
                'Apply mapped roles when the user is first created; never modify roles on subsequent logins.'
        }
    ];

    readonly protocolOptions = [
        { label: 'None', value: 'none' },
        { label: 'OIDC', value: 'oidc' },
        { label: 'SAML', value: 'saml' }
    ];

    readonly tocSections = computed<TocSection[]>(() => {
        if (this.activeTab() === 'sso') {
            const ssoTrack: TocSection[] = [{ id: 'sso-protocol', label: 'Protocol' }];
            const protocol = this.store.draft().protocol;
            if (protocol === 'oidc') {
                ssoTrack.push(
                    { id: 'connection', label: 'Connection' },
                    { id: 'claims', label: 'Claim mapping' },
                    { id: 'provisioning', label: 'Provisioning' },
                    { id: 'session', label: 'Session & logout' }
                );
            } else if (protocol === 'saml') {
                ssoTrack.push(
                    { id: 'connection', label: 'Service Provider' },
                    { id: 'idp', label: 'Identity Provider' },
                    { id: 'signing', label: 'Signing & encryption' },
                    { id: 'claims', label: 'Attribute mapping' },
                    { id: 'provisioning', label: 'Provisioning' },
                    { id: 'session', label: 'Session & logout' }
                );
            }
            return ssoTrack;
        }
        return [
            { id: 'headless-overview', label: 'Enable & flow' },
            { id: 'headless-tokens', label: 'sessionRef behavior' },
            { id: 'headless-idps', label: 'Trusted IdPs' },
            { id: 'headless-origins', label: 'Allowed origins' },
            { id: 'headless-tokens-active', label: 'Emergency controls' }
        ];
    });

    readonly overrideCount = computed(() => this.ssoOverrideCount() + this.headlessOverrideCount());

    readonly ssoOverrideCount = computed(() => {
        if (this.store.isSystem()) return 0;
        return this.store.dirty() ? 1 : 0;
    });

    readonly headlessOverrideCount = computed(() => {
        if (this.store.isSystem()) return 0;
        return 0;
    });

    constructor() {
        effect(() => {
            if (this.store.status() === 'loaded' && !this.store.isSystem()) {
                this.isOverriding.set(!this.store.inherited());
            }
        });
    }

    ngOnInit(): void {
        this.store.load(this.#route.snapshot.paramMap.get('hostId') ?? this.SYSTEM_HOST);
    }

    siteLabel(): string {
        return this.store.isSystem()
            ? this.#dotMessageService.get('dotauth.config.system')
            : this.store.siteId();
    }

    scrollToSection(id: string): void {
        this.activeSection.set(id);
        const el = document.getElementById(id);
        if (el) {
            el.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
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
        this.store.update(
            `headless.trustedIdps.${index}.algs`,
            value
                .split(',')
                .map((alg) => alg.trim())
                .filter(Boolean)
        );
    }

    resetToInherit(): void {
        this.#confirm.confirm({
            header: this.#dotMessageService.get('dotauth.confirm.reset.header'),
            message: this.#dotMessageService.get('dotauth.confirm.reset.message'),
            acceptLabel: this.#dotMessageService.get('dotauth.action.reset'),
            rejectLabel: this.#dotMessageService.get('Cancel'),
            accept: () => this.store.reset()
        });
    }

    clearConfig(): void {
        this.showClearDialog.set(false);
        this.clearConfirmText.set('');
        this.store.clearOverride();
        this.#messages.add({
            severity: 'success',
            summary: 'Configuration cleared'
        });
    }

    isSecretStored(): boolean {
        const secret = this.store.draft().oidc.clientSecret;
        return secret === '****' || secret === DOT_AUTH_HIDDEN_SECRET_MASK;
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

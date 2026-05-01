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
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DOT_AUTH_SYSTEM_HOST, DotAuthUiProtocol } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import {
    DotAuthOidcConnectionComponent,
    DotAuthProvisioningComponent,
    DotAuthSamlConfigComponent,
    OidcConnectionChange,
    ProvisioningChange,
    SamlConfigChange
} from './components';
import { DotAuthConfigStore } from './store/dot-auth-config.store';

interface TocSection {
    id: string;
    label: string;
}

@Component({
    selector: 'dot-auth-config',
    imports: [
        FormsModule,
        ButtonModule,
        ConfirmDialogModule,
        DialogModule,
        InputTextModule,
        MessageModule,
        TagModule,
        ToastModule,
        ToggleSwitchModule,
        TooltipModule,
        DotMessagePipe,
        DotAuthOidcConnectionComponent,
        DotAuthSamlConfigComponent,
        DotAuthProvisioningComponent
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

    readonly activeSection = signal<string>('sso-protocol');
    readonly isOverriding = signal(false);
    readonly showClearDialog = signal(false);
    readonly clearConfirmText = signal('');

    readonly protocolOptions = [
        { label: 'None', value: 'none' },
        { label: 'OIDC', value: 'oidc' },
        { label: 'SAML', value: 'saml' }
    ];

    readonly tocSections = computed<TocSection[]>(() => {
        const sections: TocSection[] = [{ id: 'sso-protocol', label: 'Protocol' }];
        const protocol = this.store.draft().protocol;
        if (protocol === 'oidc') {
            sections.push(
                { id: 'login-behavior', label: 'Login behavior' },
                { id: 'connection', label: 'Connection' },
                { id: 'claims', label: 'Claim mapping' },
                { id: 'provisioning', label: 'Provisioning' },
                { id: 'session', label: 'Session & logout' }
            );
        } else if (protocol === 'saml') {
            sections.push(
                { id: 'login-behavior', label: 'Login behavior' },
                { id: 'connection', label: 'Service Provider' },
                { id: 'idp', label: 'Identity Provider' },
                { id: 'signing', label: 'Signing & encryption' },
                { id: 'claims', label: 'Attribute mapping' },
                { id: 'provisioning', label: 'Provisioning' },
                { id: 'session', label: 'Session & logout' },
                { id: 'extra-properties', label: 'Additional properties' }
            );
        }
        return sections;
    });

    readonly overrideCount = computed(() => {
        if (this.store.isSystem()) return 0;
        return this.store.ssoDirty() ? 1 : 0;
    });

    readonly #pendingSaveToast = signal(false);

    constructor() {
        effect(() => {
            if (this.store.status() === 'loaded' && !this.store.isSystem()) {
                this.isOverriding.set(!this.store.inherited());
            }
        });

        effect(() => {
            if (this.store.status() === 'loaded' && this.#pendingSaveToast()) {
                this.#pendingSaveToast.set(false);
                this.#messages.add({
                    severity: 'success',
                    summary: this.#dotMessageService.get('dotauth.toast.saved')
                });
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
        document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
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
        const started = this.store.saveSso();
        if (started) {
            this.#pendingSaveToast.set(true);
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

    clearConfig(): void {
        this.showClearDialog.set(false);
        this.clearConfirmText.set('');
        this.store.clearOverride();
        this.#messages.add({ severity: 'success', summary: 'Configuration cleared' });
    }

    // --- Child component event handlers ---

    onOidcChange(event: OidcConnectionChange): void {
        this.store.update(event.path, event.value);
    }

    onSamlChange(event: SamlConfigChange): void {
        this.store.update(event.path, event.value);
    }

    onFetchSamlMetadata(): void {
        this.#messages.add({
            severity: 'info',
            summary: this.#dotMessageService.get('dotauth.saml.metadata-fetch.not-implemented')
        });
    }

    onProvisioningChange(prefix: string, event: ProvisioningChange): void {
        this.store.update(`${prefix}.${event.path}`, event.value);
    }

    onDiscoverOidc(): void {
        this.store.runOidcDiscovery('oidc');
    }
}

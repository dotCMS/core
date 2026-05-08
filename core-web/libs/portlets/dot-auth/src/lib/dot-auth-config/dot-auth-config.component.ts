import {
    ChangeDetectionStrategy,
    Component,
    OnInit,
    computed,
    effect,
    inject,
    signal,
    viewChild
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

    readonly samlConfig = viewChild<DotAuthSamlConfigComponent>('samlConfig');
    readonly activeSection = signal<string>('sso-protocol');
    readonly isOverriding = signal(false);
    readonly showClearDialog = signal(false);
    readonly clearConfirmText = signal('');

    readonly tocSections = computed<TocSection[]>(() => {
        const m = (key: string) => this.#dotMessageService.get(key);
        const sections: TocSection[] = [{ id: 'sso-protocol', label: m('dotauth.toc.protocol') }];
        const protocol = this.store.draft().protocol;
        if (protocol === 'oidc') {
            sections.push(
                { id: 'login-behavior', label: m('dotauth.toc.login-behavior') },
                { id: 'connection', label: m('dotauth.toc.connection') },
                { id: 'claims', label: m('dotauth.toc.claim-mapping') },
                { id: 'provisioning', label: m('dotauth.toc.provisioning') },
                { id: 'session', label: m('dotauth.toc.session') }
            );
        } else if (protocol === 'saml') {
            sections.push(
                { id: 'login-behavior', label: m('dotauth.toc.login-behavior') },
                { id: 'connection', label: m('dotauth.toc.service-provider') },
                { id: 'idp', label: m('dotauth.toc.identity-provider') },
                { id: 'signing', label: m('dotauth.toc.signing') },
                { id: 'claims', label: m('dotauth.toc.attribute-mapping') },
                { id: 'provisioning', label: m('dotauth.toc.provisioning') },
                { id: 'session', label: m('dotauth.toc.session') },
                { id: 'extra-properties', label: m('dotauth.toc.extra-properties') }
            );
        }
        return sections;
    });

    readonly overrideCount = computed(() => {
        if (this.store.isSystem()) return 0;
        return this.store.ssoDirty() ? 1 : 0;
    });

    readonly #pendingSaveToast = signal(false);
    readonly #pendingClearToast = signal(false);

    constructor() {
        effect(() => {
            if (this.store.status() === 'loaded' && !this.store.isSystem()) {
                this.isOverriding.set(!this.store.inherited());
            }
        });

        effect(() => {
            if (this.store.status() === 'error' && this.#pendingSaveToast()) {
                this.#pendingSaveToast.set(false);
                return;
            }
            if (this.store.status() === 'loaded' && this.#pendingSaveToast()) {
                this.#pendingSaveToast.set(false);
                this.#messages.add({
                    severity: 'success',
                    summary: this.#dotMessageService.get('dotauth.toast.saved')
                });
            }
        });

        effect(() => {
            if (this.store.status() === 'error' && this.#pendingClearToast()) {
                this.#pendingClearToast.set(false);
                return;
            }
            if (this.store.status() === 'loaded' && this.#pendingClearToast()) {
                this.#pendingClearToast.set(false);
                this.#messages.add({
                    severity: 'success',
                    summary: this.#dotMessageService.get('dotauth.toast.config-cleared')
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

    back(): void {
        if (!this.store.dirty()) {
            void this.#router.navigate(['../'], { relativeTo: this.#route });
            return;
        }
        this.#confirm.confirm({
            header: this.#dotMessageService.get('dotauth.confirm.discard.header'),
            message: this.#dotMessageService.get('dotauth.confirm.leave.message'),
            acceptLabel: this.#dotMessageService.get('dotauth.action.leave'),
            rejectLabel: this.#dotMessageService.get('Cancel'),
            accept: () => void this.#router.navigate(['../'], { relativeTo: this.#route })
        });
    }

    clearConfig(): void {
        this.showClearDialog.set(false);
        this.clearConfirmText.set('');
        this.#pendingClearToast.set(true);
        this.store.clearOverride();
    }

    // --- Child component event handlers ---

    onOidcChange(event: OidcConnectionChange): void {
        this.store.update(event.path, event.value);
    }

    onSamlChange(event: SamlConfigChange): void {
        this.store.update(event.path, event.value);
    }

    onFetchSamlMetadata(url: string): void {
        this.store.fetchSamlMetadata(url);
        this.samlConfig()?.openMetadataPanel();
    }

    onProvisioningChange(prefix: string, event: ProvisioningChange): void {
        this.store.update(`${prefix}.${event.path}`, event.value);
    }

    onDiscoverOidc(): void {
        this.store.runOidcDiscovery('oidc');
    }
}

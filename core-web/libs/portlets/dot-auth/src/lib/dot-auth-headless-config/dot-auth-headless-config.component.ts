import {
    ChangeDetectionStrategy,
    Component,
    OnInit,
    computed,
    effect,
    inject,
    signal
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';

import { DotMessageService } from '@dotcms/data-access';
import { DOT_AUTH_SYSTEM_HOST } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAuthHeadlessSectionComponent, HeadlessChange } from '../dot-auth-config/components';
import { DotAuthConfigStore } from '../dot-auth-config/store/dot-auth-config.store';

interface TocSection {
    id: string;
    label: string;
}

@Component({
    selector: 'dot-auth-headless-config',
    imports: [
        ButtonModule,
        ConfirmDialogModule,
        TagModule,
        ToastModule,
        DotMessagePipe,
        DotAuthHeadlessSectionComponent
    ],
    templateUrl: './dot-auth-headless-config.component.html',
    styleUrl: './dot-auth-headless-config.component.scss',
    providers: [DotAuthConfigStore, ConfirmationService, MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAuthHeadlessConfigComponent implements OnInit {
    readonly store = inject(DotAuthConfigStore);

    readonly #router = inject(Router);
    readonly #route = inject(ActivatedRoute);
    readonly #confirm = inject(ConfirmationService);
    readonly #messages = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);

    readonly activeSection = signal<string>('headless-overview');

    readonly tocSections = computed<TocSection[]>(() => [
        { id: 'headless-overview', label: 'Enable & flow' },
        { id: 'headless-tokens', label: 'SessionRef lifetime' },
        { id: 'headless-idps', label: 'Trusted IdPs' },
        { id: 'headless-origins', label: 'Allowed origins' },
        { id: 'headless-tokens-active', label: 'Emergency controls' }
    ]);

    readonly #pendingSaveToast = signal(false);

    constructor() {
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
        this.store.load(DOT_AUTH_SYSTEM_HOST);
    }

    scrollToSection(id: string): void {
        this.activeSection.set(id);
        document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    save(): void {
        const started = this.store.saveHeadless();
        if (started) {
            this.#pendingSaveToast.set(true);
        }
    }

    discard(): void {
        if (!this.store.headlessDirty()) {
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
        if (!this.store.headlessDirty()) {
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

    onHeadlessChange(event: HeadlessChange): void {
        this.store.update(event.path, event.value);
    }

    onDiscoverIdp(index: number): void {
        this.store.runOidcDiscovery(`headless.trustedIdps.${index}`);
    }
}

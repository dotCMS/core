import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import {
    DotUsersRequestTokenDialogComponent,
    DotUsersRequestTokenPayload
} from './dot-users-request-token-dialog.component';

export interface DotUsersApiToken {
    id: string;
    label: string;
    issued: string;
    expires: string;
    requestedBy: string;
    network: string;
    active: boolean;
}

const MOCK_TOKENS: DotUsersApiToken[] = [
    {
        id: 'api9a8b7c6d…',
        label: 'CI/CD pipeline',
        issued: '2026-01-14T10:32:11Z',
        expires: '2029-01-14T10:32:11Z',
        requestedBy: 'dotcms.admin',
        network: '0.0.0.0/0',
        active: true
    }
];

/**
 * API Tokens tab. Visuals-first: renders a table of the user's tokens
 * with a "Show inactive" filter and a Request New Token action that
 * opens a small sub-dialog. State is local and hydrated from a mock
 * seed; the real wiring will exchange the seed for the tokens
 * endpoint response.
 */
@Component({
    selector: 'dot-users-api-tokens-tab',
    standalone: true,
    imports: [
        DatePipe,
        FormsModule,
        ButtonModule,
        CheckboxModule,
        TableModule,
        TagModule,
        DotMessagePipe
    ],
    templateUrl: './dot-users-api-tokens-tab.component.html',
    styleUrl: './dot-users-api-tokens-tab.component.scss',
    providers: [DialogService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col gap-4 block' }
})
export class DotUsersApiTokensTabComponent {
    private readonly dialogService = inject(DialogService);
    private readonly messageService = inject(DotMessageService);

    protected readonly tokens = signal<DotUsersApiToken[]>(MOCK_TOKENS);
    protected readonly showInactive = signal(false);

    protected readonly visibleTokens = computed(() => {
        const includeInactive = this.showInactive();

        return this.tokens().filter((token) => includeInactive || token.active);
    });

    protected onShowInactiveChange(value: boolean): void {
        this.showInactive.set(value);
    }

    protected openRequestDialog(): void {
        const ref: DynamicDialogRef = this.dialogService.open(DotUsersRequestTokenDialogComponent, {
            header: this.messageService.get('users.dialog.tokens.request.header'),
            width: '620px',
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });

        ref.onClose.pipe(take(1)).subscribe((payload: DotUsersRequestTokenPayload | undefined) => {
            if (!payload) {
                return;
            }
            const now = new Date().toISOString();
            const generatedId = `api${Math.random().toString(16).slice(2, 10)}…`;
            this.tokens.update((current) => [
                {
                    id: generatedId,
                    label: payload.label || '—',
                    issued: now,
                    expires: payload.expires,
                    requestedBy: payload.requestedBy,
                    network: payload.network || '0.0.0.0/0',
                    active: true
                },
                ...current
            ]);
        });
    }

    protected revoke(token: DotUsersApiToken): void {
        this.tokens.update((current) =>
            current.map((row) => (row.id === token.id ? { ...row, active: false } : row))
        );
    }
}

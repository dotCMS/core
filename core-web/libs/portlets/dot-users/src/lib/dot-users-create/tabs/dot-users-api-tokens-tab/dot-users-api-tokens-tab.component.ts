import { DatePipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    computed,
    effect,
    inject,
    input,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';

import { take } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotUsersRequestTokenDialogComponent } from './dot-users-request-token-dialog.component';

import {
    DotApiToken,
    DotApiTokenCreateResult,
    DotUsersService
} from '../../../services/dot-users.service';

/**
 * Row state derived from the three backend flags. Drives the row tag
 * and which action button is available — valid tokens can be revoked
 * and reveal their JWT, revoked/expired ones can only be deleted.
 */
type TokenStatus = 'valid' | 'revoked' | 'expired';

/**
 * API Tokens tab. Matches the legacy admin's contract:
 *
 * - Clicking a valid row (or Reveal button) fetches a FRESH JWT via
 *   `GET /api/v1/apitoken/{id}/jwt`. Each call mints a new signed
 *   value on the backend — the token record is unchanged, so there
 *   is no "reveal only once" limitation to worry about.
 * - Revoke (soft) keeps the row visible for audit; Delete purges it,
 *   and the button is only present for revoked/expired rows.
 * - The Show revoked toggle mirrors the legacy checkbox.
 */
@Component({
    selector: 'dot-users-api-tokens-tab',
    standalone: true,
    imports: [
        DatePipe,
        FormsModule,
        ButtonModule,
        CheckboxModule,
        ConfirmDialogModule,
        DialogModule,
        InputTextModule,
        TableModule,
        DotMessagePipe
    ],
    templateUrl: './dot-users-api-tokens-tab.component.html',
    styleUrl: './dot-users-api-tokens-tab.component.scss',
    providers: [DialogService, ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col gap-4 block' }
})
export class DotUsersApiTokensTabComponent {
    private readonly dialogService = inject(DialogService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly messageService = inject(DotMessageService);
    private readonly usersService = inject(DotUsersService);
    private readonly httpErrorManager = inject(DotHttpErrorManagerService);
    private readonly destroyRef = inject(DestroyRef);

    /**
     * User whose tokens we're managing. Missing on create mode (the
     * user doesn't exist yet); we hide the whole tab body when there's
     * no id to key requests to.
     */
    readonly userId = input<string | null>(null);

    protected readonly tokens = signal<DotApiToken[]>([]);
    protected readonly showRevoked = signal(false);
    protected readonly isLoading = signal(false);

    /**
     * The reveal dialog's state. `jwt === null` means "still fetching",
     * a string means "ready to copy". Populated on-demand via row
     * click or after a create — the backend re-mints on every call.
     */
    protected readonly revealVisible = signal(false);
    protected readonly revealJwt = signal<string | null>(null);
    protected readonly revealTokenId = signal<string>('');
    protected readonly copied = signal(false);

    protected readonly hasUser = computed(() => !!this.userId());

    constructor() {
        effect(() => {
            const id = this.userId();
            const showRevoked = this.showRevoked();
            if (!id) {
                this.tokens.set([]);

                return;
            }

            this.loadTokens(id, showRevoked);
        });
    }

    protected onShowRevokedChange(value: boolean): void {
        this.showRevoked.set(value);
    }

    /**
     * Buckets each row into `valid` / `revoked` / `expired`. Revoked
     * takes precedence over expired to keep the action tree
     * consistent for a token that's both.
     */
    protected statusOf(token: DotApiToken): TokenStatus {
        if (token.revoked) {
            return 'revoked';
        }
        if (token.expired) {
            return 'expired';
        }

        return 'valid';
    }

    protected shortId(id: string): string {
        const dash = id.indexOf('-');

        return `${dash > 0 ? id.slice(0, dash) : id.slice(0, 8)}…`;
    }

    /**
     * Opens the reveal dialog for a token id and fetches a fresh JWT.
     * Backend refuses revoked/expired tokens with a 400 — we gate the
     * caller (`onRowClick`, template button) to valid rows only so
     * this never fires for those.
     */
    protected reveal(tokenId: string): void {
        this.revealTokenId.set(tokenId);
        this.revealJwt.set(null);
        this.copied.set(false);
        this.revealVisible.set(true);

        this.usersService
            .getApiTokenJwt(tokenId)
            .pipe(take(1), takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (jwt) => this.revealJwt.set(jwt),
                error: (error) => {
                    this.revealVisible.set(false);
                    this.httpErrorManager.handle(error);
                }
            });
    }

    protected onRowClick(token: DotApiToken): void {
        if (this.statusOf(token) !== 'valid') {
            return;
        }

        this.reveal(token.id);
    }

    protected closeReveal(): void {
        this.revealVisible.set(false);
        this.revealJwt.set(null);
        this.revealTokenId.set('');
        this.copied.set(false);
    }

    protected async copyJwt(): Promise<void> {
        const jwt = this.revealJwt();
        if (!jwt) {
            return;
        }

        try {
            await navigator.clipboard.writeText(jwt);
            this.copied.set(true);
        } catch {
            // Clipboard API can be blocked (insecure context, permission
            // policy). Fall back to selecting the field so the admin can
            // Cmd+C manually.
            const el = document.querySelector<HTMLInputElement>(
                '[data-testid="users-api-tokens-reveal-input"]'
            );
            el?.select();
        }
    }

    protected openRequestDialog(): void {
        const id = this.userId();
        if (!id) {
            return;
        }

        const ref: DynamicDialogRef = this.dialogService.open(DotUsersRequestTokenDialogComponent, {
            header: this.messageService.get('users.dialog.tokens.request.header'),
            width: '500px',
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center',
            data: { userId: id }
        });

        ref.onClose.pipe(take(1)).subscribe((result: DotApiTokenCreateResult | undefined) => {
            if (!result) {
                return;
            }

            // Prepend so the new row is visible even before the refetch
            // resolves — the follow-up refetch replaces the whole list
            // with server-authoritative data.
            this.tokens.update((current) => [result.token, ...current]);
            this.reloadTokens();

            // Show the JWT the create call already returned; skips one
            // round-trip and keeps a single reveal-dialog surface.
            this.revealTokenId.set(result.token.id);
            this.revealJwt.set(result.jwt);
            this.copied.set(false);
            this.revealVisible.set(true);
        });
    }

    protected revoke(token: DotApiToken, event: MouseEvent): void {
        event.stopPropagation();
        this.confirmationService.confirm({
            header: this.messageService.get('users.dialog.tokens.revoke.confirm.header'),
            message: this.messageService.get('users.dialog.tokens.revoke.confirm.message'),
            acceptLabel: this.messageService.get('users.dialog.tokens.revoke'),
            rejectLabel: this.messageService.get('users.cancel'),
            acceptButtonProps: { severity: 'danger' },
            rejectButtonProps: { severity: 'secondary', text: true },
            accept: () => {
                this.usersService
                    .revokeApiToken(token.id)
                    .pipe(take(1), takeUntilDestroyed(this.destroyRef))
                    .subscribe({
                        next: () => this.reloadTokens(),
                        error: (error) => this.httpErrorManager.handle(error)
                    });
            }
        });
    }

    private reloadTokens(): void {
        const id = this.userId();
        if (!id) {
            return;
        }

        this.loadTokens(id, this.showRevoked());
    }

    private loadTokens(userId: string, showRevoked: boolean): void {
        this.isLoading.set(true);
        this.usersService
            .getApiTokens(userId, showRevoked)
            .pipe(take(1), takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (tokens) => {
                    this.tokens.set(tokens);
                    this.isLoading.set(false);
                },
                error: (error) => {
                    this.isLoading.set(false);
                    this.httpErrorManager.handle(error);
                }
            });
    }
}

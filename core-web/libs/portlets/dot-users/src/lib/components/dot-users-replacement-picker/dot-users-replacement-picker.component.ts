import { of, Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    inject,
    input,
    output,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { AutoCompleteCompleteEvent, AutoCompleteModule } from 'primeng/autocomplete';

import { catchError, switchMap, tap } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { DotUserListItem, DotUsersService } from '../../services/dot-users.service';

/**
 * Server-backed replacement-user picker used by the delete flows.
 * Reused by the single-user delete confirm (inside the profile
 * dialog) and the bulk-delete confirm (on the list toolbar).
 *
 * The excluded-ids input keeps deletion targets out of the
 * suggestion list on the client — the backend also rejects invalid
 * replacements, but pre-filtering avoids showing picks that would
 * fail on submit.
 */
@Component({
    selector: 'dot-users-replacement-picker',
    imports: [FormsModule, AutoCompleteModule, DotMessagePipe],
    templateUrl: './dot-users-replacement-picker.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'block' }
})
export class DotUsersReplacementPickerComponent {
    readonly #usersService = inject(DotUsersService);
    readonly #destroyRef = inject(DestroyRef);

    /** ID passed to the underlying <input> so an external <label for> hooks up. */
    readonly inputId = input<string>('users-replacement-picker');

    /** i18n key for the input placeholder. */
    readonly placeholderKey = input<string>('users.dialog.delete-confirm.replacement-placeholder');

    /** User IDs that must not appear as replacement candidates. */
    readonly excludedUserIds = input<string[]>([]);

    /** Currently selected user, or null when the picker is empty. */
    readonly value = input<DotUserListItem | null>(null);

    /**
     * When true, the underlying p-autoComplete renders in its error
     * state (red outline). Field-level error text is the caller's
     * responsibility so this component stays reusable.
     */
    readonly invalid = input<boolean>(false);

    /** Emits every selection change (user or null when cleared). */
    readonly selectionChange = output<DotUserListItem | null>();

    protected readonly $suggestions = signal<DotUserListItem[]>([]);
    protected readonly $isLoading = signal(false);
    /**
     * Distinct from "no matches" so a 500 doesn't look like an empty
     * result set. The empty-template reads this to swap the copy.
     */
    protected readonly $hasError = signal(false);

    /**
     * Search queries flow through a Subject so `switchMap` can cancel
     * an in-flight request the moment a newer query arrives. The 300ms
     * PrimeNG delay narrows the race but a slow early response can
     * still overwrite a newer one if we subscribe per keystroke.
     */
    readonly #query$ = new Subject<string>();

    constructor() {
        this.#query$
            .pipe(
                tap(() => {
                    this.$isLoading.set(true);
                    this.$hasError.set(false);
                }),
                switchMap((query) =>
                    this.#usersService
                        .getUsersPaginated({ filter: query, page: 1, perPage: 10 })
                        .pipe(catchError(() => of(null)))
                ),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((response) => {
                this.$isLoading.set(false);
                if (response === null) {
                    this.$hasError.set(true);
                    this.$suggestions.set([]);

                    return;
                }
                const excluded = new Set(this.excludedUserIds());
                this.$suggestions.set(response.entity.filter((user) => !excluded.has(user.userId)));
            });
    }

    protected onSearch(event: AutoCompleteCompleteEvent): void {
        this.#query$.next(event.query);
    }

    protected onSelect(value: DotUserListItem | null): void {
        this.selectionChange.emit(value);
    }

    /**
     * Builds the display string shown inside the input and in each
     * suggestion row. `fullName` is populated for most accounts but can
     * be blank for legacy or partially-imported users, so we fall back
     * to `name`, then to the concatenated first/last, then to the email
     * to guarantee the row is never rendered as `[object Object]`.
     */
    protected displayName(user: DotUserListItem): string {
        const fullName = (user.fullName ?? '').trim();
        if (fullName) {
            return fullName;
        }

        const name = (user.name ?? '').trim();
        if (name) {
            return name;
        }

        const first = (user.firstName ?? '').trim();
        const last = (user.lastName ?? '').trim();
        const combined = `${first} ${last}`.trim();
        if (combined) {
            return combined;
        }

        return user.emailAddress ?? '';
    }
}

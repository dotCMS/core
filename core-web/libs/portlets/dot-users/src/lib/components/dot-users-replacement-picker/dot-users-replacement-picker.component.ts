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

import { take } from 'rxjs/operators';

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

    protected onSearch(event: AutoCompleteCompleteEvent): void {
        this.#usersService
            .getUsersPaginated({
                filter: event.query,
                page: 1,
                perPage: 10
            })
            .pipe(take(1), takeUntilDestroyed(this.#destroyRef))
            .subscribe({
                next: (response) => {
                    const excluded = new Set(this.excludedUserIds());
                    this.$suggestions.set(
                        response.entity.filter((user) => !excluded.has(user.userId))
                    );
                },
                error: () => this.$suggestions.set([])
            });
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

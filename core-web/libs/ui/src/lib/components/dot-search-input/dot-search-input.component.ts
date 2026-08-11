import { timer } from 'rxjs';

import { ChangeDetectionStrategy, Component, effect, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';

import { debounce, tap } from 'rxjs/operators';

import { DEFAULT_SEARCH_DEBOUNCE } from './constants';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

/**
 * Presentational debounced search box shared across Content Drive and AssetPicker.
 *
 * The host owns the value: it flows in through `value` and back out through `search` after the
 * debounce window. Pushing a new `value` (URL restore, "clear all") re-syncs the control without
 * echoing an emission back to the host.
 */
@Component({
    selector: 'dot-search-input',
    templateUrl: './dot-search-input.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [IconField, InputIcon, InputTextModule, ReactiveFormsModule, DotMessagePipe],
    host: { class: 'block w-full' }
})
export class DotSearchInputComponent {
    /**
     * Current search term, owned by the host.
     * @alias value
     */
    readonly $value = input('', { alias: 'value' });

    /**
     * i18n key for the input placeholder.
     * @alias placeholder
     */
    readonly $placeholder = input('search', { alias: 'placeholder' });

    /**
     * Debounce (ms) applied before emitting. Read on every keystroke, so it can change at runtime.
     * @alias debounceTime
     */
    readonly $debounceTime = input(DEFAULT_SEARCH_DEBOUNCE, { alias: 'debounceTime' });

    /**
     * `data-testId` for the input, so a host that renders more than one search box can tell them
     * apart — the AssetPicker has both an asset search and a sites-and-folders search on screen at
     * once, and a single hardcoded id made every selector ambiguous.
     *
     * Defaults to the original value, so existing consumers and their tests are unaffected.
     *
     * @alias testId
     */
    readonly $testId = input('search-input-field', { alias: 'testId' });

    /** Emits the trimmed term once the debounce window closes. */
    readonly search = output<string>();

    protected readonly searchControl = new FormControl('');

    /** Mirrors what the user sees, so the clear icon reacts immediately instead of after the debounce. */
    protected readonly $text = signal('');

    /**
     * Last value handed to the host (or received from it). Guards against re-emitting a term the
     * host already knows — e.g. typing a trailing space, which trims back to the same term.
     */
    #lastEmitted = '';

    constructor() {
        effect(() => {
            const value = this.$value();

            if (value === this.searchControl.value) {
                return;
            }

            this.searchControl.setValue(value, { emitEvent: false });
            this.$text.set(value);
            this.#lastEmitted = value;
        });

        this.searchControl.valueChanges
            .pipe(
                tap((value) => this.$text.set(value ?? '')),
                debounce(() => timer(this.$debounceTime())),
                takeUntilDestroyed()
            )
            .subscribe((value) => {
                const term = value?.trim() ?? '';

                if (term === this.#lastEmitted) {
                    return;
                }

                this.#lastEmitted = term;
                this.search.emit(term);
            });
    }
}

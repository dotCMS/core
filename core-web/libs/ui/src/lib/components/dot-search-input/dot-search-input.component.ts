import { timer } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    effect,
    ElementRef,
    input,
    output,
    signal,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import type { InputIconPassThrough } from 'primeng/types/inputicon';

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

    /**
     * PrimeNG design-token overrides for the actual `<input>`, e.g. to flatten one side's radius
     * when this component sits inside a `p-inputgroup`.
     *
     * Exists for one reason: a host that nests this component inside an input group cannot reach
     * the `<input>` from outside, because PrimeNG's own inputgroup CSS only rewires direct
     * structural relationships (`.p-inputgroup > .p-component`,
     * `.p-inputgroup > .p-iconfield > .p-component`) — and this component's own host element sits
     * between the group and that input, breaking the chain.
     *
     * `dt`, not a class: a design-token override sets the CSS custom property the component's own
     * stylesheet already reads, so it applies unconditionally rather than fighting PrimeNG's
     * dynamically-injected styles for specificity. `undefined` by default, not `{}` — PrimeNG's
     * `BaseComponent` only skips loading a scoped stylesheet and registering a theme-change
     * listener when `dt()` is falsy, and `{}` is truthy, so every existing consumer (the
     * AssetPicker included) would otherwise pay that per-instance overhead for a preset that
     * overrides nothing.
     *
     * @alias inputDt
     */
    readonly $inputDt = input<Record<string, unknown> | undefined>(undefined, {
        alias: 'inputDt'
    });

    /** Emits the trimmed term once the debounce window closes. */
    readonly search = output<string>();

    /**
     * Keeps both icons above the field they annotate, even when that field raises itself on focus.
     *
     * Inside a `p-inputgroup` (Content Drive's search bar), PrimeNG's own stylesheet gives the
     * focused field `z-index: 1` — a rule that lands, because the group turns the icon field into
     * a flex container and flex items honor `z-index` without positioning. The icons already sit
     * at that same `z-index: 1` (the iconfield stylesheet's own value, not a design token), but the
     * field comes later in the DOM, so once focused its opaque background painted over them and the
     * magnifier vanished the moment the user clicked in. One step above wins the tie; outside an
     * input group nothing competes with the icons, so the value is inert there.
     *
     * PT `root.style`, not a class: the style slot is applied through the host's `[style]` binding
     * (see `Bind`), a real inline style that beats the dynamically injected PrimeNG stylesheets
     * unconditionally. Hoisted so the object is not recreated on every change detection cycle.
     *
     * The value is a string because the PT `style` slot is typed as `Partial<CSSStyleDeclaration>`,
     * whose properties are all strings — the same shape the DOM would store anyway.
     */
    protected readonly ICON_PT: InputIconPassThrough = { root: { style: { zIndex: '2' } } };

    /** The text field itself, so a host can hand it focus. */
    // NOTE: `private`, not `#`, despite TYPESCRIPT_STANDARDS.md:87. Angular's compiler rejects a
    // signal query on an ES-private field: "Cannot use 'viewChild' on a class member that is
    // declared as ES private." The standard cannot be followed here.
    private readonly $input = viewChild<ElementRef<HTMLInputElement>>('input');

    /**
     * Moves focus to the text field.
     *
     * Deliberately the whole of this component's involvement in keyboard shortcuts: the host owns the
     * combination and the registration and calls this, so the box stays usable by a surface that has
     * no shortcut registry at all. Never alters the current term, and is a no-op when the field
     * already holds focus.
     */
    focus(): void {
        this.$input()?.nativeElement.focus();
    }

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

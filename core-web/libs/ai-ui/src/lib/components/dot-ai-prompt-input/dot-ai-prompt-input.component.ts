import {
    afterRenderEffect,
    Component,
    ElementRef,
    input,
    output,
    signal,
    untracked,
    viewChild
} from '@angular/core';

import { TextareaModule } from 'primeng/textarea';

/**
 * The prompt composer shared by the dotAI screens.
 *
 * **Starts as a single line and grows with the content.** Empty, it is one row: the projected
 * controls sit on the same line as the field. As soon as the prompt needs a second line the
 * field takes the full width and the controls drop to a row of their own underneath, so the
 * toolbar is always along the bottom edge of the card. Height comes from `scrollHeight` on
 * every keystroke, capped by `max-h` so a long prompt scrolls instead of pushing the results
 * off screen.
 *
 * The border belongs to the card, so the textarea's own border, radius, padding and focus
 * shadow are switched off rather than nested inside it — otherwise you see a box within a
 * box. The card is deliberately **not** `overflow-hidden`: there is nothing to clip (the
 * textarea paints no border or background) and clipping crops any overlay a projected control
 * opens.
 *
 * Both slots are projected rather than configured, so this component stays unaware of what
 * each screen puts in them — a size select and a Generate button on one, a send/stop pair on
 * another:
 *
 * - `[promptStart]` sits at the left, before the field when collapsed.
 * - `[promptEnd]` is pushed to the right; hints and action buttons go here.
 *
 * Enter submits and Shift+Enter inserts a newline, so the shortcut is the same wherever the
 * composer appears.
 */
@Component({
    selector: 'dot-ai-prompt-input',
    imports: [TextareaModule],
    templateUrl: './dot-ai-prompt-input.component.html',
    host: { class: 'block' }
})
export class DotAiPromptInputComponent {
    /** Current prompt text. Paired with {@link valueChange} rather than a form directive. */
    readonly value = input<string>('');

    /** Resolved placeholder text — callers pipe their own i18n. */
    readonly placeholder = input<string>('');

    /** Resolved accessible name for the textarea. */
    readonly ariaLabel = input<string>('');

    /** Blocks typing, e.g. while the instance is unconfigured. */
    readonly disabled = input<boolean>(false);

    readonly valueChange = output<string>();

    /** Enter pressed without Shift. Callers decide what submitting means. */
    readonly submitted = output<void>();

    // Not `#field`: Angular rejects a signal query on an ES-private member (NG1053).
    private readonly $field = viewChild.required<ElementRef<HTMLTextAreaElement>>('field');

    /** Whether the field has outgrown one line and the toolbar has its own row. */
    protected readonly $expanded = signal(false);

    constructor() {
        // afterRenderEffect, not a plain effect: this measures the DOM, and on an external
        // change — the field being cleared after a submit — a plain effect runs before the
        // `[value]` binding has been applied, so it would measure the previous prompt.
        // Reading `value()` is what registers the dependency; the text itself is read off the
        // element, which is current in both this path and the keystroke one.
        afterRenderEffect(() => {
            this.value();
            this.#syncSize();
        });
    }

    protected onInput(event: Event): void {
        const field = event.target as HTMLTextAreaElement;

        this.valueChange.emit(field.value);
        // Resized here as well as in the render effect, so the composer grows as you type
        // even if a caller chooses not to bind `value` back.
        this.#syncSize();
    }

    protected onKeydown(event: KeyboardEvent): void {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            this.submitted.emit();
        }
    }

    /**
     * Matches the field's height to its content and decides which layout to use.
     *
     * Clearing the height first is what makes the measurement meaningful: with `rows="1"` and
     * no explicit height, `clientHeight` reports the one-line box while `scrollHeight` reports
     * what the content actually needs.
     */
    #syncSize(): void {
        const field = this.$field().nativeElement;

        field.style.height = 'auto';

        const text = field.value;
        // A newline always means a second line whatever the width — and in jsdom, where every
        // measurement is 0, it is the only signal there is.
        const needsRoom = text.includes('\n') || field.scrollHeight > field.clientHeight + 1;

        if (needsRoom) {
            this.$expanded.set(true);
        } else if (!text) {
            // Only an empty field returns to a single row. Collapsing the moment the content
            // "fits" is what oscillates: the collapsed row is narrower, because the controls
            // share it, so a prompt that fits at the expanded width wraps again as soon as
            // they rejoin the line — and the composer would flip on every keystroke for any
            // prompt in that band. The field still shrinks back to its content height; it is
            // only the toolbar that stays put until the prompt is sent or cleared.
            this.$expanded.set(false);
        }

        // untracked: this effect writes `$expanded` above, so reading it back as a dependency
        // would schedule itself forever.
        field.style.height = untracked(this.$expanded) ? `${field.scrollHeight}px` : '';
    }
}

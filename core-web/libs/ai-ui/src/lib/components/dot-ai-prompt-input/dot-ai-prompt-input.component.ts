import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { TextareaModule } from 'primeng/textarea';

/**
 * The prompt composer shared by the dotAI screens: one bordered card holding a borderless
 * textarea with a row of controls beneath it.
 *
 * The border belongs to the card, so the textarea's own border, radius and focus shadow are
 * switched off rather than nested inside it — otherwise you see a box within a box. The card
 * is deliberately **not** `overflow-hidden`: there is nothing to clip (the textarea paints no
 * border or background) and clipping crops any overlay a projected control opens.
 *
 * Both slots are projected rather than configured, so this component stays unaware of what
 * each screen puts in them — a size select and a Generate button on one, a send/stop pair on
 * another:
 *
 * - `[promptStart]` sits at the left of the control row.
 * - `[promptEnd]` is pushed to the right; hints and action buttons go here.
 *
 * Enter submits and Shift+Enter inserts a newline, so the shortcut is the same wherever the
 * composer appears.
 */
@Component({
    selector: 'dot-ai-prompt-input',
    imports: [TextareaModule],
    templateUrl: './dot-ai-prompt-input.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
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

    /** Visible rows before the textarea scrolls. */
    readonly rows = input<number>(2);

    readonly valueChange = output<string>();

    /** Enter pressed without Shift. Callers decide what submitting means. */
    readonly submitted = output<void>();

    protected onInput(event: Event): void {
        this.valueChange.emit((event.target as HTMLTextAreaElement).value);
    }

    protected onKeydown(event: KeyboardEvent): void {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            this.submitted.emit();
        }
    }
}

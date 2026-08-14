import {
    ChangeDetectionStrategy,
    Component,
    computed,
    ElementRef,
    input,
    output,
    viewChild
} from '@angular/core';

import { DotMessagePipe, SafeUrlPipe } from '@dotcms/ui';

/**
 * One framed page render: a browser-chrome address bar over an iframe.
 *
 * Purely presentational — it frames whatever URL it's handed and reports when the
 * document loaded. The marker overlay and the scroll sync need BOTH frames at once,
 * so those live in the parent ({@link DotA11yPreviewComponent}), which reaches the
 * element through {@link element}.
 */
@Component({
    selector: 'dot-a11y-preview-frame',
    imports: [DotMessagePipe, SafeUrlPipe],
    templateUrl: './a11y-preview-frame.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex min-h-0 flex-col overflow-hidden rounded-2xl border border-surface-300 bg-white shadow-lg'
    }
})
export class DotA11yPreviewFrameComponent {
    /** The page URL to frame. Empty renders no iframe at all. */
    readonly url = input.required<string>();

    /** i18n key for the mode badge and the iframe's accessible title. */
    readonly labelKey = input.required<string>();

    /** Material Symbols ligature for the mode badge ("auto_awesome" / "public"). */
    readonly icon = input.required<string>();

    /** What the address bar shows — host + path. Decorative: the pill isn't editable. */
    readonly address = input('');

    /**
     * Test id stem, e.g. `studio-preview` → `studio-preview-iframe` on the frame and
     * `studio-preview-label` on the mode badge.
     */
    readonly testId = input.required<string>();

    /** The framed document finished (re)loading — markers and scroll sync must rewire. */
    readonly loaded = output<void>();

    protected readonly $iframeTestId = computed(() => `${this.testId()}-iframe`);
    protected readonly $labelTestId = computed(() => `${this.testId()}-label`);

    // NOTE: `private`, not `#`. Angular rejects an ES-private member for a signal query
    // outright — "Cannot use 'viewChild' on a class member that is declared as ES private"
    // — because the compiler has to write to the field from generated code.
    private readonly $frame = viewChild<ElementRef<HTMLIFrameElement>>('frame');

    /**
     * The `<iframe>` element, or undefined before it renders (no URL yet).
     * The parent needs it to inject markers and mirror scroll across both frames.
     */
    readonly element = computed(() => this.$frame()?.nativeElement);
}

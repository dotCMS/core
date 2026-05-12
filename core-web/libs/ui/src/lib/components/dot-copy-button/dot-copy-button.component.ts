import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    signal,
    viewChild
} from '@angular/core';

import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';

import { DotClipboardUtil } from '../../services/clipboard/ClipboardUtil';

/**
 * Icon button to copy to clipboard the string you pass to it,
 * it uses tooltip to indicate the user the action and the result.
 */
@Component({
    selector: 'dot-copy-button',
    providers: [DotClipboardUtil],
    templateUrl: './dot-copy-button.component.html',
    imports: [Tooltip, Button],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCopyButtonComponent {
    /**
     * String to be copied to clipboard
     */
    copy = input.required<string>();

    /**
     * Label to be displayed in the button
     */
    label = input('');

    /**
     * Tooltip text to be displayed when hovering the button
     */
    originalTooltipText = input('', { alias: 'tooltipText' });

    /**
     * Custom class to be added to the button
     */
    customClass = input('');

    /**
     * Tooltip position to be displayed when hovering the button
     */
    tooltipPosition = input('bottom');

    /** Icon-only mode (no label) uses fixed dimensions for compact input-field integration. */
    $clazz = computed(() => {
        if (this.label()) {
            return this.customClass();
        }

        // Icon-only: constrain dimensions for compact display
        return `w-8 h-8 min-w-8 p-0 ${this.customClass()}`.trim();
    });

    private readonly tooltipRef = viewChild(Tooltip);

    private dotClipboardUtil: DotClipboardUtil = inject(DotClipboardUtil);
    private dotMessageService: DotMessageService = inject(DotMessageService);
    private $copyState = signal<'idle' | 'copied' | 'error'>('idle');
    private $resetTimer: ReturnType<typeof setTimeout> | null = null;

    $icon = computed(() => (this.$copyState() === 'copied' ? 'pi pi-check' : 'pi pi-copy'));

    $tooltipText = computed(() => {
        const state = this.$copyState();
        if (state === 'copied') return this.dotMessageService.get('Copied');
        if (state === 'error') return 'Error';

        return this.originalTooltipText() || this.dotMessageService.get('Copy');
    });

    /**
     * Copy url to clipboard and update the tooltip text with the result
     *
     * @memberof DotCopyButtonComponent
     */
    copyUrlToClipboard($event: MouseEvent): void {
        $event.stopPropagation();
        clearTimeout(this.$resetTimer);

        this.dotClipboardUtil
            .copy(this.copy())
            .then(() => {
                this.$copyState.set('copied');
                this.tooltipRef()?.show();
                this.$resetTimer = setTimeout(() => this.$copyState.set('idle'), 1000);
            })
            .catch((error) => {
                this.$copyState.set('error');
                this.tooltipRef()?.show();
                this.$resetTimer = setTimeout(() => this.$copyState.set('idle'), 1000);
                console.error('[DotCopyButtonComponent] Error copying to clipboard: ', error);
            });
    }
}

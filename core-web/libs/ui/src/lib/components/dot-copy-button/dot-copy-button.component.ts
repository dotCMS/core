import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';

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
    styleUrls: ['./dot-copy-button.component.scss'],
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

    // Final CSS class to be added to the button
    $clazz = computed(() => `p-button p-button-sm p-button-text ${this.customClass()}`);

    private dotClipboardUtil: DotClipboardUtil = inject(DotClipboardUtil);
    private dotMessageService: DotMessageService = inject(DotMessageService);
    private $tempTooltipText = signal<string>('');

    // Final tooltip text to be displayed
    $tooltipText = computed(() => {
        if (this.$tempTooltipText()) {
            return this.$tempTooltipText();
        }

        return this.originalTooltipText() || this.dotMessageService.get('Copy');
    });

    /**
     * Copy url to clipboard and update the tooltip text with the result
     *
     * @memberof DotCopyButtonComponent
     */
    copyUrlToClipboard($event: MouseEvent): void {
        $event.stopPropagation();

        this.dotClipboardUtil
            .copy(this.copy())
            .then(() => {
                this.$tempTooltipText.set(this.dotMessageService.get('Copied'));
                setTimeout(() => this.$tempTooltipText.set(''), 1000);
            })
            .catch((error) => {
                this.$tempTooltipText.set('Error');
                console.error('[DotCopyButtonComponent] Error copying to clipboard: ', error);
            });
    }
}

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
    // When label is empty, use icon-only button styling for input field integration
    // When label exists, use text button for standalone usage
    $clazz = computed(() => {
        const baseClasses = 'p-button p-button-text p-button-sm ';

        if (this.label()) {
            return `${baseClasses} ${this.customClass()}`;
        } else {
            return `${baseClasses} w-9 h-9 min-w-9 p-0 ${this.customClass()}`;
        }

        return `${baseClasses} ${this.customClass()}`;
    });

    /**
     * PrimeNG passthrough configuration for icon-only button styling.
     * Uses Tailwind classes for transparent background and proper sizing.
     */
    readonly iconOnlyPt = {
        root: {
            class: '!w-8 !h-8 !min-w-8 !min-h-8 !p-0 !bg-transparent !border-none'
        },
        icon: {
            class: '!m-0'
        }
    };

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

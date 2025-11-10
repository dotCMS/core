import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { DotMessagePipe, fadeInContent } from '@dotcms/ui';

@Component({
    selector: 'dot-analytics-state-message',
    imports: [CommonModule, DotMessagePipe],
    template: `
        <div
            class="flex flex-column justify-content-center align-items-center h-full text-center gap-3"
            [@fadeInContent]>
            <i [class]="$iconClasses()"></i>
            <div class="state-message">{{ message() | dm }}</div>
        </div>
    `,
    styleUrl: './dot-analytics-state-message.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: [fadeInContent]
})
export class DotAnalyticsStateMessageComponent {
    /**
     * The message key to display (will be translated)
     */
    message = input.required<string>();

    /**
     * The PrimeNG icon class name (e.g., 'pi-info-circle', 'pi-exclamation-triangle')
     */
    icon = input.required<string>();

    /**
     * Icon size class (default: 'text-2xl')
     */
    iconSize = input<string>('text-4xl');

    /**
     * Icon color class (default: 'text-gray-400')
     */
    iconColor = input<string>('text-gray-400');

    /**
     * Additional icon CSS classes
     */
    iconClasses = input<string>('');

    /**
     * Computed signal for complete icon classes combining all icon-related inputs
     */
    protected readonly $iconClasses = computed(() => {
        const baseClasses = 'pi';
        const iconName = this.icon();
        const size = this.iconSize();
        const color = this.iconColor();
        const additional = this.iconClasses();

        const classes = [baseClasses, iconName, size, color];

        if (additional) {
            classes.push(additional);
        }

        return classes.filter(Boolean).join(' ');
    });
}

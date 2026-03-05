import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { DotMessagePipe, fadeInContent } from '@dotcms/ui';

/**
 * Reusable empty/error state message component for analytics charts and tables.
 * Displays a centered icon and translated message for loading, error, and empty states.
 */
@Component({
    selector: 'dot-analytics-state-message',
    imports: [DotMessagePipe],
    templateUrl: './dot-analytics-state-message.component.html',
    styleUrl: './dot-analytics-state-message.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: [fadeInContent]
})
export class DotAnalyticsStateMessageComponent {
    /** The message key to display (will be translated) */
    readonly $message = input.required<string>({ alias: 'message' });

    /** The PrimeNG icon class name (e.g., 'pi-info-circle', 'pi-exclamation-triangle') */
    readonly $icon = input.required<string>({ alias: 'icon' });

    /** Icon size class (default: 'text-4xl') */
    readonly $iconSize = input<string>('text-4xl', { alias: 'iconSize' });

    /** Icon color class (default: 'text-gray-400') */
    readonly $iconColor = input<string>('text-gray-400', { alias: 'iconColor' });

    /** Additional icon CSS classes */
    readonly $extraClasses = input<string>('', { alias: 'iconClasses' });

    /** Computed signal for complete icon classes combining all icon-related inputs */
    protected readonly $iconClasses = computed(() => {
        const baseClasses = 'pi';
        const iconName = this.$icon();
        const size = this.$iconSize();
        const color = this.$iconColor();
        const additional = this.$extraClasses();

        const classes = [baseClasses, iconName, size, color];

        if (additional) {
            classes.push(additional);
        }

        return classes.filter(Boolean).join(' ');
    });
}

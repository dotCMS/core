import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-analytics-state-message',
    standalone: true,
    imports: [CommonModule, DotMessagePipe],
    template: `
        <div
            class="flex flex-column justify-content-center align-items-center h-full text-center gap-3">
            <i class="pi {{ icon() }} text-2xl"></i>
            <div class="state-message">{{ message() | dm }}</div>
        </div>
    `,
    styleUrl: './dot-analytics-state-message.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
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
}

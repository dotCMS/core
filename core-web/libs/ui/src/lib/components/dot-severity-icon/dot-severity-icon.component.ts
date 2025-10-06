import { Component, input } from '@angular/core';

import { CheckIcon } from 'primeng/icons/check';
import { ExclamationTriangleIcon } from 'primeng/icons/exclamationtriangle';
import { InfoCircleIcon } from 'primeng/icons/infocircle';
import { TimesCircleIcon } from 'primeng/icons/timescircle';

/**
 * Component that renders the appropriate icon based on message severity
 * Supports PrimeNG toast severity types: 'success', 'info', 'error', 'warn'
 *
 * @export
 * @class DotSeverityIconComponent
 */
@Component({
    selector: 'dot-severity-icon',
    imports: [CheckIcon, InfoCircleIcon, TimesCircleIcon, ExclamationTriangleIcon],
    templateUrl: './dot-severity-icon.component.html'
})
export class DotSeverityIconComponent {
    /**
     * The severity level that determines which icon to display
     * Valid values: 'success', 'info', 'error', 'warn'
     */
    $severity = input<'success' | 'info' | 'error' | 'warn'>('info', { alias: 'severity' });
}

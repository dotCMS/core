import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { DotPageScannerService } from '@dotcms/portlets/dot-ema/ui';

import { DotAccessibilityStudioPickerComponent } from '../dot-accessibility-studio-picker/dot-accessibility-studio-picker.component';
import { DotAccessibilityStudioRunComponent } from '../dot-accessibility-studio-run/dot-accessibility-studio-run.component';
import { AccessibilityStudioStore } from '../store/accessibility-studio.store';

/**
 * Full-screen Accessibility Studio shell. Provides the store and switches between
 * the page picker and the run screen based on the studio phase (§7). The store
 * is provided here so the selected page + run state survive the screen switch.
 */
@Component({
    selector: 'dot-accessibility-studio-shell',
    standalone: true,
    imports: [DotAccessibilityStudioPickerComponent, DotAccessibilityStudioRunComponent],
    template: `
        @if (store.inPicker()) {
            <dot-accessibility-studio-picker />
        } @else {
            <dot-accessibility-studio-run />
        }
    `,
    providers: [AccessibilityStudioStore, DotPageScannerService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 block bg-surface-100' }
})
export class DotAccessibilityStudioShellComponent {
    readonly store = inject(AccessibilityStudioStore);
}

import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { ToastModule } from 'primeng/toast';
import type { ToastPositionType } from 'primeng/types/toast';

import { DotSeverityIconComponent } from '../dot-severity-icon/dot-severity-icon.component';

/**
 * Toast outlet for `MessageService` messages.
 *
 * Exists because PrimeNG's default template renders `summary` and `detail` as escaped text, while
 * several language keys carry markup — `content-drive.add-dotasset-success-detail` bolds the file
 * name, for example — so a bare `<p-toast />` shows literal `<b>` tags. Both fields go through
 * `innerHtml` instead (Angular sanitizes it, so backend-supplied error text stays safe), and the
 * severity glyph is swapped for `dot-severity-icon`.
 *
 * The `MessageService` stays with the consumer: provide it on the host so each outlet — a portlet
 * shell, a dialog — keeps its own message stream instead of sharing one globally.
 */
@Component({
    selector: 'dot-toast',
    imports: [ToastModule, DotSeverityIconComponent],
    templateUrl: './dot-toast.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotToastComponent {
    /** Where the stack renders, mirroring `p-toast`'s own positions. */
    $position = input<ToastPositionType>('top-center', { alias: 'position' });
}

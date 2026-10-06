import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Footer slot of {@link DotDialogComponent}: the action bar pinned to the bottom of the dialog.
 *
 * A pure slot — it contributes the divider, the padding and the right alignment, and nothing else.
 * Each dialog projects its own buttons, because the number and kind of actions vary (a single
 * Close, Cancel + Save, or Cancel + a secondary action + Save).
 *
 * `flex-wrap` is what lets a full-width child (`class="w-full"`, e.g. an error message) take its own
 * line above the buttons without any extra API.
 */
@Component({
    selector: 'dot-dialog-footer',
    templateUrl: './dot-dialog-footer.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex flex-none flex-wrap items-center justify-end gap-2 border-t border-gray-200 px-4 py-3'
    }
})
export class DotDialogFooterComponent {}

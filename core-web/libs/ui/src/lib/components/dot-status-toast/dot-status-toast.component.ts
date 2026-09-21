import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { ToastModule } from 'primeng/toast';
import type { ToastPositionType } from 'primeng/types/toast';

import { DotSeverityIconComponent } from '../dot-severity-icon/dot-severity-icon.component';

/** The key this outlet claims by default, so callers and the template cannot drift apart. */
export const STATUS_TOAST_KEY = 'dot-status';

/**
 * Toast outlet for a run's status: an icon, one short line, and a dismiss.
 *
 * Separate from {@link DotToastComponent} because the two carry different things, not because the
 * styling differs. That one is a report — a title plus a detail paragraph, often several lines,
 * frequently carrying markup — and it is 350px wide because it has to be. This one states an
 * outcome in a couple of words, so it is sized to its text and drops `detail` entirely rather than
 * growing to fit something a caller should not have sent here.
 *
 * It offers no way to dismiss it either. A status reports something already under way and clears
 * when whoever raised it says the work is done, so a close button would ask the reader to tidy up
 * after a thing they did not start and cannot affect.
 *
 * Colour comes from Lara through the dotCMS preset, keyed on the message severity. The prototype
 * this follows used a dark pill; that is deliberately not reproduced, because a black surface is
 * not a pattern in this design system.
 */
@Component({
    selector: 'dot-status-toast',
    imports: [ToastModule, DotSeverityIconComponent],
    templateUrl: './dot-status-toast.component.html',
    styleUrl: './dot-status-toast.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotStatusToastComponent {
    /** Where the stack renders, mirroring `p-toast`'s own positions. */
    $position = input<ToastPositionType>('bottom-center', { alias: 'position' });

    /**
     * Which messages this outlet claims.
     *
     * Not optional in practice: a portlet provides ONE `MessageService`, and an outlet with no key
     * renders every message on it — so sharing a shell with `dot-toast` showed each status twice,
     * once wide at the top and once compact at the bottom. Callers add with this key to reach this
     * outlet and no other.
     */
    $key = input<string>(STATUS_TOAST_KEY, { alias: 'key' });
}

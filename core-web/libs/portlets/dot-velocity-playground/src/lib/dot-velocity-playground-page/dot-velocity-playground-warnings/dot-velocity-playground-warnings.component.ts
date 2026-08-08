import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';

import { MessageModule } from 'primeng/message';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { VelocityWarning } from '../../models/dot-velocity-playground.models';

/**
 * The non-fatal warnings banner for a Velocity run — one row per
 * {@link VelocityWarning} (type chip, message, and an optional line/column).
 *
 * Split out of the output pane's template, which had grown large enough that the
 * warnings list buried the pane's actual structure. Presentational only: the
 * caller decides when to show it (`store.hasWarnings() && !store.hasError()`).
 */
@Component({
    selector: 'dot-velocity-playground-warnings',
    standalone: true,
    imports: [MessageModule, DotMessagePipe],
    templateUrl: './dot-velocity-playground-warnings.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'contents' }
})
export class DotVelocityPlaygroundWarningsComponent {
    /** The warnings to list. Rendering an empty array shows an empty banner. */
    readonly warnings = input.required<VelocityWarning[]>();

    private readonly dm = inject(DotMessageService);

    /**
     * The "line N, column M" suffix for a warning, or null when it carries no
     * line (the template then omits the element entirely). A missing column
     * renders as an em dash — the backend reports line without column for some
     * warning types. Built here rather than in the template: the interpolation
     * needs two null checks plus number→string coercion per argument, which as a
     * nested pipe expression was effectively unreadable.
     */
    protected location(warning: VelocityWarning): string | null {
        if (warning.line === undefined || warning.line === null) {
            return null;
        }

        const column =
            warning.column === undefined || warning.column === null ? '—' : String(warning.column);

        return this.dm.get('velocityPlayground.error.location', String(warning.line), column);
    }
}

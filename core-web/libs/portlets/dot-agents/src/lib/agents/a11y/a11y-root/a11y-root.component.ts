import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Root of the Accessibility Studio agent — a thin layout wrapper that owns the
 * full-height host box and hosts the child routes via `<router-outlet>`:
 *   - `''`     → the page picker (provides its own {@link A11yPickerStore})
 *   - `**`     → the run screen (provides its own {@link A11yRunStore})
 *
 * It holds NO store: the picker and run screens are independent routes, each
 * providing its own store at its component (see {@link dotAccessibilityStudioRoutes}),
 * so run state resets per page and picker state never leaks into a run.
 */
@Component({
    selector: 'dot-a11y',
    standalone: true,
    imports: [RouterOutlet],
    template: `
        <router-outlet />
    `,
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 block bg-surface-100' }
})
export class DotA11yRootComponent {}

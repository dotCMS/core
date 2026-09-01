import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Host for the AI agents area. Thin router-outlet wrapper: the gallery landing
 * renders at the base path and each agent renders full-screen at `agents/{id}`.
 * Owns only the full-height layout box so agents (and the landing) can fill it.
 */
@Component({
    selector: 'dot-agents-shell',
    imports: [RouterOutlet],
    template: `
        <router-outlet />
    `,
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 block bg-surface-100' }
})
export class DotAgentsShellComponent {}

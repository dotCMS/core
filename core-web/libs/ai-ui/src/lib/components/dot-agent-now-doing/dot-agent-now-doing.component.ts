import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { AgentMessage } from '../../models/agent-message';

/**
 * The pulsing "now doing" banner — a spinner beside the text of the step the
 * agent is currently working on. Falls back to an i18n key when the agent is
 * working but hasn't reported a step yet. Render it while a run is in flight.
 */
@Component({
    selector: 'dot-agent-now-doing',
    imports: [DotMessagePipe],
    templateUrl: './dot-agent-now-doing.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'mt-2 mb-1 flex items-center gap-2.5 rounded-xl border border-primary-100 bg-primary-50 px-3.5 py-2.5',
        'data-testid': 'agent-now-doing'
    }
})
export class DotAgentNowDoingComponent {
    /** The step the agent is currently on. Null → show the fallback text. */
    readonly message = input<AgentMessage | null>(null);

    /** i18n key for the fallback text when there's no active step yet. */
    readonly fallbackKey = input<string>('agent.activity.working');
}

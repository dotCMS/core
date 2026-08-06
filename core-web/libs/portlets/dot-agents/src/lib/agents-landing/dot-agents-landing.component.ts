import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';

import { DotColorIconComponent, DotMessagePipe } from '@dotcms/ui';

import { DOT_AGENTS } from '../agent-registry';

/**
 * The agents gallery: a card grid of every registered agent. Available agents
 * link to `agents/{id}`; `coming-soon` agents render disabled with a tag. The
 * grid is derived entirely from {@link DOT_AGENTS}, so adding an agent needs no
 * change here.
 */
@Component({
    selector: 'dot-agents-landing',
    standalone: true,
    imports: [
        NgTemplateOutlet,
        RouterLink,
        CardModule,
        TagModule,
        DotMessagePipe,
        DotColorIconComponent
    ],
    templateUrl: './dot-agents-landing.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'block h-full min-h-0 overflow-y-auto' }
})
export class DotAgentsLandingComponent {
    protected readonly agents = DOT_AGENTS;
}

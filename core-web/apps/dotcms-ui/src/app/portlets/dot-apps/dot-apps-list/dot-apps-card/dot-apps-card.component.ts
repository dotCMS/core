import { MarkdownComponent } from 'ngx-markdown';

import { Component, input, output } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { TooltipModule } from 'primeng/tooltip';

import { DotApp } from '@dotcms/dotcms-models';
import { DotAvatarDirective, DotColorIconComponent, DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-apps-card',
    templateUrl: './dot-apps-card.component.html',
    imports: [
        AvatarModule,
        BadgeModule,
        MarkdownComponent,
        TooltipModule,
        DotAvatarDirective,
        DotColorIconComponent,
        DotMessagePipe
    ]
})
export class DotAppsCardComponent {
    $app = input.required<DotApp>({ alias: 'app' });
    actionFired = output<string>();
}

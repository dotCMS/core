import { MarkdownComponent } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { CardModule } from 'primeng/card';
import { TooltipModule } from 'primeng/tooltip';

import { DotApp } from '@dotcms/dotcms-models';
import { DotAvatarDirective, DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-apps-card',
    templateUrl: './dot-apps-card.component.html',
    styleUrls: ['./dot-apps-card.component.scss'],
    imports: [
        CommonModule,
        CardModule,
        AvatarModule,
        BadgeModule,

        MarkdownComponent,
        TooltipModule,
        DotAvatarDirective,
        DotMessagePipe
    ]
})
export class DotAppsCardComponent {
    $app = input.required<DotApp>({ alias: 'app' });
    actionFired = output<string>();
}

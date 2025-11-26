import { MarkdownComponent } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { CardModule } from 'primeng/card';
import { TooltipModule } from 'primeng/tooltip';

import { DotApp } from '@dotcms/dotcms-models';
import { DotAvatarDirective, DotIconComponent, DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-apps-card',
    templateUrl: './dot-apps-card.component.html',
    styleUrls: ['./dot-apps-card.component.scss'],
    imports: [
        CommonModule,
        CardModule,
        AvatarModule,
        BadgeModule,
        DotIconComponent,
        MarkdownComponent,
        TooltipModule,
        DotAvatarDirective,
        DotMessagePipe
    ]
})
export class DotAppsCardComponent {
    @Input() app: DotApp;
    @Output() actionFired = new EventEmitter<string>();
}

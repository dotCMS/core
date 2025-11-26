import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

export interface InfoPage {
    icon: string;
    title: string;
    description: string;
    buttonPath: string;
    buttonText: string;
}

@Component({
    selector: 'dot-info-page',
    imports: [DotMessagePipe, ButtonModule, RouterLink],
    templateUrl: './dot-info-page.component.html',
    styleUrl: './dot-info-page.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotInfoPageComponent {
    @Input({ required: true }) info: InfoPage;
}

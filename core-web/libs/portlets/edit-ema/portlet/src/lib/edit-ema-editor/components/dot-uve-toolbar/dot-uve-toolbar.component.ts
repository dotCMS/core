import { ChangeDetectionStrategy, Component } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';


@Component({
    selector: 'dot-uve-toolbar',
    standalone: true,
    imports: [ButtonModule, DotMessagePipe],
    templateUrl: './dot-uve-toolbar.component.html',
    styleUrl: './dot-uve-toolbar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveToolbarComponent {}

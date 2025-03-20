import { ChangeDetectionStrategy, Component } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-relationship-header',
    standalone: true,
    imports: [DotMessagePipe],
    templateUrl: './header.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class HeaderComponent {}

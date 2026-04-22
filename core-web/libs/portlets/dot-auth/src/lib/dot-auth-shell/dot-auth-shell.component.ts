import { ChangeDetectionStrategy, Component } from '@angular/core';

import { DotAuthListComponent } from '../dot-auth-list/dot-auth-list.component';

@Component({
    selector: 'dot-auth-shell',
    imports: [DotAuthListComponent],
    templateUrl: './dot-auth-shell.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0' }
})
export class DotAuthShellComponent {}

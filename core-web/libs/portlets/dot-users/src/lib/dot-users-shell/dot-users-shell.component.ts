import { ChangeDetectionStrategy, Component } from '@angular/core';

import { DotUsersListComponent } from '../dot-users-list/dot-users-list.component';

@Component({
    selector: 'dot-users-shell',
    standalone: true,
    imports: [DotUsersListComponent],
    template: '<dot-users-list />',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 block' }
})
export class DotUsersShellComponent {}

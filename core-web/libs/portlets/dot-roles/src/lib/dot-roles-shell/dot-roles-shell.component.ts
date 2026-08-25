import { ChangeDetectionStrategy, Component } from '@angular/core';

import { DotRolesPageComponent } from '../dot-roles-page/dot-roles-page.component';

@Component({
    selector: 'dot-roles-shell',
    standalone: true,
    imports: [DotRolesPageComponent],
    template: '<dot-roles-page />',
    host: { class: 'flex flex-col h-full min-h-0 block' },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotRolesShellComponent {}

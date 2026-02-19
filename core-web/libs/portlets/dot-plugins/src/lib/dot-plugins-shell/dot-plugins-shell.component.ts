import { ChangeDetectionStrategy, Component } from '@angular/core';

import { DotPluginsListComponent } from '../dot-plugins-list/dot-plugins-list.component';

@Component({
    selector: 'dot-plugins-shell',
    standalone: true,
    imports: [DotPluginsListComponent],
    template: '<dot-plugins-list />',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 block' }
})
export class DotPluginsShellComponent {}

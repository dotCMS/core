import { ChangeDetectionStrategy, Component } from '@angular/core';

import { DotToolsPageComponent } from '../dot-tools-page/dot-tools-page.component';

@Component({
    selector: 'dot-tools-shell',
    standalone: true,
    imports: [DotToolsPageComponent],
    template: '<dot-tools-page />',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 block' }
})
export class DotToolsShellComponent {}

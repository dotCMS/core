import { Component } from '@angular/core';

import { DotNetworkPageComponent } from '../dot-network-page/dot-network-page.component';

@Component({
    selector: 'dot-network-shell',
    imports: [DotNetworkPageComponent],
    template: '<dot-network-page />',
    host: { class: 'flex flex-col h-full min-h-0 block' }
})
export class DotNetworkShellComponent {}

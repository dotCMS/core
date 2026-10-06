import { ChangeDetectionStrategy, Component } from '@angular/core';

import { DotQueryToolPageComponent } from '../dot-query-tool-page/dot-query-tool-page.component';

@Component({
    selector: 'dot-query-tool-shell',
    imports: [DotQueryToolPageComponent],
    template: `
        <dot-query-tool-page />
    `,
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 block' }
})
export class DotQueryToolShellComponent {}

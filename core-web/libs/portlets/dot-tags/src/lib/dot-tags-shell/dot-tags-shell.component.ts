import { ChangeDetectionStrategy, Component } from '@angular/core';

import { DotTagsListComponent } from '../dot-tags-list/dot-tags-list.component';

@Component({
    selector: 'dot-tags-shell',
    standalone: true,
    imports: [DotTagsListComponent],
    template: '<dot-tags-list />',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 block' }
})
export class DotTagsShellComponent {}

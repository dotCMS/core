import { ChangeDetectionStrategy, Component } from '@angular/core';

import { DotCategoriesListComponent } from '../dot-categories-list/dot-categories-list.component';

@Component({
    selector: 'dot-categories-shell',
    standalone: true,
    imports: [DotCategoriesListComponent],
    template: '<dot-categories-list />',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCategoriesShellComponent {}

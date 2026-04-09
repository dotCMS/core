import { ChangeDetectionStrategy, Component } from '@angular/core';

import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';

import { DotCategoriesListComponent } from '../dot-categories-list/dot-categories-list.component';

@Component({
    selector: 'dot-categories-shell',
    imports: [DotCategoriesListComponent, ToastModule],
    providers: [MessageService],
    template: `
        <dot-categories-list />
        <p-toast />
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCategoriesShellComponent {}

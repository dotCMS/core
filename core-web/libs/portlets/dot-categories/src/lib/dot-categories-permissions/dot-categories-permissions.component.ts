import { ChangeDetectionStrategy, Component } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-categories-permissions',
    standalone: true,
    imports: [DotMessagePipe],
    template: `<p class="p-4 text-center">{{ 'categories.permissions.placeholder' | dm }}</p>`,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCategoriesPermissionsComponent {}

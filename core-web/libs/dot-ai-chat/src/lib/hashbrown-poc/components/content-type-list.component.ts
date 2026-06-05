import { exposeComponent } from '@hashbrownai/angular';

import { ChangeDetectionStrategy, Component } from '@angular/core';

import { AiContentTypeCardComponent } from './content-type-card.component';

@Component({
    selector: 'app-content-type-list',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [],
    template: `
        <div class="grid max-h-96 grid-cols-1 gap-3 overflow-y-auto md:grid-cols-2">
            <ng-content />
        </div>
    `
})
export class ContentTypeListComponent {}

export const AiContentTypeListComponent = exposeComponent(ContentTypeListComponent, {
    description:
        'A scrollable content type list container that renders card children in a responsive two-column grid.',
    input: {},
    children: [AiContentTypeCardComponent]
});

import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dot-card-field-content',
    template: `
        <ng-content />
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCardFieldContentComponent {}

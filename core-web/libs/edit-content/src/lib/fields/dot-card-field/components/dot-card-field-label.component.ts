import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dot-card-field-label',
    template: `
        <ng-content />
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCardFieldLabelComponent {}

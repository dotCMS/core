import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dot-card-field-content',
    styles: [
        `
        :host {
            display: block;
        }
        `
    ],
    template: `
        <ng-content />
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCardFieldContentComponent {}

import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
    selector: 'dot-card-field-label',
    template: `
        <label [for]="$for()" [class.p-label-input-required]="$isRequired()">
            <ng-content />
        </label>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCardFieldLabelComponent {
    $for = input<string>(null, { alias: 'for' });
    $isRequired = input.required<boolean>({ alias: 'isRequired' });
}

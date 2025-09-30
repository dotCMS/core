import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
    selector: 'dot-card-field',
    template: `
        @if ($hasError()) {
            <div class="field-error-marker"></div>
        }

        <div class="flex flex-column gap-2">
            <ng-content />
            <ng-content select="dot-card-field-content" />
            <ng-content select="dot-card-field-footer" />
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCardFieldComponent {
    $hasError = input.required<boolean>({ alias: 'hasError' });
}

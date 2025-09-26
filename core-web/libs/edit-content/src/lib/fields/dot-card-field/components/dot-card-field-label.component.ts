import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { TooltipModule } from 'primeng/tooltip';

@Component({
    selector: 'dot-card-field-label',
    imports: [TooltipModule],
    template: `
        <div class="flex gap-1 align-items-center">
            <label
                [attr.data-testid]="'label-' + $variableName()"
                [class.p-label-input-required]="$isRequired()"
                [for]="$variableName()">
                <ng-content />
            </label>
            @if ($hint()) {
                <i class="pi pi-info-circle text-xs" [pTooltip]="$hint()"></i>
            }
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush,
    styles: `
        @use 'variables' as *;
        ::ng-deep {
            .p-tooltip .p-tooltip-text {
                background: $foreground-active;
                color: $white;
            }

            .p-tooltip.p-tooltip-right .p-tooltip-arrow {
                border-right-color: $foreground-active;
            }
            .p-tooltip.p-tooltip-left .p-tooltip-arrow {
                border-left-color: $color-palette-black-op-50;
            }
            .p-tooltip.p-tooltip-top .p-tooltip-arrow {
                border-top-color: $color-palette-black-op-50;
            }
            .p-tooltip.p-tooltip-bottom .p-tooltip-arrow {
                border-bottom-color: $color-palette-black-op-50;
            }
        }
    `
})
export class DotCardFieldLabelComponent {
    $variableName = input.required<string>({ alias: 'variableName' });
    $isRequired = input.required<boolean>({ alias: 'isRequired' });
    $hint = input<string>(null, { alias: 'hint' });
}

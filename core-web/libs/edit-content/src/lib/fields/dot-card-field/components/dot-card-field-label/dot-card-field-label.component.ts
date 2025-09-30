import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { TooltipModule } from 'primeng/tooltip';

/**
 * A reusable component that renders a field label with optional hint tooltip and required indicator.
 * This component is designed to be used within card field components to provide consistent labeling.
 *
 * @example
 * ```html
 * <dot-card-field-label
 *   variableName="title"
 *   [isRequired]="true"
 *   hint="Enter a descriptive title for your content">
 * </dot-card-field-label>
 * ```
 *
 */
@Component({
    selector: 'dot-card-field-label',
    imports: [TooltipModule],
    styleUrl: './dot-card-field-label.component.scss',
    templateUrl: './dot-card-field-label.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCardFieldLabelComponent {
    /**
     * The variable name or field identifier to display as the label text.
     * This is typically the field name from the content type definition.
     *
     * @required
     */
    $variableName = input.required<string>({ alias: 'variableName' });

    /**
     * Determines whether the field is required and should display a visual indicator (usually an asterisk).
     * When true, the label will show a required indicator to inform users that the field is mandatory.
     *
     * @required
     */
    $isRequired = input.required<boolean>({ alias: 'isRequired' });

    /**
     * Optional hint text that provides additional guidance or context about the field.
     * When provided, this text will be displayed as a tooltip when users hover over the label.
     *
     * @optional
     * ```
     */
    $hint = input<string>(null, { alias: 'hint' });
}

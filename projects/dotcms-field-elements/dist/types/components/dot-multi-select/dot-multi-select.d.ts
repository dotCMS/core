import '../../stencil.core';
import { EventEmitter } from '../../stencil.core';
import { DotOption, DotFieldStatus, DotFieldValueEvent, DotFieldStatusEvent } from '../../models';
/**
 * Represent a dotcms multi select control.
 *
 * @export
 * @class DotSelectComponent
 */
export declare class DotMultiSelectComponent {
    el: HTMLElement;
    /** (optional) Disables field's interaction */
    disabled: boolean;
    /** Name that will be used as ID */
    name: string;
    /** (optional) Text to be rendered next to input field */
    label: string;
    /** (optional) Hint text that suggest a clue of the field */
    hint: string;
    /** Value/Label dropdown options separated by comma, to be formatted as: Value|Label */
    options: string;
    /** (optional) Determine if it is mandatory */
    required: boolean;
    /** (optional) Text that will be shown when required is set and condition is not met */
    requiredMessage: string;
    /** (optional) Size number of the multi-select dropdown (default=3) */
    size: string;
    /** Value set from the dropdown option */
    value: string;
    _options: DotOption[];
    status: DotFieldStatus;
    valueChange: EventEmitter<DotFieldValueEvent>;
    statusChange: EventEmitter<DotFieldStatusEvent>;
    _dotTouched: boolean;
    _dotPristine: boolean;
    componentWillLoad(): void;
    componentDidLoad(): void;
    optionsWatch(): void;
    hostData(): {
        class: import("../../models").DotFieldStatusClasses;
    };
    /**
     * Reset properties of the field, clear value and emit events.
     *
     * @memberof DotSelectComponent
     *
     */
    reset(): void;
    render(): JSX.Element;
    private validateProps;
    private shouldBeDisabled;
    private setValue;
    private getValueFromMultiSelect;
    private emitInitialValue;
    private emitStatusChange;
    private isValid;
    private emitValueChange;
}

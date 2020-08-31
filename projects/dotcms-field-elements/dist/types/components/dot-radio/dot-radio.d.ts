import '../../stencil.core';
import { EventEmitter } from '../../stencil.core';
import { DotFieldStatus, DotFieldStatusEvent, DotFieldValueEvent, DotOption } from '../../models';
/**
 * Represent a dotcms radio control.
 *
 * @export
 * @class DotRadioComponent
 */
export declare class DotRadioComponent {
    el: HTMLElement;
    /** Value set from the ratio option */
    value: string;
    /** Name that will be used as ID */
    name: string;
    /** (optional) Text to be rendered next to input field */
    label: string;
    /** (optional) Hint text that suggest a clue of the field */
    hint: string;
    /** (optional) Determine if it is mandatory */
    required: boolean;
    /** (optional) Disables field's interaction */
    disabled: boolean;
    /** (optional) Text that will be shown when required is set and condition is not met */
    requiredMessage: string;
    /** Value/Label ratio options separated by comma, to be formatted as: Value|Label */
    options: string;
    _options: DotOption[];
    status: DotFieldStatus;
    valueChange: EventEmitter<DotFieldValueEvent>;
    statusChange: EventEmitter<DotFieldStatusEvent>;
    /**
     * Reset properties of the field, clear value and emit events.
     */
    reset(): void;
    componentWillLoad(): void;
    componentDidLoad(): void;
    optionsWatch(): void;
    valueWatch(): void;
    hostData(): {
        class: import("../../models").DotFieldStatusClasses;
    };
    render(): JSX.Element;
    private validateProps;
    private isValid;
    private showErrorMessage;
    private getErrorMessage;
    private setValue;
    private emitStatusChange;
    private emitValueChange;
}

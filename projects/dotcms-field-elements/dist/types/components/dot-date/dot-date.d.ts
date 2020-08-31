import '../../stencil.core';
import { EventEmitter } from '../../stencil.core';
import { DotFieldStatusClasses, DotFieldStatusEvent, DotFieldValueEvent } from '../../models';
export declare class DotDateComponent {
    el: HTMLElement;
    /** Value format yyyy-mm-dd  e.g., 2005-12-01 */
    value: string;
    /** Name that will be used as ID */
    name: string;
    /** (optional) Text to be rendered next to input field */
    label: string;
    /** (optional) Hint text that suggest a clue of the field */
    hint: string;
    /** (optional) Determine if it is mandatory */
    required: boolean;
    /** (optional) Text that be shown when required is set and condition not met */
    requiredMessage: string;
    /** (optional) Text that be shown when min or max are set and condition not met */
    validationMessage: string;
    /** (optional) Disables field's interaction */
    disabled: boolean;
    /** (optional) Min, minimum value that the field will allow to set. Format should be yyyy-mm-dd */
    min: string;
    /** (optional) Max, maximum value that the field will allow to set. Format should be yyyy-mm-dd */
    max: string;
    /** (optional) Step specifies the legal number intervals for the input field */
    step: string;
    classNames: DotFieldStatusClasses;
    errorMessageElement: JSX.Element;
    valueChange: EventEmitter<DotFieldValueEvent>;
    statusChange: EventEmitter<DotFieldStatusEvent>;
    /**
     * Reset properties of the field, clear value and emit events.
     */
    reset(): void;
    componentWillLoad(): void;
    componentDidLoad(): void;
    minWatch(): void;
    maxWatch(): void;
    emitValueChange(event: CustomEvent): void;
    emitStatusChange(event: CustomEvent): void;
    hostData(): {
        class: DotFieldStatusClasses;
    };
    render(): JSX.Element;
    private validateProps;
    private setErrorMessageElement;
    private getErrorMessage;
}

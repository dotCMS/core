import '../../stencil.core';
import { EventEmitter } from '../../stencil.core';
import { DotFieldStatusClasses, DotFieldStatusEvent, DotFieldValueEvent } from '../../models';
export declare class DotDateTimeComponent {
    el: HTMLElement;
    /** Value format yyyy-mm-dd hh:mm:ss e.g., 2005-12-01 15:22:00 */
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
    /** (optional) Min, minimum value that the field will allow to set. Format should be yyyy-mm-dd hh:mm:ss | yyyy-mm-dd | hh:mm:ss */
    min: string;
    /** (optional) Max, maximum value that the field will allow to set. Format should be yyyy-mm-dd hh:mm:ss | yyyy-mm-dd | hh:mm:ss */
    max: string;
    /** (optional) Step specifies the legal number intervals for the input fields date && time e.g., 2,10 */
    step: string;
    /** (optional) The string to use in the date label field */
    dateLabel: string;
    /** (optional) The string to use in the time label field */
    timeLabel: string;
    classNames: DotFieldStatusClasses;
    errorMessageElement: JSX.Element;
    valueChange: EventEmitter<DotFieldValueEvent>;
    statusChange: EventEmitter<DotFieldStatusEvent>;
    private _minDateTime;
    private _maxDateTime;
    private _value;
    private _step;
    private _status;
    /**
     * Reset properties of the filed, clear value and emit events.
     */
    reset(): void;
    componentWillLoad(): void;
    valueWatch(): void;
    minWatch(): void;
    maxWatch(): void;
    stepWatch(): void;
    emitValueChange(event: CustomEvent): void;
    emitStatusChange(event: CustomEvent): void;
    hostData(): {
        class: DotFieldStatusClasses;
    };
    componentDidLoad(): void;
    render(): JSX.Element;
    private setDotAttributes;
    private validateProps;
    private statusHandler;
    private formatValue;
    private getValue;
    private setStatus;
    private isValueComplete;
    private isStatusComplete;
    private isValid;
    private isStatusInRange;
    private setErrorMessageElement;
    private getErrorMessage;
}

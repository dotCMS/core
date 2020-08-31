import '../../stencil.core';
import { EventEmitter } from '../../stencil.core';
import { DotFieldStatus, DotFieldValueEvent, DotFieldStatusEvent } from '../../models';
export declare class DotDateRangeComponent {
    el: HTMLElement;
    /** (optional) Value formatted with start and end date splitted with a comma */
    value: string;
    /** Name that will be used as ID */
    name: string;
    /** (optional) Text to be rendered next to input field */
    label: string;
    /** (optional) Hint text that suggest a clue of the field */
    hint: string;
    /** (optional) Max value that the field will allow to set */
    max: string;
    /** (optional) Min value that the field will allow to set */
    min: string;
    /** (optional) Determine if it is needed */
    required: boolean;
    /** (optional) Text that be shown when required is set and condition not met */
    requiredMessage: string;
    /** (optional) Disables field's interaction */
    disabled: boolean;
    /** (optional) Date format used by the field when displayed */
    displayFormat: string;
    /** (optional) Array of date presets formatted as [{ label: 'PRESET_LABEL', days: NUMBER }] */
    presets: {
        label: string;
        days: number;
    }[];
    /** (optional) Text to be rendered next to presets field */
    presetLabel: string;
    status: DotFieldStatus;
    valueChange: EventEmitter<DotFieldValueEvent>;
    statusChange: EventEmitter<DotFieldStatusEvent>;
    private flatpickr;
    private defaultPresets;
    /**
     * Reset properties of the field, clear value and emit events.
     */
    reset(): void;
    valueWatch(): void;
    presetsWatch(): void;
    componentWillLoad(): void;
    componentDidLoad(): void;
    hostData(): {
        class: import("../../models").DotFieldStatusClasses;
    };
    render(): JSX.Element;
    private parseDate;
    private validateProps;
    private isDisabled;
    private setPreset;
    private isValid;
    private isDateRangeValid;
    private setValue;
    private showErrorMessage;
    private getErrorMessage;
    private emitStatusChange;
    private emitValueChange;
}

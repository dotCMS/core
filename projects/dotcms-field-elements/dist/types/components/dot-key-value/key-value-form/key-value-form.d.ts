import '../../../stencil.core';
import { EventEmitter } from '../../../stencil.core';
import { DotKeyValueField } from '../../../models';
export declare class DotKeyValueComponent {
    el: HTMLElement;
    /** (optional) Disables all form interaction */
    disabled: boolean;
    /** (optional) Label for the add item button */
    addButtonLabel: string;
    /** (optional) Placeholder for the key input text */
    keyPlaceholder: string;
    /** (optional) Placeholder for the value input text */
    valuePlaceholder: string;
    /** (optional) The string to use in the key input label */
    keyLabel: string;
    /** (optional) The string to use in the value input label */
    valueLabel: string;
    /** Emit the added value, key/value pair */
    add: EventEmitter<DotKeyValueField>;
    /** Emit when any of the input is blur */
    lostFocus: EventEmitter<FocusEvent>;
    inputs: DotKeyValueField;
    render(): JSX.Element;
    private isButtonDisabled;
    private isFormValid;
    private setValue;
    private addKey;
    private clearForm;
    private focusKeyInputField;
}

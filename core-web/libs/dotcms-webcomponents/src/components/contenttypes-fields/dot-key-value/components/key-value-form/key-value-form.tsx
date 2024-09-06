import { Component, Prop, State, Element, Event, EventEmitter, h, Watch } from '@stencil/core';
import { DotKeyValueField } from '../../../../../models';

const DEFAULT_VALUE = { key: '', value: '' };

@Component({
    tag: 'key-value-form',
    styleUrl: 'key-value-form.scss'
})
export class DotKeyValueComponent {
    @Element()
    el: HTMLElement;

    /** (optional) Disables all form interaction */
    @Prop({ reflect: true })
    disabled = false;

    /** (optional) Label for the add item button */
    @Prop({
        reflect: true
    })
    addButtonLabel = 'Add';

    /** (optional) Placeholder for the key input text */
    @Prop({
        reflect: true
    })
    keyPlaceholder = '';

    /** (optional) Placeholder for the value input text */
    @Prop({
        reflect: true
    })
    valuePlaceholder = '';

    /** (optional) The string to use in the key input label */
    @Prop({
        reflect: true
    })
    keyLabel = 'Key';

    /** (optional) The string to use in the value input label */
    @Prop({
        reflect: true
    })
    valueLabel = 'Value';

    /** (optional) Label for the empty option in white-list select */
    @Prop({
        reflect: true
    })
    emptyDropdownOptionLabel = 'Pick an option';

    /** (optional) The string to use for white-list key/values */
    @Prop({
        reflect: true
    })
    whiteList = '';

    /** Emit the added value, key/value pair */
    @Event()
    add: EventEmitter<DotKeyValueField>;

    /** Emit when key is changed */
    @Event()
    keyChanged: EventEmitter<string>;

    /** Emit when any of the input is blur */
    @Event()
    lostFocus: EventEmitter<FocusEvent>;

    @State()
    inputs: DotKeyValueField = { ...DEFAULT_VALUE };

    @State()
    selectedWhiteListKey = '';

    @Watch('selectedWhiteListKey')
    selectedWhiteListKeyWatch(): void {
        /* */
    }

    private whiteListArray = {};

    componentWillLoad(): void {
        this.whiteListArray = this.whiteList.length ? JSON.parse(this.whiteList) : '';
    }

    render() {
        const buttonDisabled = this.isButtonDisabled();
        return (
            <form onSubmit={this.addKey.bind(this)}>
                <table>
                    <tbody>
                        <tr>
                            <td class="key-value-table-form__key">
                                <label>{this.keyLabel}</label>
                            </td>
                            <td class="key-value-table-form__value">
                                <label>{this.valueLabel}</label>
                            </td>
                            <td class="key-value-table-form__action"></td>
                        </tr>
                        {Object.keys(this.whiteListArray).length === 0
                            ? this.getKeyValueForm(buttonDisabled)
                            : this.getWhiteListForm(buttonDisabled)}
                    </tbody>
                </table>
            </form>
        );
    }

    private getKeyValueForm(buttonDisabled: boolean): JSX.Element {
        return (
            <tr>
                <td class="key-value-table-form__key">
                    <input
                        disabled={this.disabled}
                        name="key"
                        onBlur={(e: FocusEvent) => this.lostFocus.emit(e)}
                        onInput={(event: Event) => this.setValue(event)}
                        placeholder={this.keyPlaceholder}
                        type="text"
                        value={this.inputs.key}
                    />
                </td>
                <td class="key-value-table-form__value">
                    <input
                        disabled={this.disabled}
                        name="value"
                        onBlur={(e: FocusEvent) => this.lostFocus.emit(e)}
                        onInput={(event: Event) => this.setValue(event)}
                        placeholder={this.valuePlaceholder}
                        type="text"
                        value={this.inputs.value}
                    />
                </td>
                <td class="key-value-table-form__action">
                    <button
                        class="key-value-form__save__button"
                        type="submit"
                        disabled={buttonDisabled}>
                        {this.addButtonLabel}
                    </button>
                </td>
            </tr>
        );
    }

    private getWhiteListForm(buttonDisabled: boolean): JSX.Element {
        return (
            <tr>
                <td class="key-value-table-form__key">{this.getWhiteListKeysDropdown()}</td>
                <td class="key-value-table-form__value">
                    {this.selectedWhiteListKey ? this.getWhiteListValueControl() : null}
                </td>
                <td class="key-value-table-form__action">
                    <button
                        class="key-value-form__save__button"
                        type="submit"
                        disabled={buttonDisabled}>
                        {this.addButtonLabel}
                    </button>
                </td>
            </tr>
        );
    }

    private getWhiteListValueControl(): boolean {
        return this.whiteListArray[this.selectedWhiteListKey].length ? (
            this.getWhiteListValuesDropdown()
        ) : (
            <input
                disabled={this.disabled}
                name="value"
                onBlur={(e: FocusEvent) => this.lostFocus.emit(e)}
                onInput={(event: Event) => this.setValue(event)}
                placeholder={this.valuePlaceholder}
                type="text"
                value={this.inputs.value}
            />
        );
    }

    private getWhiteListKeysDropdown(): JSX.Element {
        return (
            <select
                disabled={this.disabled}
                name="key"
                onChange={(event: Event) => this.changeWhiteListKey(event)}>
                <option value="">{this.emptyDropdownOptionLabel}</option>
                {Object.keys(this.whiteListArray).map((key: string) => {
                    return <option value={key}>{key}</option>;
                })}
            </select>
        );
    }

    private getWhiteListValuesDropdown(): JSX.Element {
        return (
            <select
                disabled={this.disabled}
                name="value"
                onChange={(event: Event) => this.changeWhiteListValue(event)}>
                <option value="">{this.emptyDropdownOptionLabel}</option>
                {this.whiteListArray[this.selectedWhiteListKey].map((item: string) => {
                    return <option value={item}>{item}</option>;
                })}
            </select>
        );
    }

    private changeWhiteListKey(event: Event): void {
        event.stopImmediatePropagation();
        this.clearForm();
        const target = event.target as HTMLInputElement;
        this.selectedWhiteListKey = target.value;
        this.setValue(event);
    }

    private changeWhiteListValue(event: Event): void {
        event.stopImmediatePropagation();
        this.setValue(event);
    }

    private isButtonDisabled(): boolean {
        return !this.isFormValid() || this.disabled || null;
    }

    private isFormValid(): boolean {
        return !!(this.inputs.key.length && this.inputs.value.length);
    }

    private setValue(event: Event): void {
        event.stopImmediatePropagation();

        const target = event.target as HTMLInputElement;

        if (target.name === 'key') {
            this.keyChanged.emit(target.value.toString());
        }

        this.inputs = {
            ...this.inputs,
            [target.name]: target.value.toString()
        };
    }

    private addKey(event: Event): void {
        event.preventDefault();
        event.stopImmediatePropagation();

        if (this.inputs.key && this.inputs.value) {
            this.add.emit(this.inputs);
            this.clearForm();
            this.focusKeyInputField();
        }
    }

    private clearForm(): void {
        this.inputs = { ...DEFAULT_VALUE };
    }

    private focusKeyInputField(): void {
        const input: HTMLInputElement = this.el.querySelector('[name="key"]');
        input.focus();
    }
}

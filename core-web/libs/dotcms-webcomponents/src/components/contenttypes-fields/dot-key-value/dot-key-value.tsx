import {
    Component,
    Prop,
    State,
    Element,
    Event,
    EventEmitter,
    Method,
    Listen,
    Watch,
    h,
    Host
} from '@stencil/core';
import {
    DotFieldStatus,
    DotFieldValueEvent,
    DotFieldStatusEvent,
    DotKeyValueField,
    DotOption
} from '../../../models';
import {
    checkProp,
    getClassNames,
    getDotOptionsFromFieldValue,
    getOriginalStatus,
    getJsonStringFromDotKeyArray,
    getTagError,
    getTagHint,
    updateStatus,
    getHintId
} from '../../../utils';

const mapToKeyValue = ({ label, value }: DotOption) => {
    return {
        key: label,
        value
    };
};

@Component({
    tag: 'dot-key-value',
    styleUrl: 'dot-key-value.scss'
})
export class DotKeyValueComponent {
    @Element()
    el: HTMLElement;

    /** Value of the field */
    @Prop({ reflect: true, mutable: true })
    value = '';

    /** Name that will be used as ID */
    @Prop({
        reflect: true
    })
    name = '';

    /** (optional) Text to be rendered next to input field */
    @Prop({
        reflect: true
    })
    label = '';

    /** (optional) Hint text that suggest a clue of the field */
    @Prop({
        reflect: true
    })
    hint = '';

    /** (optional) Determine if it is mandatory */
    @Prop({
        reflect: true
    })
    required = false;

    /** (optional) Text that will be shown when required is set and condition is not met */
    @Prop({
        reflect: true
    })
    requiredMessage = 'This field is required';

    /** (optional) Text that will be shown when required is set and condition is not met */
    @Prop({
        reflect: true
    })
    duplicatedKeyMessage = 'The key already exists';

    /** (optional) Disables field's interaction */
    @Prop({
        reflect: true
    })
    disabled = false;

    /** (optional) Allows unique keys only */
    @Prop({
        reflect: true
    })
    uniqueKeys = false;

    /** (optional) Placeholder for the key input text in the key-value-form */
    @Prop({
        reflect: true
    })
    formKeyPlaceholder: string;

    /** (optional) Placeholder for the value input text in the key-value-form */
    @Prop({
        reflect: true
    })
    formValuePlaceholder: string;

    /** (optional) The string to use in the key label in the key-value-form */
    @Prop({
        reflect: true
    })
    formKeyLabel: string;

    /** (optional) The string to use in the value label in the key-value-form */
    @Prop({
        reflect: true
    })
    formValueLabel: string;

    /** (optional) Label for the add button in the key-value-form */
    @Prop({
        reflect: true
    })
    formAddButtonLabel: string;

    /** (optional) The string to use in the delete button of a key/value item */
    @Prop({
        reflect: true
    })
    listDeleteLabel: string;

    /** (optional) The string to use in the empty option of whitelist dropdown key/value item */
    @Prop({
        reflect: true
    })
    whiteListEmptyOptionLabel: string;

    /** (optional) The string containing the value to be parsed for whitelist key/value */
    @Prop({
        reflect: true
    })
    whiteList: string;

    @State()
    errorExistingKey: boolean;
    @State()
    status: DotFieldStatus;
    @State()
    items: DotKeyValueField[] = [];

    @Event()
    dotValueChange: EventEmitter<DotFieldValueEvent>;
    @Event()
    dotStatusChange: EventEmitter<DotFieldStatusEvent>;

    @Watch('value')
    valueWatch(): void {
        this.value = checkProp<DotKeyValueComponent, string>(this, 'value', 'string');

        let formattedValue = '';
        if (this.value) {
            formattedValue = this.value
                .replace(/&lt;/gi, '<')
                .replace(/[|]/gi, '&#124;')
                .replace(/&#x22;:&#x22;/gi, '|')
                .replace(/&#x22;,&#x22;/gi, ',')
                .replace(/{&#x22;/gi, '')
                .replace(/&#x22;}/gi, '')
                .replace(/&#x22;/gi, '"');
        }

        this.items = getDotOptionsFromFieldValue(formattedValue).map(mapToKeyValue);
    }

    /**
     * Reset properties of the field, clear value and emit events.
     */
    @Method()
    async reset(): Promise<void> {
        this.items = [];
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitChanges();
    }

    @Listen('delete')
    deleteItemHandler(event: CustomEvent<number>) {
        event.stopImmediatePropagation();

        this.items = this.items.filter(
            (_item: DotKeyValueField, index: number) => index !== event.detail
        );
        this.errorExistingKey = false;
        this.refreshStatus();
        this.emitChanges();
    }

    @Listen('reorder')
    reorderItemsHandler(event: CustomEvent) {
        event.stopImmediatePropagation();

        //created with the purpose of only access the DOM of the triggered element

        // Hack to clean the items in DOM without showing "No values" label
        this.items = [{ key: ' ', value: '' }];

        //only manipulate the keys and values of the selected content
        const keys = this.el.querySelectorAll('.key-value-table-wc__key');
        const values = this.el.querySelectorAll('.key-value-table-wc__value');

        let keyValueRawData = '';

        for (let i = 0, total = keys.length; i < total; i++) {
            // Escaping "Comma" and "Pipe" symbols are needed due to format structure designed to separate values
            keyValueRawData += `${keys[i].textContent
                .replace(/,/gi, '&#44;')
                .replace(/[|]/gi, '&#124;')}|${values[i].textContent
                .replace(/,/gi, '&#44;')
                .replace(/[|]/gi, '&#124;')},`;
        }

        // Timeout to let the DOM get cleaned and then repopulate with list of keyValues
        setTimeout(() => {
            this.items = [
                ...getDotOptionsFromFieldValue(
                    keyValueRawData.substring(0, keyValueRawData.length - 1)
                ).map(mapToKeyValue)
            ];
            this.refreshStatus();
            this.emitChanges();
        }, 100);
    }

    @Listen('add')
    addItemHandler({ detail }: CustomEvent<DotKeyValueField>): void {
        this.refreshStatus();

        this.errorExistingKey = this.items.some(
            (item: DotKeyValueField) => item.key === detail.key
        );

        if ((this.uniqueKeys && !this.errorExistingKey) || !this.uniqueKeys) {
            this.items = [...this.items, detail];
            this.emitChanges();
        }
    }

    @Listen('keyChanged')
    keyChangedHandler(): void {
        // Reset errorExistingKey value when KEY is changed
        if (this.errorExistingKey) {
            this.errorExistingKey = false;
        }
    }

    componentWillLoad(): void {
        this.validateProps();
        this.setOriginalStatus();
        this.emitStatusChange();
    }

    render() {
        const classes = getClassNames(
            this.status,
            this.isValid() && !this.errorExistingKey,
            this.required
        );

        return (
            <Host class={{ ...classes }}>
                <dot-label
                    aria-describedby={getHintId(this.hint)}
                    tabIndex={this.hint ? 0 : null}
                    label={this.label}
                    required={this.required}
                    name={this.name}>
                    {!this.disabled ? this.getKeyValueForm() : ''}
                    <key-value-table
                        onClick={(e: MouseEvent) => {
                            e.preventDefault();
                        }}
                        button-label={this.listDeleteLabel}
                        disabled={this.isDisabled()}
                        items={this.items}
                    />
                </dot-label>
                {getTagHint(this.hint)}
                {getTagError(this.showErrorMessage(), this.getErrorMessage())}
            </Host>
        );
    }

    private getKeyValueForm() {
        return (
            <key-value-form
                onLostFocus={this.blurHandler.bind(this)}
                add-button-label={this.formAddButtonLabel}
                disabled={this.isDisabled()}
                empty-dropdown-option-label={this.whiteListEmptyOptionLabel}
                key-label={this.formKeyLabel}
                key-placeholder={this.formKeyPlaceholder}
                value-label={this.formValueLabel}
                value-placeholder={this.formValuePlaceholder}
                white-list={this.whiteList}
            />
        );
    }

    private isDisabled(): boolean {
        return this.disabled || null;
    }

    private blurHandler(): void {
        if (!this.status.dotTouched) {
            this.status = updateStatus(this.status, {
                dotTouched: true
            });
            this.emitStatusChange();
        }
    }

    private validateProps(): void {
        this.valueWatch();
    }

    private setOriginalStatus(): void {
        this.status = getOriginalStatus(this.isValid());
    }

    private isValid(): boolean {
        return !(this.required && !this.items.length);
    }

    private showErrorMessage(): boolean {
        return this.getErrorMessage() && !this.status.dotPristine;
    }

    private getErrorMessage(): string {
        let errorMsg = '';

        if (this.errorExistingKey) {
            errorMsg = this.duplicatedKeyMessage;
        } else if (!this.isValid()) {
            this.requiredMessage;
        }

        return errorMsg;
    }

    private refreshStatus(): void {
        this.status = updateStatus(this.status, {
            dotTouched: true,
            dotPristine: false,
            dotValid: this.isValid()
        });
    }

    private emitStatusChange(): void {
        this.dotStatusChange.emit({
            name: this.name,
            status: this.status
        });
    }

    private emitValueChange(): void {
        const returnedValue = getJsonStringFromDotKeyArray(this.items);
        this.dotValueChange.emit({
            name: this.name,
            value: returnedValue
        });
    }

    private emitChanges(): void {
        this.emitStatusChange();
        this.emitValueChange();
    }
}

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
    Host,
    h
} from '@stencil/core';
import {
    DotFieldStatus,
    DotFieldValueEvent,
    DotFieldStatusEvent,
    DotKeyValueField,
    DotOption
} from '../../models';
import {
    checkProp,
    getClassNames,
    getDotOptionsFromFieldValue,
    getOriginalStatus,
    getStringFromDotKeyArray,
    getTagError,
    getTagHint,
    updateStatus,
    getHintId
} from '../../utils';

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
    @Element() el: HTMLElement;

    /** (optional) Placeholder for the key input text in the <key-value-form> */
    @Prop({
        reflect: true
    })
    formKeyPlaceholder: string;

    /** (optional) Placeholder for the value input text in the <key-value-form> */
    @Prop({
        reflect: true
    })
    formValuePlaceholder: string;

    /** (optional) The string to use in the key label in the <key-value-form> */
    @Prop({
        reflect: true
    })
    formKeyLabel: string;

    /** (optional) The string to use in the value label in the <key-value-form> */
    @Prop({
        reflect: true
    })
    formValueLabel: string;

    /** (optional) Label for the add button in the <key-value-form> */
    @Prop({
        reflect: true
    })
    formAddButtonLabel: string;

    /** (optional) The string to use in the delete button of a key/value item */
    @Prop({
        reflect: true
    })
    listDeleteLabel: string;

    /** (optional) Disables field's interaction */
    @Prop({
        reflect: true
    })
    disabled = false;

    /** (optional) Hint text that suggest a clue of the field */
    @Prop({
        reflect: true
    })
    hint = '';

    /** (optional) Text to be rendered next to input field */
    @Prop({
        reflect: true
    })
    label = '';

    /** Name that will be used as ID */
    @Prop({
        reflect: true
    })
    name = '';

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

    /** Value of the field */
    @Prop({ reflect: true, mutable: true }) value = '';

    @State() status: DotFieldStatus;
    @State() items: DotKeyValueField[] = [];

    @Event() valueChange: EventEmitter<DotFieldValueEvent>;
    @Event() statusChange: EventEmitter<DotFieldStatusEvent>;

    @Watch('value')
    valueWatch(): void {
        this.value = checkProp<DotKeyValueComponent, string>(this, 'value', 'string');
        this.items = getDotOptionsFromFieldValue(this.value).map(mapToKeyValue);
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
        this.refreshStatus();
        this.emitChanges();
    }

    @Listen('add')
    addItemHandler({ detail }: CustomEvent<DotKeyValueField>): void {
        this.items = [...this.items, detail];
        this.refreshStatus();
        this.emitChanges();
    }

    componentWillLoad(): void {
        this.validateProps();
        this.setOriginalStatus();
        this.emitStatusChange();
    }

    render() {
        const classes = getClassNames(this.status, this.isValid(), this.required);

        return (
            <Host class={{ ...classes }}>
                <dot-label
                    aria-describedby={getHintId(this.hint)}
                    tabIndex={this.hint ? 0 : null}
                    label={this.label}
                    required={this.required}
                    name={this.name}>
                    <key-value-form
                        onLostFocus={this.blurHandler.bind(this)}
                        add-button-label={this.formAddButtonLabel}
                        disabled={this.isDisabled()}
                        key-label={this.formKeyLabel}
                        key-placeholder={this.formKeyPlaceholder}
                        value-label={this.formValueLabel}
                        value-placeholder={this.formValuePlaceholder}
                    />
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
        return this.isValid() ? '' : this.requiredMessage;
    }

    private refreshStatus(): void {
        this.status = updateStatus(this.status, {
            dotTouched: true,
            dotPristine: false,
            dotValid: this.isValid()
        });
    }

    private emitStatusChange(): void {
        this.statusChange.emit({
            name: this.name,
            status: this.status
        });
    }

    private emitValueChange(): void {
        const returnedValue = getStringFromDotKeyArray(this.items);
        this.valueChange.emit({
            name: this.name,
            value: returnedValue
        });
    }

    private emitChanges(): void {
        this.emitStatusChange();
        this.emitValueChange();
    }
}

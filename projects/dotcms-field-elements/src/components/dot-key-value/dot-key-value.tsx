import {
    Component,
    Prop,
    State,
    Element,
    Event,
    EventEmitter,
    Method,
    Listen,
    Watch
} from '@stencil/core';
import Fragment from 'stencil-fragment';
import {
    DotFieldStatus,
    DotFieldValueEvent,
    DotFieldStatusEvent,
    DotLabel,
    DotKeyValueField as DotKeyValueItem
} from '../../models';
import {
    getClassNames,
    getErrorClass,
    getId,
    getOriginalStatus,
    getStringFromDotKeyArray,
    getTagError,
    getTagHint,
    getTagLabel,
    updateStatus,
    checkProp
} from '../../utils';

@Component({
    tag: 'dot-key-value',
    styleUrl: 'dot-key-value.scss'
})
export class DotKeyValueComponent {
    @Element() el: HTMLElement;

    /** (optional) Disables field's interaction */
    @Prop() disabled = false;

    /** (optional) Hint text that suggest a clue of the field */
    @Prop() hint = '';

    /** (optional) Placeholder for the key input text in the add form */
    @Prop() keyPlaceholder = '';

    /** (optional) Text to be rendered next to input field */
    @Prop() label = '';

    /** Name that will be used as ID */
    @Prop() name = '';

    /** (optional) Determine if it is mandatory */
    @Prop() required = false;

    /** (optional) Text that will be shown when required is set and condition is not met */
    @Prop() requiredMessage = '';

    /** (optional) Label for the add item button */
    @Prop() saveBtnLabel = 'Add';

    /** (optional) Placeholder for the value input text in the add form */
    @Prop() valuePlaceholder = '';

    /** Value of the field */
    @Prop({ mutable: true }) value = '';

    @Prop() fieldType = ''; // TODO: remove this prop and fix dot-form to use tagName

    @State() status: DotFieldStatus;
    @State() items: DotKeyValueItem[] = [];

    @Event() valueChange: EventEmitter<DotFieldValueEvent>;
    @Event() statusChange: EventEmitter<DotFieldStatusEvent>;

    fieldInput: DotKeyValueItem = { key: '', value: '' };

    @Watch('value')
    valueWatch(): void {
        this.value = checkProp<DotKeyValueComponent, string>(this, 'value', 'string');
        this.setItems();
    }

    /**
     * Reset properties of the field, clear value and emit events.
     */
    @Method()
    reset(): void {
        this.fieldInput = { key: '', value: '' };
        this.items = [];
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
        this.emitValueChange();
    }

    @Listen('deleteItemEvt')
    deleteItemHandler(event: CustomEvent) {
        event.stopImmediatePropagation();
        this.items = this.items.filter((_item, internalIndex) => {
            return internalIndex !== event.detail;
        });
        this.refreshStatus();
        this.emitStatusChange();
        this.emitValueChange();
    }

    componentWillLoad(): void {
        this.validateProps();
        this.setOriginalStatus();
        this.emitStatusChange();
    }

    hostData() {
        return {
            class: getClassNames(this.status, this.isValid(), this.required)
        };
    }

    render() {
        const labelTagParams: DotLabel = {
            name: this.name,
            label: this.label,
            required: this.required
        };
        return (
            <Fragment>
                {getTagLabel(labelTagParams)}
                <input
                    class={getErrorClass(this.status.dotValid)}
                    disabled={this.isDisabled()}
                    id={getId(this.name)}
                    name="key"
                    onInput={(event: Event) => this.setValue(event)}
                    placeholder={this.keyPlaceholder}
                    type="text"
                    value={this.fieldInput.key}
                />
                <input
                    class={getErrorClass(this.status.dotValid)}
                    disabled={this.isDisabled()}
                    name="value"
                    onInput={(event: Event) => this.setValue(event)}
                    placeholder={this.valuePlaceholder}
                    type="text"
                    value={this.fieldInput.value}
                />
                <button
                    class="dot-key-value__save__button"
                    type="button"
                    disabled={this.disabled || null}
                    onClick={() => this.addKey()}
                >
                    {this.saveBtnLabel}
                </button>
                {this.getKeyValueList()}
                {getTagHint(this.hint)}
                {getTagError(this.showErrorMessage(), this.getErrorMessage())}
            </Fragment>
        );
    }

    private validateProps(): void {
        this.valueWatch();
    }

    private isDisabled(): boolean {
        return this.disabled || null;
    }

    private getKeyValueList(): JSX.Element {
        return this.items.length ? (
            <key-value-table items={this.items} disabled={this.disabled} />
        ) : null;
    }

    private setItems(): void {
        this.items = this.value
            ? this.value
                  .split(',')
                  .filter((item) => item.length > 0)
                  .map((item) => {
                      const [key, value] = item.split('|');
                      return { key, value };
                  })
            : [];
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

    private setValue(event): void {
        this.fieldInput[event.target.name] = event.target.value.toString();
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
            value: returnedValue,
            fieldType: this.fieldType
        });
    }

    private addKey(): void {
        if (this.fieldInput.key && this.fieldInput.value) {
            this.items = [
                ...this.items,
                {
                    key: this.fieldInput.key,
                    value: this.fieldInput.value
                }
            ];
            this.refreshStatus();
            this.emitStatusChange();
            this.emitValueChange();
        }
    }
}

import {
    Component,
    Prop,
    State,
    Element,
    Event,
    EventEmitter,
    Method,
    Listen
} from '@stencil/core';
import Fragment from 'stencil-fragment';
import { DotFieldStatus, DotFieldValueEvent, DotFieldStatusEvent, DotLabel, DotKeyValueField } from '../../models';
import {
    getClassNames,
    getErrorClass,
    getId,
    getOriginalStatus,
    getStringFromDotKeyArray,
    getTagError,
    getTagHint,
    getTagLabel,
    updateStatus
} from '../../utils';

@Component({
    tag: 'dot-key-value',
    styleUrl: 'dot-key-value.scss'
})
export class DotKeyValueComponent {
    @Element() el: HTMLElement;
    @Prop({ mutable: true }) value: string;
    @Prop() name: string;
    @Prop() fieldType: string;
    @Prop() label: string;
    @Prop() hint: string;
    @Prop() keyPlaceholder: string;
    @Prop() valuePlaceholder: string;
    @Prop() required: boolean;
    @Prop() requiredMessage: string;
    @Prop() saveBtnLabel = 'Add';
    @Prop() disabled = false;

    @State() status: DotFieldStatus;
    @State() values: DotKeyValueField[] = [];

    @Event() valueChange: EventEmitter<DotFieldValueEvent>;
    @Event() statusChange: EventEmitter<DotFieldStatusEvent>;

    fieldInput: DotKeyValueField = { key: '', value: '' };

    /**
     * Reset properties of the field, clear value and emit events.
     */
    @Method()
    reset(): void {
        this.fieldInput = { key: '', value: '' };
        this.values = [];
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
        this.emitValueChange();
    }

    @Listen('deleteItemEvt')
    deleteItemHandler(event: CustomEvent) {
        event.stopImmediatePropagation();
        this.values = this.values.filter((_item, internalIndex) => {
            return internalIndex !== event.detail;
        });
        this.refreshStatus();
        this.emitStatusChange();
        this.emitValueChange();
    }

    componentWillLoad(): void {
        this.setInitialValue();
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

    private isDisabled(): boolean {
        return this.disabled || null;
    }

    private getKeyValueList(): JSX.Element {
        return this.values.length ? (
            <key-value-table items={this.values} disabled={this.disabled} />
        ) : (
            ''
        );
    }

    private setInitialValue(): void {
        this.values = this.value
            ? this.value
                  .split(',')
                  .filter((item) => item.length > 0)
                  .map((item) => {
                      const [key, value] = item.split('|');
                      return { key, value };
                  })
            : [];
        this.status = getOriginalStatus(this.isValid());
    }

    private isValid(): boolean {
        return !(this.required && !this.values.length);
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
        const returnedValue = getStringFromDotKeyArray(this.values);
        this.valueChange.emit({
            name: this.name,
            value: returnedValue,
            fieldType: this.fieldType
        });
    }

    private addKey(): void {
        if (this.fieldInput.key && this.fieldInput.value) {
            this.values = [
                ...this.values,
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

import { Component, Prop, State, Element, Method, Event, EventEmitter, Watch } from '@stencil/core';
import Fragment from 'stencil-fragment';
import {
    DotOption,
    DotFieldStatus,
    DotFieldValueEvent,
    DotFieldStatusEvent,
    DotLabel
} from '../../models';
import {
    getClassNames,
    getOriginalStatus,
    getTagHint,
    getTagError,
    getTagLabel,
    getErrorClass,
    getDotOptionsFromFieldValue,
    updateStatus,
    checkProp
} from '../../utils';

@Component({
    tag: 'dot-checkbox',
    styleUrl: 'dot-checkbox.scss'
})
export class DotCheckboxComponent {
    @Element() el: HTMLElement;

    /** (optional) Disables field's interaction */
    @Prop() disabled = false;

    /** Name that will be used as ID */
    @Prop() name = '';

    /** (optional) Text to be rendered next to input field */
    @Prop() label = '';

    /** (optional) Hint text that suggest a clue of the field */
    @Prop() hint = '';

    /** Value/Label checkbox options separated by comma, to be formatted as: Value|Label */
    @Prop() options = '';

    /** (optional) Determine if it is mandatory */
    @Prop() required = false;

    /** (optional) Text that will be shown when required is set and condition is not met */
    @Prop() requiredMessage = '';

     /** Value set from the checkbox option */
    @Prop({ mutable: true }) value = '';

    @State() _options: DotOption[];
    @State() status: DotFieldStatus = getOriginalStatus();

    @Event() valueChange: EventEmitter<DotFieldValueEvent>;
    @Event() statusChange: EventEmitter<DotFieldStatusEvent>;

    componentWillLoad() {
        this.validateProps();
        this.emitValueChange();
        this.emitStatusChange();
    }

    @Watch('options')
    optionsWatch(): void {
        const validOptions = checkProp<DotCheckboxComponent, string>(this, 'options');
        this._options = getDotOptionsFromFieldValue(validOptions);
    }

    hostData() {
        return {
            class: getClassNames(this.status, this.isValid(), this.required)
        };
    }

    /**
     * Reset properties of the field, clear value and emit events.
     *
     * @memberof DotSelectComponent
     */
    @Method()
    reset(): void {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitValueChange();
        this.emitStatusChange();
    }

    render() {
        let labelTagParams: DotLabel = {
            name: this.name,
            label: this.label,
            required: this.required
        };
        return (
            <Fragment>
                {getTagLabel(labelTagParams)}
                {this._options.map((item: DotOption) => {
                    const trimmedValue = item.value.trim();
                    labelTagParams = { name: trimmedValue, label: item.label };
                    return (
                        <Fragment>
                            <input
                                class={getErrorClass(this.isValid())}
                                type="checkbox"
                                disabled={this.disabled || null}
                                id={trimmedValue}
                                checked={this.value.indexOf(trimmedValue) >= 0 || null}
                                onInput={(event: Event) => this.setValue(event)}
                                value={trimmedValue}
                            />
                            {getTagLabel(labelTagParams)}
                        </Fragment>
                    );
                })}
                {getTagHint(this.hint)}
                {getTagError(!this.isValid(), this.requiredMessage)}
            </Fragment>
        );
    }

    private validateProps(): void {
        this.optionsWatch();
    }

    // Todo: find how to set proper TYPE in TS
    private setValue(event): void {
        this.value = this.getValueFromCheckInputs(event.target.value.trim(), event.target.checked);
        this.status = updateStatus(this.status, {
            dotTouched: true,
            dotPristine: false,
            dotValid: this.isValid()
        });
        this.emitValueChange();
        this.emitStatusChange();
    }

    private getValueFromCheckInputs(value: string, checked: boolean): string {
        const valueArray = this.value.trim().length ? this.value.split(',') : [];
        const valuesSet = new Set(valueArray);
        if (checked) {
            valuesSet.add(value);
        } else {
            valuesSet.delete(value);
        }
        return Array.from(valuesSet).join(',');
    }

    private emitStatusChange(): void {
        this.statusChange.emit({
            name: this.name,
            status: this.status
        });
    }

    private isValid(): boolean {
        return this.required ? !!this.value : true;
    }

    private emitValueChange(): void {
        this.valueChange.emit({
            name: this.name,
            value: this.value
        });
    }
}

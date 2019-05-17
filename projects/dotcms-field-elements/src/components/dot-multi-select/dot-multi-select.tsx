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
    getDotOptionsFromFieldValue,
    getErrorClass,
    getId,
    getOriginalStatus,
    getTagError,
    getTagHint,
    getTagLabel,
    updateStatus,
    checkProp
} from '../../utils';

/**
 * Represent a dotcms multi select control.
 *
 * @export
 * @class DotSelectComponent
 */
@Component({
    tag: 'dot-multi-select',
    styleUrl: 'dot-multi-select.scss'
})
export class DotMultiSelectComponent {
    @Element() el: HTMLElement;

    /** (optional) Disables field's interaction */
    @Prop() disabled = false;

    /** Name that will be used as ID */
    @Prop() name = '';

    /** (optional) Text to be rendered next to input field */
    @Prop() label = '';

    /** (optional) Hint text that suggest a clue of the field */
    @Prop() hint = '';

    /** Value/Label dropdown options separated by comma, to be formatted as: Value|Label */
    @Prop() options = '';

    /** (optional) Determine if it is mandatory */
    @Prop() required = false;

    /** (optional) Text that will be shown when required is set and condition is not met */
    @Prop() requiredMessage = '';

    /** (optional) Size number of the multi-select dropdown (default=3) */
    @Prop() size = 3;

    /** Value set from the dropdown option */
    @Prop({ mutable: true }) value = '';

    @State() _options: DotOption[];
    @State() status: DotFieldStatus = getOriginalStatus();

    @Event() valueChange: EventEmitter<DotFieldValueEvent>;
    @Event() statusChange: EventEmitter<DotFieldStatusEvent>;

    _dotTouched = false;
    _dotPristine = true;

    componentWillLoad() {
        this.validateProps();
        this.emitInitialValue();
        this.emitStatusChange();
    }

    @Watch('options')
    optionsWatch(): void {
        const validOptions = checkProp<DotMultiSelectComponent, string>(
            this,
            'options'
        );
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
     *
     */
    @Method()
    reset(): void {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitInitialValue();
        this.emitStatusChange();
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
                <select
                    multiple
                    size={+this.size}
                    class={getErrorClass(this.status.dotValid)}
                    id={getId(this.name)}
                    disabled={this.shouldBeDisabled()}
                    onChange={() => this.setValue()}
                >
                    {this._options.map((item: DotOption) => {
                        return (
                            <option
                                selected={this.value === item.value ? true : null}
                                value={item.value}
                            >
                                {item.label}
                            </option>
                        );
                    })}
                </select>
                {getTagHint(this.hint)}
                {getTagError(!this.isValid(), this.requiredMessage)}
            </Fragment>
        );
    }

    private validateProps(): void {
        this.optionsWatch();
    }

    private shouldBeDisabled(): boolean {
        return this.disabled ? true : null;
    }

    // Todo: find how to set proper TYPE in TS
    private setValue(): void {
        this.value = this.getValueFromMultiSelect();
        this.status = updateStatus(this.status, {
            dotTouched: true,
            dotPristine: false,
            dotValid: this.isValid()
        });
        this.emitValueChange();
        this.emitStatusChange();
    }

    private getValueFromMultiSelect(): string {
        const selected = this.el.querySelectorAll('option:checked');
        const values = Array.from(selected).map((el: any) => el.value);
        return Array.from(values).join(',');
    }

    private emitInitialValue() {
        if (!this.value) {
            this.value = this._options.length ? this._options[0].value : '';
            this.emitValueChange();
        }
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

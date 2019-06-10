import { Component, Element, Event, EventEmitter, Method, Prop, State, Watch } from '@stencil/core';
import Fragment from 'stencil-fragment';
import { DotFieldStatus, DotFieldStatusEvent, DotFieldValueEvent, DotOption } from '../../models';
import {
    getClassNames,
    getDotOptionsFromFieldValue,
    getErrorClass,
    getOriginalStatus,
    getTagError,
    getTagHint,
    updateStatus,
    checkProp,
    getId
} from '../../utils';

/**
 * Represent a dotcms radio control.
 *
 * @export
 * @class DotRadioComponent
 */
@Component({
    tag: 'dot-radio',
    styleUrl: 'dot-radio.scss'
})
export class DotRadioComponent {
    @Element() el: HTMLElement;

    /** Value set from the ratio option */
    @Prop({ mutable: true, reflectToAttr: true }) value = '';

    /** Name that will be used as ID */
    @Prop({ reflectToAttr: true }) name = '';

    /** (optional) Text to be rendered next to input field */
    @Prop({ reflectToAttr: true }) label = '';

    /** (optional) Hint text that suggest a clue of the field */
    @Prop({ reflectToAttr: true }) hint = '';

    /** (optional) Determine if it is mandatory */
    @Prop({ reflectToAttr: true }) required = false;

    /** (optional) Disables field's interaction */
    @Prop({ reflectToAttr: true }) disabled = false;

    /** (optional) Text that will be shown when required is set and condition is not met */
    @Prop({ reflectToAttr: true }) requiredMessage = '';

    /** Value/Label ratio options separated by comma, to be formatted as: Value|Label */
    @Prop({ reflectToAttr: true }) options = '';

    @State() _options: DotOption[];
    @State() status: DotFieldStatus;

    @Event() valueChange: EventEmitter<DotFieldValueEvent>;
    @Event() statusChange: EventEmitter<DotFieldStatusEvent>;

    /**
     * Reset properties of the field, clear value and emit events.
     */
    @Method()
    reset(): void {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
        this.emitValueChange();
    }

    componentWillLoad(): void {
        this.validateProps();
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
    }

    @Watch('options')
    optionsWatch(): void {
        const validOptions = checkProp<DotRadioComponent, string>(this, 'options');
        this._options = getDotOptionsFromFieldValue(validOptions);
    }

    hostData() {
        return {
            class: getClassNames(this.status, this.isValid(), this.required)
        };
    }

    render() {
        return (
            <Fragment>
                <dot-label label={this.label} required={this.required} name={this.name}>
                    <div class="dot-radio__items">
                        {this._options.map((item: DotOption) => {
                            return (
                                <label>
                                    <input
                                        checked={this.value.indexOf(item.value) >= 0 || null}
                                        class={getErrorClass(this.isValid())}
                                        id={getId(this.name)}
                                        disabled={this.disabled || null}
                                        onInput={(event: Event) => this.setValue(event)}
                                        type="radio"
                                        value={item.value}
                                    />
                                    {item.label}
                                </label>
                            );
                        })}
                    </div>
                </dot-label>
                {getTagHint(this.hint)}
                {getTagError(this.showErrorMessage(), this.getErrorMessage())}
            </Fragment>
        );
    }

    private validateProps(): void {
        this.optionsWatch();
    }

    private isValid(): boolean {
        return this.required ? !!this.value : true;
    }

    private showErrorMessage(): boolean {
        return this.getErrorMessage() && !this.status.dotPristine;
    }

    private getErrorMessage(): string {
        return this.isValid() ? '' : this.requiredMessage;
    }

    private setValue(event): void {
        this.value = event.target.value.trim();
        this.status = updateStatus(this.status, {
            dotTouched: true,
            dotPristine: false,
            dotValid: this.isValid()
        });
        this.emitValueChange();
        this.emitStatusChange();
    }

    private emitStatusChange(): void {
        this.statusChange.emit({
            name: this.name,
            status: this.status
        });
    }

    private emitValueChange(): void {
        this.valueChange.emit({
            name: this.name,
            value: this.value
        });
    }
}

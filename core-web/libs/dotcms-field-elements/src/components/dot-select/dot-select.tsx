import {
    Component,
    Prop,
    State,
    Element,
    Method,
    Event,
    EventEmitter,
    Watch,
    Host,
    h
} from '@stencil/core';
import { DotOption, DotFieldStatus, DotFieldValueEvent, DotFieldStatusEvent } from '../../models';
import {
    checkProp,
    getClassNames,
    getDotOptionsFromFieldValue,
    getErrorClass,
    getId,
    getOriginalStatus,
    getTagError,
    getTagHint,
    updateStatus,
    getHintId
} from '../../utils';
import { getDotAttributesFromElement, setDotAttributesToElement } from '../dot-form/utils';

/**
 * Represent a dotcms select control.
 *
 * @export
 * @class DotSelectComponent
 */
@Component({
    tag: 'dot-select',
    styleUrl: 'dot-select.scss'
})
export class DotSelectComponent {
    @Element() el: HTMLElement;

    /** (optional) Disables field's interaction */
    @Prop({ reflect: true }) disabled = false;

    /** Name that will be used as ID */
    @Prop({ reflect: true }) name = '';

    /** (optional) Text to be rendered next to input field */
    @Prop({ reflect: true }) label = '';

    /** (optional) Hint text that suggest a clue of the field */
    @Prop({ reflect: true }) hint = '';

    /** Value/Label dropdown options separated by comma, to be formatted as: Value|Label */
    @Prop({ reflect: true }) options = '';

    /** (optional) Determine if it is mandatory */
    @Prop({ reflect: true }) required = false;

    /** (optional) Text that will be shown when required is set and condition is not met */
    @Prop({ reflect: true }) requiredMessage = `This field is required`;

    /** Value set from the dropdown option */
    @Prop({ mutable: true, reflect: true }) value = '';

    @State() _options: DotOption[];
    @State() status: DotFieldStatus;

    @Event() valueChange: EventEmitter<DotFieldValueEvent>;
    @Event() statusChange: EventEmitter<DotFieldStatusEvent>;

    _dotTouched = false;
    _dotPristine = true;

    componentWillLoad() {
        this.validateProps();
        this.emitInitialValue();
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
    }

    @Watch('options')
    optionsWatch(): void {
        const validOptions = checkProp<DotSelectComponent, string>(this, 'options');
        this._options = getDotOptionsFromFieldValue(validOptions);
    }

    /**
     * Reset properties of the field, clear value and emit events.
     *
     * @memberof DotSelectComponent
     *
     */
    @Method()
    async reset(): Promise<void> {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitInitialValue();
        this.emitStatusChange();
    }

    componentDidLoad(): void {
        const htmlElement = this.el.querySelector('select');
        setTimeout(() => {
            const attrs = getDotAttributesFromElement(Array.from(this.el.attributes), []);
            setDotAttributesToElement(htmlElement, attrs);
        }, 0);
    }

    render() {
        const classes = getClassNames(this.status, this.isValid(), this.required);

        return (
            <Host class={{ ...classes }}>
                <dot-label label={this.label} required={this.required} name={this.name}>
                    <select
                        aria-describedby={getHintId(this.hint)}
                        class={getErrorClass(this.status.dotValid)}
                        id={getId(this.name)}
                        disabled={this.shouldBeDisabled()}
                        onChange={(event: Event) => this.setValue(event)}>
                        {this._options.map((item: DotOption) => {
                            return (
                                <option
                                    selected={this.value === item.value ? true : null}
                                    value={item.value}>
                                    {item.label}
                                </option>
                            );
                        })}
                    </select>
                </dot-label>
                {getTagHint(this.hint)}
                {getTagError(!this.isValid(), this.requiredMessage)}
            </Host>
        );
    }

    private validateProps(): void {
        this.optionsWatch();
    }

    private shouldBeDisabled(): boolean {
        return this.disabled ? true : null;
    }

    // Todo: find how to set proper TYPE in TS
    private setValue(event): void {
        this.value = event.target.value;
        this.status = updateStatus(this.status, {
            dotTouched: true,
            dotPristine: false,
            dotValid: this.isValid()
        });
        this.emitValueChange();
        this.emitStatusChange();
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

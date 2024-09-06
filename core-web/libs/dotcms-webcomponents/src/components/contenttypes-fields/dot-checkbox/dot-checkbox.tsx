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
import {
    DotOption,
    DotFieldStatus,
    DotFieldValueEvent,
    DotFieldStatusEvent
} from '../../../models';
import {
    getClassNames,
    getOriginalStatus,
    getTagHint,
    getTagError,
    getErrorClass,
    getDotOptionsFromFieldValue,
    updateStatus,
    checkProp,
    getId,
    getHintId
} from '../../../utils';
import { getDotAttributesFromElement, setDotAttributesToElement } from '../dot-form/utils';

@Component({
    tag: 'dot-checkbox',
    styleUrl: 'dot-checkbox.scss'
})
export class DotCheckboxComponent {
    @Element()
    el: HTMLElement;

    /** Name that will be used as ID */
    @Prop({ reflect: true })
    name = '';

    /** (optional) Text to be rendered next to input field */
    @Prop({ reflect: true })
    label = '';

    /** (optional) Hint text that suggest a clue of the field */
    @Prop({ reflect: true })
    hint = '';

    /** Value/Label checkbox options separated by comma, to be formatted as: Value|Label */
    @Prop({ reflect: true })
    options = '';

    /** (optional) Determine if it is mandatory */
    @Prop({ reflect: true })
    required = false;

    /** (optional) Disables field's interaction */
    @Prop({ reflect: true, mutable: true })
    disabled = false;

    /** (optional) Text that will be shown when required is set and condition is not met */
    @Prop({ reflect: true })
    requiredMessage = `This field is required`;

    /** Value set from the checkbox option */
    @Prop({ mutable: true, reflect: true })
    value = '';

    @State()
    _options: DotOption[];
    @State()
    status: DotFieldStatus;

    @Event()
    dotValueChange: EventEmitter<DotFieldValueEvent>;
    @Event()
    dotStatusChange: EventEmitter<DotFieldStatusEvent>;

    componentWillLoad() {
        this.value = this.value || '';
        this.validateProps();
        this.emitValueChange();
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
    }

    componentDidLoad(): void {
        const attrException = ['dottype'];
        const htmlElements = this.el.querySelectorAll('input[type="checkbox"]');
        setTimeout(() => {
            const attrs = getDotAttributesFromElement(
                Array.from(this.el.attributes),
                attrException
            );
            htmlElements.forEach((htmlElement: Element) => {
                setDotAttributesToElement(htmlElement, attrs);
            });
        }, 0);
    }

    @Watch('options')
    optionsWatch(): void {
        const validOptions = checkProp<DotCheckboxComponent, string>(this, 'options');
        this._options = getDotOptionsFromFieldValue(validOptions);
    }

    @Watch('value')
    valueWatch() {
        this.value = this.value || '';
    }

    /**
     * Reset properties of the field, clear value and emit events.
     *
     * @memberof DotSelectComponent
     */
    @Method()
    async reset(): Promise<void> {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitValueChange();
        this.emitStatusChange();
    }

    render() {
        const classes = getClassNames(this.status, this.isValid(), this.required);

        return (
            <Host class={{ ...classes }}>
                <dot-label label={this.label} required={this.required} name={this.name}>
                    <div
                        aria-describedby={getHintId(this.hint)}
                        tabIndex={this.hint ? 0 : null}
                        class="dot-checkbox__items">
                        {this._options.map((item: DotOption) => {
                            const trimmedValue = item.value.trim();
                            return (
                                <label>
                                    <input
                                        class={getErrorClass(this.isValid())}
                                        name={getId(this.name)}
                                        type="checkbox"
                                        disabled={this.disabled || null}
                                        checked={this.value.indexOf(trimmedValue) >= 0 || null}
                                        onInput={(event: Event) => this.setValue(event)}
                                        value={trimmedValue}
                                    />
                                    {item.label}
                                </label>
                            );
                        })}
                    </div>
                </dot-label>
                {getTagHint(this.hint)}
                {getTagError(!this.isValid(), this.requiredMessage)}
            </Host>
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
        this.dotStatusChange.emit({
            name: this.name,
            status: this.status
        });
    }

    private isValid(): boolean {
        return this.required ? !!this.value : true;
    }

    private emitValueChange(): void {
        this.dotValueChange.emit({
            name: this.name,
            value: this.value
        });
    }
}

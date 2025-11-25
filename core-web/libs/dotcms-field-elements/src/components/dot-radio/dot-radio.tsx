import {
    Component,
    Element,
    Event,
    EventEmitter,
    Method,
    Prop,
    State,
    Watch,
    Host,
    h
} from '@stencil/core';
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
    getId,
    getHintId
} from '../../utils';
import { getDotAttributesFromElement, setDotAttributesToElement } from '../dot-form/utils';

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
    @Prop({ mutable: true, reflect: true }) value = '';

    /** Name that will be used as ID */
    @Prop({ reflect: true }) name = '';

    /** (optional) Text to be rendered next to input field */
    @Prop({ reflect: true }) label = '';

    /** (optional) Hint text that suggest a clue of the field */
    @Prop({ reflect: true }) hint = '';

    /** (optional) Determine if it is mandatory */
    @Prop({ reflect: true }) required = false;

    /** (optional) Disables field's interaction */
    @Prop({ reflect: true, mutable: true }) disabled = false;

    /** (optional) Text that will be shown when required is set and condition is not met */
    @Prop({ reflect: true }) requiredMessage = '';

    /** Value/Label ratio options separated by comma, to be formatted as: Value|Label */
    @Prop({ reflect: true }) options = '';

    @State() _options: DotOption[];
    @State() status: DotFieldStatus;

    @Event() valueChange: EventEmitter<DotFieldValueEvent>;
    @Event() statusChange: EventEmitter<DotFieldStatusEvent>;

    /**
     * Reset properties of the field, clear value and emit events.
     */
    @Method()
    async reset(): Promise<void> {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
        this.emitValueChange();
    }

    componentWillLoad(): void {
        this.value = this.value || '';
        this.validateProps();
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
    }

    componentDidLoad(): void {
        const attrException = ['dottype'];
        const htmlElements = this.el.querySelectorAll('input[type="radio"]');
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
        const validOptions = checkProp<DotRadioComponent, string>(this, 'options');
        this._options = getDotOptionsFromFieldValue(validOptions);
    }

    @Watch('value')
    valueWatch() {
        this.value = this.value || '';
    }

    render() {
        const classes = getClassNames(this.status, this.isValid(), this.required);

        return (
            <Host class={{ ...classes }}>
                <dot-label label={this.label} required={this.required} name={this.name}>
                    <div
                        class="dot-radio__items"
                        aria-describedby={getHintId(this.hint)}
                        tabIndex={this.hint ? 0 : null}
                        role="radiogroup">
                        {this._options.map((item: DotOption) => {
                            item.value = item.value.trim();
                            return (
                                <label>
                                    <input
                                        checked={this.value.indexOf(item.value) >= 0 || null}
                                        class={getErrorClass(this.isValid())}
                                        name={getId(this.name)}
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
            </Host>
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

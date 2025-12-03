import {
    Component,
    Prop,
    State,
    Element,
    Event,
    EventEmitter,
    Method,
    Watch,
    Host,
    h
} from '@stencil/core';

import { DotFieldStatus, DotFieldValueEvent, DotFieldStatusEvent } from '../../models';
import {
    checkProp,
    getClassNames,
    getErrorClass,
    getId,
    getOriginalStatus,
    getTagError,
    getTagHint,
    updateStatus,
    getHintId
} from '../../utils';
import { setDotAttributesToElement, getDotAttributesFromElement } from '../dot-form/utils';

/**
 * Represent a dotcms input control.
 *
 * @export
 * @class DotTextfieldComponent
 */
@Component({
    tag: 'dot-textfield',
    styleUrl: 'dot-textfield.scss'
})
export class DotTextfieldComponent {
    @Element() el: HTMLElement;

    /** Value specifies the value of the <input> element */
    @Prop({ mutable: true })
    value = '';

    /** Name that will be used as ID */
    @Prop() name = '';

    /** (optional) Text to be rendered next to input field */
    @Prop({ reflect: true })
    label = '';

    /** (optional) Placeholder specifies a short hint that describes the expected value of the input field */
    @Prop({ reflect: true, mutable: true })
    placeholder = '';

    /** (optional) Hint text that suggest a clue of the field */
    @Prop({ reflect: true })
    hint = '';

    /** (optional) Determine if it is mandatory */
    @Prop({ mutable: true, reflect: true })
    required = false;

    /** (optional) Text that be shown when required is set and condition not met */
    @Prop() requiredMessage = 'This field is required';

    /** (optional) Text that be shown when the Regular Expression condition not met */
    @Prop() validationMessage = "The field doesn't comply with the specified format";

    /** (optional) Disables field's interaction */
    @Prop({ mutable: true, reflect: true })
    disabled = false;

    /** (optional) Regular expresion that is checked against the value to determine if is valid  */
    @Prop({ mutable: true, reflect: true })
    regexCheck = '';

    /** type specifies the type of <input> element to display */
    @Prop({ mutable: true, reflect: true })
    type = 'text';

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
        this.validateProps();
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
    }

    componentDidLoad(): void {
        const htmlElement = this.el.querySelector('input');
        setTimeout(() => {
            const attrs = getDotAttributesFromElement(Array.from(this.el.attributes), []);
            setDotAttributesToElement(htmlElement, attrs);
        }, 0);
    }

    @Watch('regexCheck')
    regexCheckWatch(): void {
        this.regexCheck = checkProp<DotTextfieldComponent, string>(this, 'regexCheck');
    }

    @Watch('type')
    typeWatch(): void {
        this.type = checkProp<DotTextfieldComponent, string>(this, 'type');
    }

    render() {
        const classes = getClassNames(this.status, this.isValid(), this.required);

        return (
            <Host class={{ ...classes }}>
                <dot-label label={this.label} required={this.required} name={this.name}>
                    <input
                        aria-describedby={getHintId(this.hint)}
                        class={getErrorClass(this.status.dotValid)}
                        disabled={this.disabled || null}
                        id={getId(this.name)}
                        onBlur={() => this.blurHandler()}
                        onInput={(event: Event) => this.setValue(event)}
                        placeholder={this.placeholder}
                        required={this.required || null}
                        type={this.type}
                        value={this.value}
                    />
                </dot-label>
                {getTagHint(this.hint)}
                {getTagError(this.shouldShowErrorMessage(), this.getErrorMessage())}
            </Host>
        );
    }

    private validateProps(): void {
        this.regexCheckWatch();
        this.typeWatch();
    }

    private isValid(): boolean {
        return !this.isValueRequired() && this.isRegexValid();
    }

    private isValueRequired(): boolean {
        return this.required && !this.value;
    }

    private isRegexValid(): boolean {
        if (this.regexCheck && this.value) {
            const regex = new RegExp(this.regexCheck);
            return regex.test(this.value);
        }
        return true;
    }

    private shouldShowErrorMessage(): boolean {
        return this.getErrorMessage() && !this.status.dotPristine;
    }

    private getErrorMessage(): string {
        return this.isRegexValid()
            ? this.isValid()
                ? ''
                : this.requiredMessage
            : this.validationMessage;
    }

    private blurHandler(): void {
        if (!this.status.dotTouched) {
            this.status = updateStatus(this.status, {
                dotTouched: true
            });
            this.emitStatusChange();
        }
    }

    private setValue(event): void {
        this.value = event.target.value.toString();
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

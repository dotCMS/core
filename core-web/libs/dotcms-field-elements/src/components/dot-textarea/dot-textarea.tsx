import {
    Component,
    Prop,
    State,
    Method,
    Element,
    Event,
    EventEmitter,
    Watch,
    Host,
    h
} from '@stencil/core';

import { DotFieldStatus, DotFieldValueEvent, DotFieldStatusEvent } from '../../models';
import {
    getClassNames,
    getOriginalStatus,
    getTagHint,
    getTagError,
    getErrorClass,
    updateStatus,
    getId,
    checkProp,
    getHintId
} from '../../utils';
import { setDotAttributesToElement, getDotAttributesFromElement } from '../dot-form/utils';

/**
 * Represent a dotcms textarea control.
 *
 * @export
 * @class DotTextareaComponent
 */
@Component({
    tag: 'dot-textarea',
    styleUrl: 'dot-textarea.scss'
})
export class DotTextareaComponent {
    @Element() el: HTMLElement;

    /** Value specifies the value of the <textarea> element */
    @Prop({ mutable: true, reflect: true })
    value = '';

    /** Name that will be used as ID */
    @Prop({ reflect: true })
    name = '';

    /** (optional) Text to be rendered next to <textarea> element */
    @Prop({ reflect: true })
    label = '';

    /** (optional) Hint text that suggest a clue of the field */
    @Prop({ reflect: true })
    hint = '';

    /** (optional) Determine if it is mandatory */
    @Prop({ mutable: true, reflect: true })
    required = false;

    /** (optional) Text that be shown when required is set and condition not met */
    @Prop({ reflect: true })
    requiredMessage = 'This field is required';

    /** (optional) Text that be shown when the Regular Expression condition not met */
    @Prop({ reflect: true })
    validationMessage = "The field doesn't comply with the specified format";

    /** (optional) Disables field's interaction */
    @Prop({ mutable: true, reflect: true })
    disabled = false;

    /** (optional) Regular expresion that is checked against the value to determine if is valid  */
    @Prop({ mutable: true, reflect: true })
    regexCheck = '';

    @State() status: DotFieldStatus;

    @Event() valueChange: EventEmitter<DotFieldValueEvent>;
    @Event() statusChange: EventEmitter<DotFieldStatusEvent>;

    /**
     * Reset properties of the field, clear value and emit events.
     *
     * @memberof DotTextareaComponent
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
        const htmlElement = this.el.querySelector('textarea');
        setTimeout(() => {
            const attrs = getDotAttributesFromElement(Array.from(this.el.attributes), []);
            setDotAttributesToElement(htmlElement, attrs);
        }, 0);
    }

    @Watch('regexCheck')
    regexCheckWatch(): void {
        this.regexCheck = checkProp<DotTextareaComponent, string>(this, 'regexCheck');
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
                    <textarea
                        aria-describedby={getHintId(this.hint)}
                        class={getErrorClass(this.status.dotValid)}
                        id={getId(this.name)}
                        name={this.name}
                        value={this.value}
                        required={this.getRequiredAttr()}
                        onInput={(event: Event) => this.setValue(event)}
                        onBlur={() => this.blurHandler()}
                        disabled={this.getDisabledAtt()}
                    />
                </dot-label>
                {getTagHint(this.hint)}
                {getTagError(this.shouldShowErrorMessage(), this.getErrorMessage())}
            </Host>
        );
    }

    private validateProps(): void {
        this.regexCheckWatch();
    }

    private getDisabledAtt(): boolean {
        return this.disabled || null;
    }

    private getRequiredAttr(): boolean {
        return this.required ? true : null;
    }

    private isValid(): boolean {
        return !this.isValueRequired() && this.isRegexValid();
    }

    private isValueRequired(): boolean {
        return this.required && !this.value.length;
    }

    private isRegexValid(): boolean {
        if (this.regexCheck && this.value.length) {
            const regex = new RegExp(this.regexCheck, 'ig');
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

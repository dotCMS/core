import { Component, Prop, State, Method, Element, Event, EventEmitter, Watch } from '@stencil/core';
import Fragment from 'stencil-fragment';
import { DotFieldStatus, DotFieldValueEvent, DotFieldStatusEvent } from '../../models';
import {
    getClassNames,
    getOriginalStatus,
    getTagHint,
    getTagError,
    getErrorClass,
    updateStatus,
    getId,
    checkProp
} from '../../utils';

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
    @Prop({ mutable: true })
    value = '';

    /** Name that will be used as ID */
    @Prop() name = '';

    /** (optional) Text to be rendered next to <textarea> element */
    @Prop() label = '';

    /** (optional) Hint text that suggest a clue of the field */
    @Prop() hint = '';

    /** (optional) Determine if it is mandatory */
    @Prop() required = false;

    /** (optional) Text that be shown when required is set and condition not met */
    @Prop() requiredMessage = '';

    /** (optional) Text that be shown when the Regular Expression condition not met */
    @Prop() validationMessage = '';

    /** (optional) Disables field's interaction */
    @Prop() disabled = false;

    /** (optional) Regular expresion that is checked against the value to determine if is valid  */
    @Prop({ mutable: true })
    regexCheck = '';

    @State() status: DotFieldStatus = getOriginalStatus();

    @Event() valueChange: EventEmitter<DotFieldValueEvent>;
    @Event() statusChange: EventEmitter<DotFieldStatusEvent>;

    /**
     * Reset properties of the field, clear value and emit events.
     *
     * @memberof DotTextareaComponent
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
        this.emitStatusChange();
    }

    @Watch('regexCheck')
    regexCheckWatch(): void {
        this.regexCheck = checkProp<DotTextareaComponent, string>(this, 'regexCheck');
    }

    hostData() {
        return {
            class: getClassNames(this.status, this.isValid(), this.required)
        };
    }

    render() {
        return (
            <Fragment>
                <dot-label label={this.name} required={this.required} name={this.name}>
                    <textarea
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
                {getTagHint(this.hint, this.name)}
                {getTagError(this.shouldShowErrorMessage(), this.getErrorMessage())}
            </Fragment>
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

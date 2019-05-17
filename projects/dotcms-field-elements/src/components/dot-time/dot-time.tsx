import {
    Component,
    Element,
    Event,
    EventEmitter,
    Listen,
    Method,
    Prop,
    State,
    Watch
} from '@stencil/core';
import Fragment from 'stencil-fragment';
import {
    DotFieldStatusClasses,
    DotFieldStatusEvent,
    DotFieldValueEvent,
    DotLabel
} from '../../models';
import { checkProp, getClassNames, getTagError, getTagHint, getTagLabel } from '../../utils';

@Component({
    tag: 'dot-time',
    styleUrl: 'dot-time.scss'
})
export class DotTimeComponent {
    @Element() el: HTMLElement;

    /** Value format hh:mm:ss e.g., 15:22:00 */
    @Prop({ mutable: true })
    value = '';

    /** Name that will be used as ID */
    @Prop() name = '';

    /** (optional) Text to be rendered next to input field */
    @Prop() label = '';

    /** (optional) Hint text that suggest a clue of the field */
    @Prop() hint = '';

    /** (optional) Determine if it is mandatory */
    @Prop() required = false;

    /** (optional) Text that be shown when required is set and condition not met */
    @Prop() requiredMessage = '';

    /** (optional) Text that be shown when min or max are set and condition not met */
    @Prop() validationMessage = '';

    /** (optional) Disables field's interaction */
    @Prop() disabled = false;

    /** (optional) Min, minimum value that the field will allow to set. Format should be hh:mm:ss */
    @Prop({ mutable: true })
    min = '';

    /** (optional) Max, maximum value that the field will allow to set. Format should be  hh:mm:ss */
    @Prop({ mutable: true })
    max = '';

    /** (optional) Step specifies the legal number intervals for the input field */
    @Prop() step = '1';

    @State() classNames: DotFieldStatusClasses;
    @State() errorMessageElement: JSX.Element;

    @Event() valueChange: EventEmitter<DotFieldValueEvent>;
    @Event() statusChange: EventEmitter<DotFieldStatusEvent>;

    /**
     * Reset properties of the field, clear value and emit events.
     */
    @Method()
    reset(): void {
        const input = this.el.querySelector('dot-input-calendar');
        input.reset();
    }

    componentWillLoad(): void {
        this.validateProps();
    }

    @Watch('min')
    minWatch(): void {
        this.min = checkProp<DotTimeComponent, string>(this, 'min', 'time');
    }

    @Watch('max')
    maxWatch(): void {
        this.max = checkProp<DotTimeComponent, string>(this, 'max', 'time');
    }

    @Listen('_valueChange')
    emitValueChange(event: CustomEvent) {
        event.stopImmediatePropagation();
        this.valueChange.emit(event.detail);
    }

    @Listen('_statusChange')
    emitStatusChange(event: CustomEvent) {
        event.stopImmediatePropagation();
        const statusEvent: DotFieldStatusEvent = event.detail;
        this.classNames = getClassNames(
            statusEvent.status,
            statusEvent.status.dotValid,
            this.required
        );
        this.statusChange.emit(event.detail);
    }

    @Listen('_errorMessage')
    setErrorElement(event: CustomEvent) {
        event.stopImmediatePropagation();
        this.errorMessageElement = getTagError(event.detail.show, event.detail.message);
    }

    hostData() {
        return {
            class: this.classNames
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
                <dot-input-calendar
                    disabled={this.disabled}
                    type="time"
                    name={this.name}
                    value={this.value}
                    required={this.required}
                    required-message={this.requiredMessage}
                    validation-message={this.validationMessage}
                    min={this.min}
                    max={this.max}
                    step={this.step}
                />
                {getTagHint(this.hint)}
                {this.errorMessageElement}
            </Fragment>
        );
    }

    private validateProps(): void {
        this.minWatch();
        this.maxWatch();
    }
}

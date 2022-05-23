import {
    Component,
    Element,
    Event,
    EventEmitter,
    Listen,
    Method,
    Prop,
    State,
    Watch,
    h,
    Host
} from '@stencil/core';
import {
    DotFieldStatusClasses,
    DotFieldStatusEvent,
    DotFieldValueEvent,
    DotInputCalendarStatusEvent
} from '../../../models';
import { checkProp, getClassNames, getTagError, getTagHint, getHintId } from '../../../utils';
import { setDotAttributesToElement, getDotAttributesFromElement } from '../dot-form/utils';

@Component({
    tag: 'dot-date',
    styleUrl: 'dot-date.scss'
})
export class DotDateComponent {
    @Element()
    el: HTMLElement;

    /** Value format yyyy-mm-dd  e.g., 2005-12-01 */
    @Prop({ mutable: true, reflect: true })
    value = '';

    /** Name that will be used as ID */
    @Prop({ reflect: true })
    name = '';

    /** (optional) Text to be rendered next to input field */
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

    /** (optional) Text that be shown when min or max are set and condition not met */
    @Prop({ reflect: true })
    validationMessage = "The field doesn't comply with the specified format";

    /** (optional) Disables field's interaction */
    @Prop({ mutable: true, reflect: true })
    disabled = false;

    /** (optional) Min, minimum value that the field will allow to set. Format should be yyyy-mm-dd */
    @Prop({ mutable: true, reflect: true })
    min = '';

    /** (optional) Max, maximum value that the field will allow to set. Format should be yyyy-mm-dd */
    @Prop({ mutable: true, reflect: true })
    max = '';

    /** (optional) Step specifies the legal number intervals for the input field */
    @Prop({ mutable: true, reflect: true })
    step = '1';

    @State()
    classNames: DotFieldStatusClasses;
    @State()
    errorMessageElement: JSX.Element;

    @Event()
    dotValueChange: EventEmitter<DotFieldValueEvent>;
    @Event()
    dotStatusChange: EventEmitter<DotFieldStatusEvent>;

    /**
     * Reset properties of the field, clear value and emit events.
     */
    @Method()
    async reset(): Promise<void> {
        const input = this.el.querySelector('dot-input-calendar');
        input.reset();
    }

    componentWillLoad(): void {
        this.validateProps();
    }

    componentDidLoad(): void {
        const attrException = ['dottype'];
        const htmlElement = this.el.querySelector('input[type="date"]');
        setTimeout(() => {
            const attrs = getDotAttributesFromElement(
                Array.from(this.el.attributes),
                attrException
            );
            setDotAttributesToElement(htmlElement, attrs);
        }, 0);
    }

    @Watch('min')
    minWatch(): void {
        this.min = checkProp<DotDateComponent, string>(this, 'min', 'date');
    }

    @Watch('max')
    maxWatch(): void {
        this.max = checkProp<DotDateComponent, string>(this, 'max', 'date');
    }

    @Listen('_dotValueChange')
    emitValueChange(event: CustomEvent) {
        event.stopImmediatePropagation();
        const valueEvent: DotFieldValueEvent = event.detail;
        this.value = valueEvent.value as string;
        this.dotValueChange.emit(valueEvent);
    }

    @Listen('_dotStatusChange')
    emitStatusChange(event: CustomEvent) {
        event.stopImmediatePropagation();
        const inputCalendarStatus: DotInputCalendarStatusEvent = event.detail;
        this.classNames = getClassNames(
            inputCalendarStatus.status,
            inputCalendarStatus.status.dotValid,
            this.required
        );
        this.setErrorMessageElement(inputCalendarStatus);
        this.dotStatusChange.emit({
            name: inputCalendarStatus.name,
            status: inputCalendarStatus.status
        });
    }

    render() {
        return (
            <Host class={{ ...this.classNames }}>
                <dot-label label={this.label} required={this.required} name={this.name}>
                    <dot-input-calendar
                        aria-describedby={getHintId(this.hint)}
                        tabIndex={this.hint ? 0 : null}
                        disabled={this.disabled}
                        type="date"
                        name={this.name}
                        value={this.value}
                        required={this.required}
                        min={this.min}
                        max={this.max}
                        step={this.step}
                    />
                </dot-label>
                {getTagHint(this.hint)}
                {this.errorMessageElement}
            </Host>
        );
    }

    private validateProps(): void {
        this.minWatch();
        this.maxWatch();
    }

    private setErrorMessageElement(statusEvent: DotInputCalendarStatusEvent) {
        this.errorMessageElement = getTagError(
            !statusEvent.status.dotValid && !statusEvent.status.dotPristine,
            this.getErrorMessage(statusEvent)
        );
    }

    private getErrorMessage(statusEvent: DotInputCalendarStatusEvent): string {
        return !!this.value
            ? statusEvent.isValidRange
                ? ''
                : this.validationMessage
            : this.requiredMessage;
    }
}

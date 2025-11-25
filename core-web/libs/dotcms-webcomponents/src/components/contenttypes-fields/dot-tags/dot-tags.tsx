import {
    Component,
    Prop,
    State,
    Element,
    Event,
    EventEmitter,
    Method,
    Watch,
    h,
    Host
} from '@stencil/core';
import { DotFieldStatus, DotFieldValueEvent, DotFieldStatusEvent } from '../../../models';
import {
    checkProp,
    getClassNames,
    getErrorClass,
    getOriginalStatus,
    getTagError,
    getTagHint,
    updateStatus,
    getHintId,
    isStringType
} from '../../../utils';
import { SelectionFeedback } from './components/dot-autocomplete/dot-autocomplete';

@Component({
    tag: 'dot-tags',
    styleUrl: 'dot-tags.scss'
})
export class DotTagsComponent {
    @Element()
    el: HTMLElement;

    /** Value formatted splitted with a comma, for example: tag-1,tag-2 */
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

    /** (optional) text to show when no value is set */
    @Prop({ reflect: true })
    placeholder = '';

    /** (optional) Determine if it is mandatory */
    @Prop({ reflect: true })
    required = false;

    /** (optional) Text that be shown when required is set and value is not set */
    @Prop({ reflect: true })
    requiredMessage = 'This field is required';

    /** (optional) Disables field's interaction */
    @Prop({ reflect: true })
    disabled = false;

    /** Min characters to start search in the autocomplete input */
    @Prop({ reflect: true })
    threshold = 1;

    /** Duraction in ms to start search into the autocomplete */
    @Prop({ reflect: true })
    debounce = 300;

    /** Function or array of string to get the data to use for the autocomplete search */
    @Prop()
    data: () => Promise<string[]> | string[] = null;

    @State()
    status: DotFieldStatus;

    @Event()
    dotValueChange: EventEmitter<DotFieldValueEvent>;
    @Event()
    dotStatusChange: EventEmitter<DotFieldStatusEvent>;

    /**
     * Reset properties of the filed, clear value and emit events.
     */
    @Method()
    async reset(): Promise<void> {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitChanges();
    }

    @Watch('value')
    valueWatch(): void {
        this.value = checkProp<DotTagsComponent, string>(this, 'value', 'string');
    }

    componentWillLoad(): void {
        this.status = getOriginalStatus(this.isValid());
        this.validateProps();
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
                        class="dot-tags__container">
                        <dot-autocomplete
                            class={getErrorClass(this.status.dotValid)}
                            data={this.data}
                            debounce={this.debounce}
                            disabled={this.isDisabled()}
                            onEnter={this.onEnterHandler.bind(this)}
                            onLostFocus={this.blurHandler.bind(this)}
                            onSelection={this.onSelectHandler.bind(this)}
                            placeholder={this.placeholder || null}
                            threshold={this.threshold}
                        />
                        <div class="dot-tags__chips">
                            {this.getValues().map((tagLab: string) => (
                                <dot-chip
                                    disabled={this.isDisabled()}
                                    label={tagLab}
                                    onRemove={this.removeTag.bind(this)}
                                />
                            ))}
                        </div>
                    </div>
                </dot-label>

                {getTagHint(this.hint)}
                {getTagError(this.showErrorMessage(), this.getErrorMessage())}
            </Host>
        );
    }

    private addTag(label: string): void {
        const values = this.getValues();

        if (!values.includes(label)) {
            values.push(label);
            this.value = values.join(',');

            this.updateStatus();
            this.emitChanges();
        }
    }

    private blurHandler(): void {
        if (!this.status.dotTouched) {
            this.status = updateStatus(this.status, {
                dotTouched: true
            });
            this.emitStatusChange();
        }
    }

    private emitChanges(): void {
        this.emitStatusChange();
        this.emitValueChange();
    }

    private emitStatusChange(): void {
        this.dotStatusChange.emit({
            name: this.name,
            status: this.status
        });
    }

    private emitValueChange(): void {
        this.dotValueChange.emit({
            name: this.name,
            value: this.value
        });
    }

    private getErrorMessage(): string {
        return this.isValid() ? '' : this.requiredMessage;
    }

    private getValues(): string[] {
        return isStringType(this.value) ? this.value.split(',') : [];
    }

    private isDisabled(): boolean {
        return this.disabled || null;
    }

    private isValid(): boolean {
        return !this.required || (this.required && !!this.value);
    }

    private onEnterHandler({ detail = '' }: CustomEvent<string>) {
        detail.split(',').forEach((label: string) => {
            this.addTag(label.trim());
        });
    }

    private onSelectHandler({ detail }: CustomEvent<SelectionFeedback>) {
        const value = detail.selection.value.replace(',', ' ').replace(/\s+/g, ' ');
        this.addTag(value);
    }

    private removeTag(event: CustomEvent): void {
        const values = this.getValues().filter((item) => item !== event.detail);
        this.value = values.join(',');

        this.updateStatus();
        this.emitChanges();
    }

    private showErrorMessage(): boolean {
        return this.getErrorMessage() && !this.status.dotPristine;
    }

    private updateStatus(): void {
        this.status = updateStatus(this.status, {
            dotTouched: true,
            dotPristine: false,
            dotValid: this.isValid()
        });
    }

    private validateProps(): void {
        this.valueWatch();
    }
}

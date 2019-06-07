import { Component, Prop, State, Element, Event, EventEmitter, Method, Watch } from '@stencil/core';
import Fragment from 'stencil-fragment';
import { DotFieldStatus, DotFieldValueEvent, DotFieldStatusEvent } from '../../models';
import {
    checkProp,
    getClassNames,
    getErrorClass,
    getOriginalStatus,
    getTagError,
    getTagHint,
    updateStatus,
    isStringType
} from '../../utils';

const getData = async (): Promise<string[]> => {
    return fetch('https://tarekraafat.github.io/autoComplete.js/demo/db/generic.json')
        .then((data) => data.json())
        .then((items) => items.map(({ food }) => food));
};

@Component({
    tag: 'dot-tags',
    styleUrl: 'dot-tags.scss'
})
export class DotTagsComponent {
    @Element() el: HTMLElement;

    /** Value formatted splitted with a comma, for example: tag-1,tag-2 */
    @Prop({ mutable: true, reflectToAttr: true }) value = '';

    /** Name that will be used as ID */
    @Prop({ reflectToAttr: true }) name = '';

    /** (optional) Text to be rendered next to input field */
    @Prop({ reflectToAttr: true }) label = '';

    /** (optional) Hint text that suggest a clue of the field */
    @Prop({ reflectToAttr: true }) hint = '';

    /** (optional) text to show when no value is set */
    @Prop({ reflectToAttr: true }) placeholder = '';

    /** (optional) Determine if it is mandatory */
    @Prop({ reflectToAttr: true }) required = false;

    /** (optional) Text that be shown when required is set and value is not set */
    @Prop({ reflectToAttr: true }) requiredMessage = 'This field is required';

    /** (optional) Disables field's interaction */
    @Prop({ reflectToAttr: true }) disabled = false;

    /** Min characters to start search in the autocomplete input */
    @Prop({ reflectToAttr: true }) threshold = 0;

    /** Duraction in ms to start search into the autocomplete */
    @Prop({ reflectToAttr: true }) debounce = 300;

    @State() status: DotFieldStatus = getOriginalStatus();

    @Event() valueChange: EventEmitter<DotFieldValueEvent>;
    @Event() statusChange: EventEmitter<DotFieldStatusEvent>;

    /**
     * Reset properties of the filed, clear value and emit events.
     */
    @Method()
    reset(): void {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitChanges();
    }

    @Watch('value')
    valueWatch(): void {
        this.value = checkProp<DotTagsComponent, string>(this, 'value', 'string');
    }

    componentWillLoad(): void {
        this.validateProps();
        this.emitStatusChange();
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
                    <div class="dot-tags__container">
                        <dot-autocomplete
                            class={getErrorClass(this.status.dotValid)}
                            data={getData}
                            debounce={this.debounce}
                            disabled={this.disabled}
                            onLostFocus={() => this.blurHandler()}
                            onSelect={(event: CustomEvent<string>) => this.addTag(event.detail)}
                            placeholder={this.placeholder || null}
                            threshold={this.threshold}
                        />
                        <div class="dot-tags__chips">
                            {this.getValues().map((tagLab: string) => (
                                <dot-chip
                                    disabled={this.disabled}
                                    label={tagLab}
                                    onRemove={this.removeTag.bind(this)}
                                />
                            ))}
                        </div>
                    </div>
                </dot-label>

                {getTagHint(this.hint)}
                {getTagError(this.showErrorMessage(), this.getErrorMessage())}
            </Fragment>
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

    private getErrorMessage(): string {
        return this.isValid() ? '' : this.requiredMessage;
    }

    private getValues(): string[] {
        return isStringType(this.value) ? this.value.split(',') : [];
    }

    private isValid(): boolean {
        return !this.required || (this.required && !!this.value);
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

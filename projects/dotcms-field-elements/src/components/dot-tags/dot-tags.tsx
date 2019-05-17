import { Component, Prop, State, Element, Event, EventEmitter, Method, Watch } from '@stencil/core';
import Fragment from 'stencil-fragment';
import { DotFieldStatus, DotFieldValueEvent, DotFieldStatusEvent, DotLabel } from '../../models';
import {
    getClassNames,
    getOriginalStatus,
    getTagHint,
    getTagError,
    getTagLabel,
    getErrorClass,
    updateStatus
} from '../../utils';

@Component({
    tag: 'dot-tags',
    styleUrl: 'dot-tags.scss'
})
export class DotTagsComponent {
    @Element() el: HTMLElement;

     /** Value formatted splitted with a comma, for example: tag-1,tag-2 */
    @Prop({ mutable: true }) value = '';

    /** Name that will be used as ID */
    @Prop() name = '';

    /** (optional) Text to be rendered next to input field */
    @Prop() label = '';

    /** (optional) Hint text that suggest a clue of the field */
    @Prop() hint= '';

    /** (optional) text to show when no value is set */
    @Prop() placeholder= '';

     /** (optional) Determine if it is mandatory */
    @Prop() required =  false;

    /** (optional) Text that be shown when required is set and value is not set */
    @Prop() requiredMessage = '';

    /** (optional) Disables field's interaction */
    @Prop() disabled = false;

    /** Min characters to start search in the autocomplete input */
    @Prop() threshold = 0;

    /** Duraction in ms to start search into the autocomplete */
    @Prop() debounce = 300;

    @State() status: DotFieldStatus = getOriginalStatus();

    @Event() valueChange: EventEmitter<DotFieldValueEvent>;
    @Event() statusChange: EventEmitter<DotFieldStatusEvent>;
    @Event() selected: EventEmitter<String>;
    @Event() removed: EventEmitter<String>;

    /**
     * Reset properties of the filed, clear value and emit events.
     */
    @Method()
    reset(): void {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
        this.emitValueChange();
    }

    componentWillLoad(): void {
        this.emitStatusChange();
    }

    hostData() {
        return {
            class: getClassNames(this.status, this.isValid(), this.required)
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
                <div class="tag_container">
                    {this.getValues().map((tagLab: string) =>
                        <dot-chip
                            label={tagLab}
                            disabled={this.disabled}
                            onRemove={this.removeTag.bind(this)}
                        >
                        </dot-chip>
                    )}
                </div>

                <dot-autocomplete
                    id={this.name}
                    class={getErrorClass(this.status.dotValid)}
                    disabled={this.disabled || null}
                    placeholder={this.placeholder || null}
                    threshold={this.threshold}
                    debounce={this.debounce}
                    data={this.getData.bind(this)}
                    onLostFocus={() => this.blurHandler()}
                    onSelection={(event) => this.addTag(event.detail)}
                >
                </dot-autocomplete>

                {getTagHint(this.hint)}
                {getTagError(this.showErrorMessage(), this.getErrorMessage())}
            </Fragment>
        );
    }

    @Watch('value')
    setValue(): void {
        this.status = updateStatus(this.status, {
            dotTouched: true,
            dotPristine: false,
            dotValid: this.isValid()
        });

        this.emitValueChange();
        this.emitStatusChange();
    }

    private addTag(label: string): void {
        const values = this.getValues();

        if (!values.includes(label)) {
            values.push(label);
            this.value = values.join(',');
            this.selected.emit(label);
        }
    }

    private removeTag(event: CustomEvent): void {
        const values = this.getValues().filter(item => item !== event.detail);
        this.value = values.join(',');
        this.removed.emit(event.detail);
    }

    private getValues(): string[] {
        return this.value ? this.value.split(',') : [];
    }

    private isValid(): boolean {
        return !this.required || (this.required && !!this.value);
    }

    private showErrorMessage(): boolean {
        return this.getErrorMessage() && !this.status.dotPristine;
    }

    private getErrorMessage(): string {
        return this.isValid()
                ? ''
                : this.requiredMessage;
    }

    private blurHandler(): void {
        if (!this.status.dotTouched) {
            this.status = updateStatus(this.status, {
                dotTouched: true
            });
            this.emitStatusChange();
        }
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

    private async getData(): Promise<String[]> {
        const source = await fetch(
            'https://tarekraafat.github.io/autoComplete.js/demo/db/generic.json'
        );
        return (await source.json()).map(item => item.food);
    }
}

import { Component, Prop, State, Element, Method, Event, EventEmitter } from '@stencil/core';
import Fragment from 'stencil-fragment';
import { DotOption, DotFieldStatus, DotFieldValueEvent, DotFieldStatusEvent, DotLabel } from '../../models';
import {
    getClassNames,
    getDotOptionsFromFieldValue,
    getErrorClass,
    getId,
    getOriginalStatus,
    getTagError,
    getTagHint,
    getTagLabel,
    updateStatus
} from '../../utils';

/**
 * Represent a dotcms select control.
 *
 * @export
 * @class DotSelectComponent
 */
@Component({
    tag: 'dot-select',
    styleUrl: 'dot-select.scss'
})
export class DotSelectComponent {
    @Element() el: HTMLElement;

    @Prop() disabled = false;
    @Prop() name: string;
    @Prop() label: string;
    @Prop() hint: string;
    @Prop() options: string;
    @Prop() required: boolean;
    @Prop() requiredMessage: string;
    @Prop({ mutable: true }) value: string;

    @State() _options: DotOption[];
    @State() status: DotFieldStatus = getOriginalStatus();

    @Event() valueChange: EventEmitter<DotFieldValueEvent>;
    @Event() statusChange: EventEmitter<DotFieldStatusEvent>;

    _dotTouched = false;
    _dotPristine = true;

    componentWillLoad() {
        this._options = getDotOptionsFromFieldValue(this.options);
        this.emitInitialValue();
        this.emitStatusChange();
    }

    hostData() {
        return {
            class: getClassNames(this.status, this.isValid(), this.required)
        };
    }

    /**
     * Reset properties of the field, clear value and emit events.
     *
     * @memberof DotSelectComponent
     *
     */
    @Method()
    reset(): void {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitInitialValue();
        this.emitStatusChange();
    }

    render() {
        const labelTagParams: DotLabel = {name: this.name, label: this.label, required: this.required};
        return (
            <Fragment>
                {getTagLabel(labelTagParams)}
                <select
                    class={getErrorClass(this.status.dotValid)}
                    id={getId(this.name)}
                    disabled={this.shouldBeDisabled()}
                    onChange={(event: Event) => this.setValue(event)}>

                    {this._options.map((item: DotOption) => {
                        return (
                            <option
                                selected={this.value === item.value ? true : null}
                                value={item.value}
                            >
                                {item.label}
                            </option>
                        );
                    })}

                </select>
                {getTagHint(this.hint)}
                {getTagError(!this.isValid(), this.requiredMessage)}
            </Fragment>
        );
    }

    private shouldBeDisabled(): boolean {
        return this.disabled ? true : null;
    }

     // Todo: find how to set proper TYPE in TS
    private setValue(event): void {
        this.value = event.target.value;
        this.status = updateStatus(this.status, {
            dotTouched: true,
            dotPristine: false,
            dotValid: this.isValid()
        });
        this.emitValueChange();
        this.emitStatusChange();
    }

    private emitInitialValue() {
        if (!this.value) {
            this.value = this._options[0].value;
            this.emitValueChange();
        }
    }

    private emitStatusChange(): void {
        this.statusChange.emit({
            name: this.name,
            status: this.status
        });
    }

    private isValid(): boolean {
        return this.required ? !!this.value : true;
    }

    private emitValueChange(): void {
        this.valueChange.emit({
            name: this.name,
            value: this.value
        });
    }
}

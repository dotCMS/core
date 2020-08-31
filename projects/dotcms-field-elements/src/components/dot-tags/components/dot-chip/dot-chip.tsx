import { Component, Prop, Element, Event, EventEmitter } from '@stencil/core';
import Fragment from 'stencil-fragment';

@Component({
    tag: 'dot-chip',
    styleUrl: 'dot-chip.scss'
})
export class DotChipComponent {
    @Element() el: HTMLElement;

    /** Chip's label */
    @Prop({ reflectToAttr: true }) label = '';

    /** (optional) Delete button's label */
    @Prop({ reflectToAttr: true }) deleteLabel = 'Delete';

    /** (optional) If is true disabled the delete button */
    @Prop({ reflectToAttr: true }) disabled = false;

    @Event() remove: EventEmitter<String>;

    render() {
        const label = this.label ? `${this.deleteLabel} ${this.label}` : null;
        return (
            <Fragment>
                <span>
                    {this.label}
                </span>
                <button type="button"
                    aria-label={label}
                    disabled={this.disabled}
                    onClick={() => this.remove.emit(this.label)}
                >
                    {this.deleteLabel}
                </button>
            </Fragment>
        );
    }
}

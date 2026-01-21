import { Component, Prop, Element, Event, EventEmitter, Host, h } from '@stencil/core';

@Component({
    tag: 'dot-chip',
    styleUrl: 'dot-chip.scss'
})
export class DotChipComponent {
    @Element() el: HTMLElement;

    /** Chip's label */
    @Prop({ reflect: true }) label = '';

    /** (optional) Delete button's label */
    @Prop({ reflect: true }) deleteLabel = 'Delete';

    /** (optional) If is true disabled the delete button */
    @Prop({ reflect: true }) disabled = false;

    @Event() remove: EventEmitter<string>;

    render() {
        const label = this.label ? `${this.deleteLabel} ${this.label}` : null;
        return (
            <Host>
                <span>{this.label}</span>
                <button
                    type="button"
                    aria-label={label}
                    disabled={this.disabled}
                    onClick={() => this.remove.emit(this.label)}>
                    {this.deleteLabel}
                </button>
            </Host>
        );
    }
}

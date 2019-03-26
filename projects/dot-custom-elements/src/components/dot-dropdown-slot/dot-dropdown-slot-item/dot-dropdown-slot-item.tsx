import { Component, Prop, Event, EventEmitter } from '@stencil/core';

@Component({
    tag: 'dot-dropdown-slot-item',
    styleUrl: 'dot-dropdown-slot-item.scss'
    //   shadow: true
})
export class DotDropdownSlotItemComponent {
    @Prop() value: string;
    @Prop() label: string;
    @Event() dotItemClicked: EventEmitter;

    clicked() {
        this.dotItemClicked.emit(this.value);
    }

    render() {
        return (
            <span
                id={this.value}
                onClick={() => {
                    this.clicked();
                }}
            >
                {this.label}
            </span>
        );
    }
}

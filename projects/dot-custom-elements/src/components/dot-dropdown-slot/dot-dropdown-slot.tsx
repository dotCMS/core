import {
    Component, State, Method, Element, Event, EventEmitter, Listen } from '@stencil/core';

@Component({
    tag: 'dot-dropdown-slot',
    styleUrl: 'dot-dropdown-slot.scss'
})
export class DotDropdownSlotComponent {
    @Element() el: HTMLElement;
    @Event() onOpen: EventEmitter;
    @Event() onClose: EventEmitter;

    @State() opened = false;

    @Method()
    open() {
        this.opened = true;
    }

    @Method()
    close() {
        this.opened = false;
    }

    @Listen('dotItemClicked')
    dotItemClickedHandler(event) {
        console.log('---dot-dropdown-slot', event.detail);
        this._toggle();
    }

    _toggle() {
        if (this.opened) {
            this.close();
            this.onClose.emit();
        } else {
            this.open();
            this.onOpen.emit();
        }
    }

    render() {
        return [
            <div aria-haspopup='true' aria-expanded='false' onClick={() => this._toggle()}>
                <button slot='trigger'>test</button>
            </div>,
            <div class={`${this.opened ? 'show' : ''} dropdown-content`}>
                <slot />
            </div>
        ];
    }
}

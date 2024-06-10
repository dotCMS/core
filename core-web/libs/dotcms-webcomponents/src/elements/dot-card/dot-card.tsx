import { Component, h } from '@stencil/core';

@Component({
    tag: 'dot-card',
    styleUrl: 'dot-card.scss',
    shadow: true
})
export class DotCard {
    render() {
        return <slot />;
    }
}

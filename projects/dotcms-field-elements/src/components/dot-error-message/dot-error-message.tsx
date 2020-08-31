import { Component } from '@stencil/core';

@Component({
    tag: 'dot-error-message',
    styleUrl: 'dot-error-message.scss'
})
export class DotErrorMessageComponent {
    render() {
        return <slot />;
    }
}

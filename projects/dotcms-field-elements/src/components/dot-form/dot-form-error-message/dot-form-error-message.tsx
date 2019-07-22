import { Component } from '@stencil/core';

@Component({
    tag: 'dot-form-error-message',
    styleUrl: 'dot-form-error-message.scss'
})
export class DotFormErrorMessageComponent {
    render() {
        return <slot />;
    }
}

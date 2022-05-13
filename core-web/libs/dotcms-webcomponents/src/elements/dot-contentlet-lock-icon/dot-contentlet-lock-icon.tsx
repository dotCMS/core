import { Component, h, Prop, Host } from '@stencil/core';
import '@material/mwc-icon';

@Component({
    tag: 'dot-contentlet-lock-icon',
    styleUrl: 'dot-contentlet-lock-icon.scss',
    shadow: true
})
export class DotContentletLockIcon {
    @Prop() locked: boolean;
    @Prop() size = '16px';

    render() {
        return (
            <Host style={{ '--mdc-icon-size': this.size }}>
                <mwc-icon>{this.locked ? 'locked' : 'lock_open'}</mwc-icon>
            </Host>
        );
    }
}

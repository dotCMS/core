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
                {/* lock_open_right swings the shackle out, so unlocked reads differently from locked at 16px; lock_open only lifts it a pixel */}
                <mwc-icon>{this.locked ? 'lock' : 'lock_open_right'}</mwc-icon>
            </Host>
        );
    }
}

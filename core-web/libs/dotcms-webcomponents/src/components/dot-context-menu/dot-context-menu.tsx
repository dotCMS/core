import { Component, h, Element, Host, Prop, Method, State } from '@stencil/core';
import { DotContextMenuOption } from '../../models/dot-context-menu.model';

import '@material/mwc-menu';
import '@material/mwc-list/mwc-list-item';
import '@material/mwc-icon';
import { DotContextMenuAction } from '../../models/dot-context-menu-action.model';

@Component({
    tag: 'dot-context-menu',
    styleUrl: 'dot-context-menu.scss',
    shadow: true
})
export class DotContextMenu {
    @Element() el: HTMLElement;

    @Prop() options: DotContextMenuOption<DotContextMenuAction>[] = [];
    @Prop() fontSize = '16px';

    @State() state = {
        x: 0,
        y: 0,
        position: 'relative',
        show: false
    };

    @Method()
    async hide(): Promise<void> {
        this.state = {
            ...this.state,
            show: false
        };
    }

    @Method()
    async show(x: number, y: number, position = 'inherit'): Promise<void> {
        await this.hide();

        requestAnimationFrame(() => {
            this.state = {
                x,
                y,
                position,
                show: true
            };
        });
    }

    render() {
        return (
            <Host style={{ '--menu-item-font-size': this.fontSize, position: this.state.position }}>
                <button
                    type="button"
                    onClick={async () => {
                        await this.show(0, 0, 'relative');
                    }}>
                    <mwc-icon>more_vert</mwc-icon>
                </button>
                <mwc-menu
                    open={this.state.show}
                    x={this.state.x}
                    y={this.state.y}
                    onAction={(e: CustomEvent<DotContextMenuAction>) => {
                        this.state = {
                            ...this.state,
                            show: false
                        };
                        this.options[e.detail.index].action(e);
                    }}>
                    {this.options.map(({ label }: DotContextMenuOption<DotContextMenuAction>) => (
                        <mwc-list-item>{label}</mwc-list-item>
                    ))}
                </mwc-menu>
            </Host>
        );
    }
}

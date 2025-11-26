import { Component, h, Element, Prop, Event, EventEmitter, Method } from '@stencil/core';
import {
    DotCardContentletItem,
    DotCardContentletEvent
} from '../../models/dot-card-contentlet.model';

import '@material/mwc-checkbox';
import '@material/mwc-formfield';
import { Checkbox } from '@material/mwc-checkbox';
import { DotContentletItem } from '../../models/dot-contentlet-item.model';
import { DotContentState } from '@dotcms/dotcms-models';

@Component({
    tag: 'dot-card-contentlet',
    styleUrl: 'dot-card-contentlet.scss',
    shadow: true
})
export class DotCardContentlet {
    @Element() el: HTMLDotCardContentletElement;

    @Prop() item: DotCardContentletItem;

    @Prop() thumbnailSize = '260';
    @Prop() iconSize = '96px';

    @Prop({
        reflect: true,
        mutable: true
    })
    checked: boolean;

    @Prop() showVideoThumbnail = false;

    @Event() checkboxChange: EventEmitter<DotCardContentletEvent>;
    @Event() contextMenuClick: EventEmitter<MouseEvent>;

    private menu: HTMLDotContextMenuElement;

    private isShiftKey = false;

    @Method()
    async showMenu(x: number, y: number) {
        const { left, top } = this.el.getBoundingClientRect();
        this.menu.show(x - left, y - top);
    }

    @Method()
    async hideMenu() {
        this.menu.hide();
    }

    componentDidLoad() {
        this.menu = this.el.shadowRoot.querySelector('dot-context-menu');
    }

    render() {
        const contentlet = this.item.data;
        const title = contentlet?.title;
        return (
            <dot-card>
                <dot-contentlet-thumbnail
                    showVideoThumbnail={this.showVideoThumbnail}
                    backgroundImage={true}
                    contentlet={contentlet}
                    width={this.thumbnailSize}
                    height={this.thumbnailSize}
                    alt={title}
                    iconSize={this.iconSize}
                />
                <header>
                    <div class="main">
                        <mwc-checkbox
                            checked={this.checked}
                            onClick={(e: MouseEvent) => {
                                e.stopImmediatePropagation();
                                this.isShiftKey = e.shiftKey;
                            }}
                            onChange={(e: MouseEvent) => {
                                const target = e.target as Checkbox;
                                this.checked = target.checked;
                                this.menu.hide();

                                this.checkboxChange.emit({
                                    originalTarget: this.el,
                                    shiftKey: this.isShiftKey
                                });
                            }}
                        />
                        <label id="label">{title}</label>
                        <dot-tooltip position="left top" delay={400} content={title} for="label" />
                    </div>
                    <div class="extra">
                        <div class="state">
                            <dot-state-icon state={this.getContentState(contentlet)} size="16px" />
                            <dot-badge bordered={true}>{contentlet.language}</dot-badge>
                            {contentlet.locked === 'true' ? (
                                <dot-contentlet-lock-icon locked={JSON.parse(contentlet.locked)} />
                            ) : null}
                        </div>
                        {this.item?.actions?.length ? (
                            <dot-context-menu
                                onClick={(e: MouseEvent) => {
                                    e.stopImmediatePropagation();
                                    this.contextMenuClick.emit(e);
                                }}
                                options={this.item.actions}
                            />
                        ) : null}
                    </div>
                </header>
            </dot-card>
        );
    }

    private getContentState({
        live,
        working,
        deleted,
        hasLiveVersion
    }: DotContentletItem): DotContentState {
        return { live, working, deleted, hasLiveVersion };
    }
}

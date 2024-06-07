import { Component, h, Host, Prop } from '@stencil/core';
import { DotContentState } from '@dotcms/dotcms-models';

@Component({
    tag: 'dot-state-icon',
    styleUrl: 'dot-state-icon.scss',
    shadow: true
})
export class DotStateIcon {
    @Prop({ reflect: true })
    state: DotContentState = null;
    @Prop({ reflect: true })
    size = '16px';
    @Prop({ reflect: true })
    labels = {
        archived: 'Archived',
        published: 'Published',
        revision: 'Revision',
        draft: 'Draft'
    };

    render() {
        const state = this.state ? this.getType(this.state) : '';
        const name = this.labels[state];
        return (
            <Host
                aria-label={name}
                style={{
                    '--size': this.size
                }}>
                <span>
                    <div class={state} id="icon" />
                    <dot-tooltip content={name} for="icon" />
                </span>
            </Host>
        );
    }

    private getType({ live, working, archived, deleted, hasLiveVersion }: DotContentState): string {
        if (this.isTrue(deleted) || this.isTrue(archived)) {
            return 'archived'; // crossed
        }

        if (live.toString() === 'true') {
            if (this.isTrue(hasLiveVersion) && this.isTrue(working)) {
                return 'published'; // full
            }
        } else {
            if (this.isTrue(hasLiveVersion)) {
                return 'revision'; // half
            }
        }

        return 'draft'; // empty
    }

    private isTrue(value: string | boolean): boolean {
        return value ? value.toString() === 'true' : false;
    }
}

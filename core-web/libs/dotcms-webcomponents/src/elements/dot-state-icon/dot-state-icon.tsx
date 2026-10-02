import { Component, h, Host, Prop } from '@stencil/core';
import { DotContentState } from '@dotcms/dotcms-models';

/**
 * @deprecated Use dot-contentlet-status-badge instead
 */
/** The four states this icon renders, and exactly the keys of its `labels` map. */
type DotStateIconType = 'archived' | 'published' | 'revision' | 'draft';

@Component({
    tag: 'dot-state-icon',
    styleUrl: 'dot-state-icon.scss',
    shadow: true
})
export class DotStateIcon {
    @Prop({ reflect: true })
    state?: DotContentState;
    @Prop({ reflect: true })
    size = '16px';
    @Prop({ reflect: true })
    labels: Record<DotStateIconType, string> = {
        archived: 'Archived',
        published: 'Published',
        revision: 'Revision',
        draft: 'Draft'
    };

    render() {
        const state = this.state ? this.getType(this.state) : undefined;
        // Undefined with no state, which is also what the previous `''` produced when used as a
        // key: neither is a label, so the tooltip and aria-label stay empty.
        const name = state ? this.labels[state] : undefined;
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

    private getType({
        live,
        working,
        archived,
        deleted,
        hasLiveVersion
    }: DotContentState): DotStateIconType {
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

    /**
     * `undefined` is accepted because `archived`, `deleted` and `hasLiveVersion` are all optional on
     * {@link DotContentState} — an absent flag is not true, which the falsy branch already returned.
     */
    private isTrue(value: string | boolean | undefined): boolean {
        return value ? value.toString() === 'true' : false;
    }
}

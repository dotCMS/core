import { Component, Prop, h } from '@stencil/core';

import { getLabelId } from '../../utils';

/**
 * Represent a dotcms label control.
 *
 * @export
 * @class DotLabelComponent
 */
@Component({
    tag: 'dot-label',
    styleUrl: 'dot-label.scss'
})
export class DotLabelComponent {
    /** (optional) Field name */
    @Prop({
        reflect: true
    })
    name = '';

    /** (optional) Text to be rendered */
    @Prop({
        reflect: true
    })
    label = '';

    /** (optional) Determine if it is mandatory */
    @Prop({
        reflect: true
    })
    required = false;

    render() {
        return (
            <label class="dot-label" id={getLabelId(this.name)}>
                {this.label && (
                    <span class="dot-label__text">
                        {this.label}
                        {this.required ? <span class="dot-label__required-mark">*</span> : null}
                    </span>
                )}
                <slot />
            </label>
        );
    }
}

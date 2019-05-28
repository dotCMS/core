import { Component, Prop } from '@stencil/core';
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
        reflectToAttr: true
    })
    name = '';

    /** (optional) Text to be rendered */
    @Prop({
        reflectToAttr: true
    })
    label = '';

    /** (optional) Determine if it is mandatory */
    @Prop({
        reflectToAttr: true
    })
    required = false;

    render() {
        return (
            <label class="dot-label" id={getLabelId(this.name)}>
                <span class="dot-label__text">
                    {this.label}
                    {this.required ? <span class="dot-label__required-mark">*</span> : null}
                </span>
                <slot />
            </label>
        );
    }
}

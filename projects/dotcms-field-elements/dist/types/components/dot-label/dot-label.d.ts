import '../../stencil.core';
/**
 * Represent a dotcms label control.
 *
 * @export
 * @class DotLabelComponent
 */
export declare class DotLabelComponent {
    /** (optional) Field name */
    name: string;
    /** (optional) Text to be rendered */
    label: string;
    /** (optional) Determine if it is mandatory */
    required: boolean;
    render(): JSX.Element;
}

import { UVE_MODE } from '@dotcms/types';

import { useDotCMSShowWhen } from '../../hooks/useDotCMSShowWhen';

/**
 * Props for the DotCMSShow component.
 *
 * @typedef {Object} DotCMSShowProps
 * @property {React.ReactNode} children - The children to be rendered when the condition is met.
 * @property {UVE_MODE} [when=UVE_MODE.EDIT] - The UVE mode in which the children should be rendered.
 */
type DotCMSShowProps = {
    children: React.ReactNode;
    when?: UVE_MODE;
};

/**
 * DotCMSShow component is used to conditionally render its children
 * based on the Universal Visual Editor (UVE) mode. It checks if the UVE
 * is in a specified mode and only renders its children in that case.
 *
 * @param {Object} props - The component props.
 * @param {React.ReactNode} props.children - The children to be rendered when the condition is met.
 * @param {UVE_MODE} [props.when=UVE_MODE.EDIT] - The UVE mode in which the children should be rendered.
 * @returns {React.ReactNode | null} The children if the current UVE mode matches the `when` prop, otherwise null.
 *
 * @example
 * // Basic usage: Render content only in edit mode
 * <DotCMSShow when={UVE_MODE.EDIT}>
 *     <div>Edit Mode Content</div>
 * </DotCMSShow>
 *
 * // This will render <div>Edit Mode Content</div> only if the UVE is in edit mode.
 *
 * @example
 * // Render content in preview mode
 * <DotCMSShow when={UVE_MODE.PREVIEW}>
 *     <MyCustomPreviewComponent />
 * </DotCMSShow>
 *
 * // MyCustomPreviewComponent will only be rendered if the UVE is in preview mode.
 *
 * @example
 * // Render content in live mode
 * <DotCMSShow when={UVE_MODE.LIVE}>
 *     <LiveContentComponent />
 * </DotCMSShow>
 *
 * // LiveContentComponent will only be rendered if the UVE is in live mode.
 */
export const DotCMSShow = ({
    children,
    when = UVE_MODE.EDIT
}: DotCMSShowProps): React.ReactNode | null => {
    const show = useDotCMSShowWhen(when);

    if (!show) {
        return null;
    }

    return children;
};

import { useEffect, useState } from 'react';

import { getUVEState } from '@dotcms/uve';
import { UVE_MODE } from '@dotcms/uve/types';

/**
 * DotCMSShowControl component is used to conditionally render its children
 * based on the Universal Visual Editor (UVE) mode. It specifically checks
 * if the UVE is in edit mode and only renders its children in that case.
 *
 * @param {Object} props - The component props.
 * @param {React.ReactNode} props.children - The children to be rendered when in edit mode.
 * @returns {React.ReactNode | null} The children if in edit mode, otherwise null.
 *
 * @example
 * // Basic usage:
 * <DotCMSShowControl>
 *     <div>Edit Mode Content</div>
 * </DotCMSShowControl>
 *
 * // This will render <div>Edit Mode Content</div> only if the UVE is in edit mode.
 *
 * @example
 * // Using with other components:
 * <DotCMSShowControl>
 *     <MyCustomEditorComponent />
 * </DotCMSShowControl>
 *
 * // MyCustomEditorComponent will only be rendered if the UVE is in edit mode.
 */
export const DotCMSShowControl = ({ children }: { children: React.ReactNode }) => {
    const [isEditing, setIsEditing] = useState(false);

    useEffect(() => {
        setIsEditing(getUVEState()?.mode === UVE_MODE.EDIT);
    }, []);

    if (!isEditing) {
        return null;
    }

    return children;
};

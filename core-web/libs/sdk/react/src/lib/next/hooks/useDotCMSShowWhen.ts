import { useEffect, useState } from 'react';

import { UVE_MODE } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';

/**
 * Custom hook to determine if the current UVE (Universal Visual Editor) mode
 * matches the specified mode. This hook is useful for conditionally rendering
 * components based on the UVE mode.
 *
 * @param {UVE_MODE} when - The UVE mode to check against.
 * @returns {boolean} True if the current UVE mode matches the specified mode, otherwise false.
 *
 * @example
 * // Basic usage: Check if the UVE is in edit mode
 * const showInEditMode = useDotCMSShowWhen(UVE_MODE.EDIT);
 * if (showInEditMode) {
 *     // Render edit-specific components
 * }
 *
 * @example
 * // Check if the UVE is in preview mode
 * const showInPreviewMode = useDotCMSShowWhen(UVE_MODE.PREVIEW);
 * if (showInPreviewMode) {
 *     // Render preview-specific components
 * }
 *
 * @example
 * // Check if the UVE is in live mode
 * const showInLiveMode = useDotCMSShowWhen(UVE_MODE.LIVE);
 * if (showInLiveMode) {
 *     // Render live-specific components
 * }
 */
export const useDotCMSShowWhen = (when: UVE_MODE): boolean => {
    const [show, setShow] = useState(false);

    useEffect(() => {
        setShow(getUVEState()?.mode === when);
    }, [when]);

    return show;
};

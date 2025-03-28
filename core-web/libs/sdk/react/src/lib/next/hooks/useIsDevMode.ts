import { useContext, useEffect, useState } from 'react';

import { getUVEState } from '@dotcms/uve';
import { DEVELOPMENT_MODE } from '@dotcms/uve/internal';
import { DotCMSPageRendererMode, UVE_MODE } from '@dotcms/uve/types';

import { DotCMSPageContext } from '../contexts/DotCMSPageContext';

/**
 * @internal
 * A React hook that determines if the current environment is in development mode.
 *
 * The hook returns `true` if either:
 *   - The context mode (or the optional `renderMode` argument) is set to 'development', or
 *   - The application is running inside the DotCMS editor (as determined by `isInsideEditor()`).
 *
 * @param {DotCMSPageRendererMode} [renderMode] - Optional override for the render mode.
 * @returns {boolean} - `true` if in development mode or inside the editor; otherwise, `false`.
 */
export const useIsDevMode = (renderMode?: DotCMSPageRendererMode) => {
    const { mode } = useContext(DotCMSPageContext);

    const effectiveMode = renderMode ?? mode;
    const [isDevMode, setIsDevMode] = useState(effectiveMode === 'development');

    useEffect(() => {
        // Inside UVE we rely on the UVE state to determine if we are in development mode
        if (getUVEState()?.mode) {
            const isUVEInEditor = getUVEState()?.mode === UVE_MODE.EDIT;
            setIsDevMode(isUVEInEditor);

            return;
        }

        const effectiveMode = renderMode ?? mode;
        setIsDevMode(effectiveMode === DEVELOPMENT_MODE);
    }, [renderMode, mode]);

    return isDevMode;
};

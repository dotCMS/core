import { useContext, useEffect, useState } from 'react';

import { isInsideEditor } from '@dotcms/client';

import { DotCMSPageContext, RendererMode } from '../contexts/DotCMSPageContext';

/**
 * A React hook that determines if the current environment is in development mode.
 *
 * The hook returns `true` if either:
 *   - The context mode (or the optional `renderMode` argument) is set to 'development', or
 *   - The application is running inside the DotCMS editor (as determined by `isInsideEditor()`).
 *
 * @param {RendererMode} [renderMode] - Optional override for the render mode.
 * @returns {boolean} - `true` if in development mode or inside the editor; otherwise, `false`.
 */
export const useIsDevMode = (renderMode?: RendererMode) => {
    const { mode } = useContext(DotCMSPageContext);
    const effectiveMode = renderMode ?? mode;

    const [isDevMode, setIsDevMode] = useState(effectiveMode === 'development');

    useEffect(() => {
        setIsDevMode(effectiveMode === 'development' || isInsideEditor());
    }, [effectiveMode]);

    return isDevMode;
};

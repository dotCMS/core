import { useContext, useEffect, useState } from 'react';

import { UVE_MODE } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';
import { DEVELOPMENT_MODE } from '@dotcms/uve/internal';

import { DotCMSPageContext } from '../contexts/DotCMSPageContext';

/**
 * @internal
 * A React hook that determines if the current environment is in development mode.
 *
 * The hook returns `true` if either:
 *   - The application is running inside the DotCMS editor (as determined by `getUVEState()`).
 *
 * @returns {boolean} - `true` if in development mode or inside the editor; otherwise, `false`.
 */
export const useIsDevMode = (): boolean => {
    const { mode } = useContext(DotCMSPageContext);

    const [isDevMode, setIsDevMode] = useState(mode === 'development');

    useEffect(() => {
        // Inside UVE we rely on the UVE state to determine if we are in development mode
        if (getUVEState()?.mode) {
            const isUVEInEditor = getUVEState()?.mode === UVE_MODE.EDIT;
            setIsDevMode(isUVEInEditor);

            return;
        }

        setIsDevMode(mode === DEVELOPMENT_MODE);
    }, [mode]);

    return isDevMode;
};

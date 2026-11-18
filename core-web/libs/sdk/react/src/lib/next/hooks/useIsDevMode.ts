'use client';

import { useContext, useEffect, useState } from 'react';

import { DotCMSPageRendererMode, UVE_MODE } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';
import { DEVELOPMENT_MODE } from '@dotcms/uve/internal';

import { DotCMSPageContext } from '../contexts/DotCMSPageContext';

/**
 * @internal
 *
 * Resolve whether we are rendering in "development" mode — i.e. whether editor metadata
 * (`data-dot-*` attributes, empty-state placeholders, fallback components) should be emitted.
 * Inside the UVE it follows the UVE state (dev when the mode is EDIT); otherwise it follows
 * the renderer `mode` passed to `DotCMSLayoutBody`.
 *
 * @param mode the renderer mode from the page context
 * @returns `true` when editor metadata should be emitted
 */
export const resolveDevMode = (mode: DotCMSPageRendererMode | undefined): boolean => {
    const uveMode = getUVEState()?.mode;

    if (uveMode) {
        return uveMode === UVE_MODE.EDIT;
    }

    return mode === DEVELOPMENT_MODE;
};

/**
 * @internal
 *
 * Owns the development-mode state for one layout tree. Called once by `DotCMSPageProvider`;
 * the result travels down through the page context.
 *
 * `getUVEState()` reads the browser, so it is deliberately resolved in an effect rather than
 * during render: on the server it would always report "not in the editor", and resolving it
 * synchronously on the client would make the first render disagree with the server-rendered
 * markup and trip React's hydration check inside the UVE.
 *
 * @param mode the renderer mode passed to `DotCMSLayoutBody`
 * @returns `true` when editor metadata should be emitted
 */
export const useResolvedDevMode = (mode: DotCMSPageRendererMode | undefined): boolean => {
    const [isDevMode, setIsDevMode] = useState(mode === DEVELOPMENT_MODE);

    useEffect(() => {
        setIsDevMode(resolveDevMode(mode));
    }, [mode]);

    return isDevMode;
};

/**
 * @internal
 *
 * A React hook that determines if the current environment is in development mode.
 *
 * The value is resolved once per layout tree by `DotCMSPageProvider` and shared through the
 * page context, so a page with many containers and contentlets resolves it a single time
 * instead of once per component.
 *
 * @returns {boolean} - `true` if in development mode or inside the editor; otherwise, `false`.
 */
export const useIsDevMode = (): boolean => {
    const { isDevMode } = useContext(DotCMSPageContext);

    return isDevMode;
};

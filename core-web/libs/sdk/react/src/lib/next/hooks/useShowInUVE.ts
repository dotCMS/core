import { useEffect, useState } from 'react';

import { getUVEState } from '@dotcms/uve';
import { UVE_MODE } from '@dotcms/uve/types';

/**
 * Custom hook to determine if the current UVE mode matches the specified mode.
 *
 * @param {UVE_MODE} when - The UVE mode to check against.
 * @returns {boolean} True if the current UVE mode matches the specified mode, otherwise false.
 */
export const useShowInUVE = (when: UVE_MODE): boolean => {
    const [show, setShow] = useState(false);

    useEffect(() => {
        setShow(getUVEState()?.mode === when);
    }, [when]);

    return show;
};

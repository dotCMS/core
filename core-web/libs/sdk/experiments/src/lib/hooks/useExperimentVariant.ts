import { useContext, useEffect, useMemo } from 'react';

import { DotCMSPageAsset, UVE_MODE } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';

import DotExperimentsContext from '../contexts/DotExperimentsContext';

/**
 * A React Hook that determines whether to wait for the correct variant in an A/B testing scenario.
 * This is used to avoid flickering - showing the original content before redirecting to the assigned variant.
 *
 * The hook uses the running experiment id and viewAs (containing variantId) from the provided data.
 * It then works with the DotExperimentsContext to synchronize between the assigned variant and the one requested.
 * If the hook is executed inside an editor or if no running experiment id is provided, it immediately signals not to wait for the variant.
 * Similarly, if the assigned variant matches the requested one, it signals not to wait for the variant.
 * By default, the hook signals to wait for the variant.
 *
 * @param {Object} data - An object containing the runningExperimentId and viewAs (containing variantId).
 * @returns {Object} An object with a function `shouldWaitForVariant` that, when called, returns `true` if it should wait for the correct variant, `false` otherwise.
 */
export const useExperimentVariant = (data: DotCMSPageAsset): { shouldWaitForVariant: boolean } => {
    const dotExperimentInstance = useContext(DotExperimentsContext);

    const { runningExperimentId, viewAs } = data;

    const variantId = viewAs?.variantId;

    // Derive shouldWaitForVariant synchronously using useMemo to avoid
    // synchronous setState calls inside useEffect (set-state-in-effect rule).
    const shouldWaitForVariant = useMemo(() => {
        const isInsideEditor = getUVEState()?.mode === UVE_MODE.EDIT;

        if (isInsideEditor || !runningExperimentId) {
            return false;
        }

        if (!variantId) {
            return false;
        }

        const location = typeof window !== 'undefined' ? window.location : undefined;

        if (location && dotExperimentInstance) {
            const variantAssigned = dotExperimentInstance.getVariantFromHref(location.pathname);

            if (variantAssigned && variantId === variantAssigned.name) {
                // the data requested and the variant assigned is the correct no need to wait
                return false;
            }
        }

        return true;
    }, [dotExperimentInstance, variantId, runningExperimentId]);

    // Emit warning as a side effect (separate from the memoized computation)
    useEffect(() => {
        if (runningExperimentId && !variantId) {
            // eslint-disable-next-line no-console
            console.warn(
                '[DotExperiments] variantId is required but missing. ' +
                    'Please ensure the page data includes variantId in viewAs. ' +
                    'Showing content to prevent blank screen.'
            );
        }
    }, [runningExperimentId, variantId]);

    return { shouldWaitForVariant };
};

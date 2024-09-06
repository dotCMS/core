import { useContext, useEffect, useState } from 'react';

import { isInsideEditor } from '@dotcms/client';

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
export const useExperimentVariant = (data: {
    runningExperimentId?: string;
    viewAs: { variantId: string };
}): { shouldWaitForVariant: boolean } => {
    const dotExperimentInstance = useContext(DotExperimentsContext);

    const { runningExperimentId, viewAs } = data;

    const { variantId } = viewAs;

    // By default, wait for the variant
    const [shouldWaitForVariant, setShouldWaitForVariant] = useState<boolean>(true);

    useEffect(() => {
        if (isInsideEditor() || !runningExperimentId) {
            setShouldWaitForVariant(false);

            return;
        }

        const location = typeof window !== 'undefined' ? window.location : undefined;

        if (location && dotExperimentInstance) {
            const variantAssigned = dotExperimentInstance.getVariantFromHref(location.pathname);

            if (variantAssigned && variantId === variantAssigned.name) {
                // the data requested and the variant assigned is the correct no need to wait
                setShouldWaitForVariant(false);

                return;
            }
        }
    }, [dotExperimentInstance, data]);

    return { shouldWaitForVariant };
};

import { useContext, useEffect, useState } from 'react';

import { isInsideEditor } from '@dotcms/client';

import DotExperimentsContext from '../contexts/DotExperimentsContext';

// TODO: the data type could be get from the SDK Client
export const useExperimentVariant = (data: {
    runningExperimentId?: string;
    viewAs: { variantId: string };
}): { shouldWaitForVariant: () => boolean } => {
    const dotExperimentInstance = useContext(DotExperimentsContext);

    const { runningExperimentId, viewAs } = data;

    const { variantId } = viewAs;

    // By default wait for the variant
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

    return { shouldWaitForVariant: () => shouldWaitForVariant };
};

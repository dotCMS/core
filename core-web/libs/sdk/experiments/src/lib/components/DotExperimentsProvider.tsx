import { ReactElement, ReactNode, useEffect, useState } from 'react';

import { getUVEState } from '@dotcms/uve';

import DotExperimentsContext from '../contexts/DotExperimentsContext';
import { DotExperiments } from '../dot-experiments';
import { useExperiments } from '../hooks/useExperiments';
import { DotExperimentConfig } from '../shared/models';

interface DotExperimentsProviderProps {
    children?: ReactNode;
    config: DotExperimentConfig;
}

/**
 * Internal React context provider used by `withExperiments`.
 *
 * @internal Do not import or use directly — use `withExperiments` from the package entry point.
 *
 * @param props.children - Descendants that need access to the `DotExperiments` instance.
 * @param props.config - Configuration object for `DotExperiments`.
 * @returns The provider component.
 */
export const DotExperimentsProvider = ({
    children,
    config
}: DotExperimentsProviderProps): ReactElement => {
    const [instance, setInstance] = useState<DotExperiments | null>(null);

    // Run Experiments detection
    useExperiments(instance);

    // Initialize the DotExperiments instance
    useEffect(() => {
        const insideEditor = getUVEState()?.mode;

        if (!insideEditor) {
            const dotExperimentsInstance = DotExperiments.getInstance(config);

            dotExperimentsInstance
                .ready()
                .catch((error) => {
                    if (config.debug) {
                        console.error(
                            'DotExperimentsProvider: failed to initialize DotExperiments instance.',
                            error
                        );
                    }
                })
                .finally(() => {
                    // Expose the instance even on failure, so consumers stop waiting and render.
                    setInstance(dotExperimentsInstance);
                });
        } else {
            if (config.debug) {
                console.warn(
                    'DotExperimentsProvider: DotExperiments instance not initialized because it is inside the editor.'
                );
            }
        }
    }, [config]);

    return (
        <DotExperimentsContext.Provider value={instance}>{children}</DotExperimentsContext.Provider>
    );
};

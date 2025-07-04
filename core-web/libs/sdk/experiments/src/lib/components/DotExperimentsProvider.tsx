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
 * `DotExperimentsProvider` is a component that uses React's Context API to provide
 * an instance of `DotExperiments` to all of its descendants.
 *
 *
 * @component
 * @example
 * ```jsx
 *
 * // Your application component
 * function App() {
 *
 * // Configuration options could be taken from environment variables or can send you own.
 *  const experimentConfig = {
 *     apiKey: process.env.NEXT_PUBLIC_EXPERIMENTS_API_KEY ,
 *     server: process.env.NEXT_PUBLIC_DOTCMS_HOST ,
 *     debug: process.env.NEXT_PUBLIC_EXPERIMENTS_DEBUG,
 *     redirectFn: YourRedirectFunction
 *   };
 *
 *   return (
 *     <DotExperimentsProvider config={experimentConfig}>
 *       <Header>
 *           <Navigation items={nav} />
 *         </Header>
 *       <DotcmsLayout  entity={{...}} config={{...}} />
 *     </DotExperimentsProvider>
 *   );
 * }
 * ```
 *
 * @param {object} props - The properties that define the `DotExperimentsProvider`.
 * @param {ReactNode} props.children - The descendants of this provider, which will
 *   have access to the provided `DotExperiments` instance.
 * @param {DotExperimentConfig} props.config - The configuration object for `DotExperiments`.
 *
 * @returns {ReactElement} The provider component, which should wrap the components
 * that need access to the `DotExperiments` instance.
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

            dotExperimentsInstance.ready().then(() => {
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

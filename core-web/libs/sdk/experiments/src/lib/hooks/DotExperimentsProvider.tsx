import { ReactElement, ReactNode, useEffect, useState } from 'react';

import DotExperimentsContext from './DotExperimentsContext';

import { DotExperiments } from '../dot-experiments';
import { DotExperimentConfig } from '../shared/models';

type RedirectFn = () => void;

interface ConfigWithRedirectAndLocation extends DotExperimentConfig {
    redirectFn: RedirectFn;
}

interface DotExperimentsProviderProps {
    children?: ReactNode;
    config: ConfigWithRedirectAndLocation;
}

/**
 * `DotExperimentsProvider` is a component that uses React's Context API to provide
 * an instance of `DotExperiments` to all of its descendants.
 *
 * @component
 * @example
 * ```jsx
 * import { DotExperimentConfig } from '../shared/models';
 *
 * // Your application component
 * function App() {
 *   const config: DotExperimentConfig = // your DotExperiments configuration
 *   return (
 *     <DotExperimentsProvider config={config}>
 *       <YourDescendantComponent />
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
    const redirectFn = config.redirectFn;

    // Initialize the DotExperiments instance
    useEffect(() => {
        const defaultConfig = {
            'api-key': process.env.NEXT_PUBLIC_EXPERIMENTS_API_KEY,
            server: process.env.NEXT_PUBLIC_DOTCMS_HOST,
            debug: process.env.NEXT_PUBLIC_EXPERIMENTS_DEBUG
        };

        const finalConfig = { ...defaultConfig, ...config };
        const dotExperimentsInstance = DotExperiments.getInstance(finalConfig);
        dotExperimentsInstance.ready().then(() => {
            setInstance(dotExperimentsInstance);
        });
    }, [config]);

    // Update the location of the DotExperiments instance when it changes
    useEffect(() => {
        const location = typeof window !== 'undefined' ? window.location : undefined;
        if (instance && location) {
            instance.locationChanged(location, redirectFn);
        }
    }, [instance, redirectFn]);

    return (
        <DotExperimentsContext.Provider value={instance}>{children}</DotExperimentsContext.Provider>
    );
};

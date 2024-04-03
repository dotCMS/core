import { ReactElement, ReactNode, useEffect, useState } from 'react';

import DotExperimentsContext from './DotExperimentsContext';

import { DotExperiments } from '../dot-experiments';
import { DotExperimentConfig } from '../shared/models';

type RedirectFn = () => void;

interface ConfigWithRedirect {
    redirectFn: RedirectFn;
}

interface DotExperimentsProviderProps {
    children?: ReactNode;
    config: ConfigWithRedirect;
}

/**
 * `DotExperimentsProvider` is a component that uses React's Context API to provide
 * an instance of `DotExperiments` to all of its descendants.
 *
 * The location changes are tracked to manage functionality like redirection of the page.

 * The configuration for `DotExperiments` (except `redirectFn`) is set based on the following environment variables:
 * - `api-key`: `NEXT_PUBLIC_EXPERIMENTS_API_KEY` (Analytics API key generated in DotCMS)
 * - `server`: `NEXT_PUBLIC_DOTCMS_HOST` (DotCMS server URL)
 * - `debug`: `NEXT_PUBLIC_EXPERIMENTS_DEBUG`
 *
 * @component
 * @example
 * ```jsx
 *
 * // Your application component
 * function App() {
 *
 * // Other configuration options will be taken from environment variables.
 *  const config = {
 *     redirectFn: YourRedirectFunction
 *   };
 *
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
        const defaultConfig: DotExperimentConfig = {
            'api-key': process.env.NEXT_PUBLIC_EXPERIMENTS_API_KEY || 'default-api-key',
            server: process.env.NEXT_PUBLIC_DOTCMS_HOST || 'default-host',
            debug: process.env.NEXT_PUBLIC_EXPERIMENTS_DEBUG === 'true'
        };

        const finalConfig: DotExperimentConfig = { ...defaultConfig, ...config };
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

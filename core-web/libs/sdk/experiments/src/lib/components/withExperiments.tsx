/* eslint-disable react-hooks/rules-of-hooks */
import React, { ReactNode, useCallback } from 'react';

import { DotCMSLayoutBodyProps } from '@dotcms/react';

import { DotExperimentHandlingComponent } from './DotExperimentHandlingComponent';
import { DotExperimentsProvider } from './DotExperimentsProvider';

import { DotExperimentConfig } from '../shared/models';
import { useMemoizedObject } from '../shared/utils/memoize';

export interface PageProviderProps {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    readonly entity: any;
    readonly children: ReactNode;
}

/**
 * Wraps a given component with experiment handling capabilities using the 'useExperimentVariant' hook.
 * This HOC checks if the entity's assigned experiment variant differs from the currently displayed variant.
 * If they differ, the content is hidden until the correct variant is displayed. Once the assigned variant
 * matches the displayed variant, the content of the WrappedComponent is shown.
 *
 * @param {React.ComponentType<DotcmsPageProps>} WrappedComponent - The component to be enhanced.
 * @param {DotExperimentConfig} config - Configuration for experiment handling, including any necessary
 *        redirection functions or other settings.
 * @returns {React.FunctionComponent<DotcmsPageProps>} A component that wraps the original component,
 *          adding experiment handling based on the specified configuration.
 */
export const withExperiments = (
    WrappedComponent: React.ComponentType<DotCMSLayoutBodyProps>,
    config: DotExperimentConfig
) => {
    // We need to use a custom memoization hook
    // because the useMemo or React.memo lose the reference of the object
    // in each render, causing the experiment handling to be reinitialized.
    const memoizedConfig = useMemoizedObject(config);

    return useCallback(
        (props: DotCMSLayoutBodyProps) => {
            return (
                <DotExperimentsProvider config={memoizedConfig}>
                    <DotExperimentHandlingComponent
                        {...props}
                        WrappedComponent={WrappedComponent}
                    />
                </DotExperimentsProvider>
            );
        },
        [WrappedComponent, memoizedConfig]
    );
};

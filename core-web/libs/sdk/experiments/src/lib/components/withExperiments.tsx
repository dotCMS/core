import React, { ReactNode } from 'react';

import { DotcmsPageProps } from '@dotcms/react';

import { useExperimentVariant } from '../hooks/useExperimentVariant';

export interface PageProviderProps {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    readonly entity: any;
    readonly children: ReactNode;
}

/**
 * High Order Component that enhances a given component (WrappedComponent) with experiments handling.
 * This HOC uses the 'useExperimentVariant' hook to decide whether to hide the content because
 * it has an assigned experiment variant that's different from the currently displayed one.
 * If the assigned variant differs from the displayed one, the HOC hides the content, and an internal
 * effect handles the redirect. On the next render, if the assigned variant matches the displayed one,
 * the HOC shows the content of the WrappedComponent.
 *
 * @param {React.ComponentType} WrappedComponent - The component to be enhanced with experiments handling.
 * @returns {React.FunctionComponent} - A new component that wraps the original component and adds * experiments handling.
 */

export const withExperiments = (WrappedComponent: React.ComponentType<DotcmsPageProps>) => {
    return (props: DotcmsPageProps) => {
        const { entity } = props;

        const { shouldWaitForVariant } = useExperimentVariant(entity);

        if (shouldWaitForVariant) {
            return (
                <div style={{ visibility: 'hidden' }}>
                    <WrappedComponent {...props} />
                </div>
            );
        }

        return <WrappedComponent {...props} />;
    };
};

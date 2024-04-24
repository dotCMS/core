import React, { ReactNode, Suspense } from 'react';

import { useExperimentVariant } from '../hooks/useExperimentVariant';

export interface PageProviderProps {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    readonly entity: any;
    readonly children: ReactNode;
}

/**
 * Renders a DotCMS layout with experiments.
 * This component uses 'useExperimentVariant' to determine whether to hide the content because
 * it has an assigned variant that's different from the currently displayed one.
 * If so, the component hides the content and an internal effect handles the redirect.
 * The next time the check occurs, if the assigned variant matches the displayed one,
 * the component displays the content.
 *
 * @param {PageProviderProps} props - The properties for rendering the DotCMS layout.
 * @returns {JSX.Element} The JSX element representing the DotCMS layout enhanced with experiments.
 * */

export function DotLayoutWithExperiments(props: PageProviderProps): JSX.Element | null {
    const { entity, children } = props;

    const { shouldWaitForVariant } = useExperimentVariant(entity);

    return <Suspense>{shouldWaitForVariant() ? null : children}</Suspense>;
}

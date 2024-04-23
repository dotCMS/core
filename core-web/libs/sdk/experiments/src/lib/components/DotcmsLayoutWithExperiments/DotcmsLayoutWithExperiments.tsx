import React, { ReactNode, Suspense } from 'react';

import { useExperimentVariant } from '@dotcms/experiments';

export interface PageProviderProps {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    readonly entity: any;
    readonly children: ReactNode;
}

/**
 * Renders a DotCMS layout with experiments.
 *
 * @param {PageProviderProps} props - The properties for the DotCMS layout.
 * @returns {JSX.Element} - The JSX element representing the DotCMS layout with experiments.
 */
export function DotcmsLayoutWithExperiments(props: PageProviderProps): JSX.Element | null {
    const { entity, children } = props;

    const { shouldWaitForVariant } = useExperimentVariant(entity);

    return <Suspense>{shouldWaitForVariant() ? null : children}</Suspense>;
}

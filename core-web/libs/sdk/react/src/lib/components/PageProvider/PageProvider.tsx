import { ReactNode } from 'react';

import { PageContext } from '../../contexts/PageContext';
import { DotCMSPageAsset } from '../../models';

export interface PageProviderProps {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    readonly pageContext: any;
    readonly children: ReactNode;
}

export interface DotCMSPageContext {
    /**
     * `components` is a property of the `PageProviderProps` type.
     * It is an object that maps content type variables to their corresponding React components.
     *
     * It will be use to render the contentlets in the page.
     *
     * @property {Object} components
     * @memberof PageProviderProps
     * @type {Object.<string, React.ElementType>}
     */
    components: {
        [contentTypeVariable: string]: React.ElementType;
    };
    pageAsset: DotCMSPageAsset;
    isInsideEditor: boolean;
    // If the page is part of an experiment, this will be the experiment id
    runningExperimentId?: string;
}

/**
 * `PageProvider` is a functional component that provides a context for a DotCMS page.
 * It takes a `PageProviderProps` object as a parameter and returns a JSX element.
 *
 * @category Components
 * @param {PageProviderProps} props - The properties for the PageProvider. Includes an `entity` and `children`.
 * @returns {JSX.Element} - A JSX element that provides a context for a DotCMS page.
 */
export function PageProvider(props: PageProviderProps): JSX.Element {
    const { pageContext, children } = props;

    return <PageContext.Provider value={pageContext}>{children}</PageContext.Provider>;
}

import { ReactNode } from 'react';

import { PageContext } from '../../contexts/PageContext';

export interface PageProviderProps {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    readonly pageContext: any;
    readonly children: ReactNode;
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

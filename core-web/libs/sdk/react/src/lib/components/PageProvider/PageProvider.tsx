import { ReactNode } from 'react';

import { PageContext } from '../../contexts/PageContext';

export interface PageProviderProps {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    readonly entity: any;
    readonly children: ReactNode;
}

export interface ContainerData {
    [key: string]: {
        container: {
            path: string;
            identifier: string;
            maxContentlets: number;
            parentPermissionable: Record<string, string>;
        };
        containerStructures: {
            contentTypeVar: string;
        }[];
        contentlets: {
            [key: string]: {
                contentType: string;
                identifier: string;
                title: string;
                inode: string;
                onNumberOfPages: number;
                baseType: string;
                widgetTitle?: string;
            }[];
        };
    };
}

export interface PageProviderContext {
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
    containers: ContainerData;
    layout: {
        header: boolean;
        footer: boolean;
        body: {
            rows: {
                styleClass: string;
                columns: {
                    styleClass: string;
                    width: number;
                    leftOffset: number;
                    containers: {
                        identifier: string;
                        uuid: string;
                    }[];
                }[];
            }[];
        };
    };
    page: {
        title: string;
        identifier: string;
    };
    viewAs: {
        language: {
            id: string;
        };
        persona: {
            keyTag: string;
        };
        // variant requested
        variantId: string;
    };
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
    const { entity, children } = props;

    return <PageContext.Provider value={entity}>{children}</PageContext.Provider>;
}

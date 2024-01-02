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
                widgetTitle?: string;
            }[];
        };
    };
}

export interface PageProviderContext {
    components: {
        [contentTypeVariable: string]: React.ElementType;
    };
    containers: ContainerData;
    layout: {
        header: boolean;
        footer: boolean;
        body: {
            rows: {
                columns: {
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
    };
}

/**
 * Component in charge of pass the data from the page api to the global context
 *
 * @category Components
 * @export
 * @param {PageProviderProps} props
 * @return {*}  {JSX.Element}
 */
export function PageProvider(props: PageProviderProps): JSX.Element {
    const { entity, children } = props;

    return <PageContext.Provider value={entity}>{children}</PageContext.Provider>;
}

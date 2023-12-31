import { ReactNode, createContext } from 'react';

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

export const PageContext = createContext<PageProviderContext>({
    containers: {},
    components: {},
    layout: {
        header: false,
        footer: false,
        body: {
            rows: [
                {
                    columns: [
                        {
                            width: 0,
                            leftOffset: 0,
                            containers: []
                        }
                    ]
                }
            ]
        }
    },
    page: {
        title: '',
        identifier: ''
    },
    viewAs: {
        language: {
            id: ''
        },
        persona: {
            keyTag: ''
        }
    }
});

export function PageProvider({ entity, children }: PageProviderProps) {
    return <PageContext.Provider value={entity}>{children}</PageContext.Provider>;
}

export default PageProvider;

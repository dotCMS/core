import { ReactNode, createContext } from 'react';

export interface PageProviderProps {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    entity: any;
    children: ReactNode;
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
                identifier: string;
                title: string;
                inode: string;
                widgetTitle?: string;
            }[];
        };
    };
}

export interface PageProviderContext {
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

export const GlobalContext = createContext<PageProviderContext>({
    containers: {},
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
                            containers: [],
                        },
                    ],
                },
            ],
        },
    },
    page: {
        title: '',
        identifier: '',
    },
    viewAs: {
        language: {
            id: '',
        },
        persona: {
            keyTag: '',
        },
    },
});

export function PageProvider({ entity, children }: PageProviderProps) {
    return (
        <GlobalContext.Provider value={entity}>
            {children}
        </GlobalContext.Provider>
    );
}

export default PageProvider;

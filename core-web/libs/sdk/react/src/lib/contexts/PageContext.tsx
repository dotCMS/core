import { createContext } from 'react';

import { PageProviderContext } from '../components/PageProvider/PageProvider';

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

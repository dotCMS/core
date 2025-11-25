import { renderHook } from '@testing-library/react-hooks';
import { ReactNode } from 'react';

import { useDotcmsPageContext } from './useDotcmsPageContext'; // Adjust the import path based on your file structure.

import { PageContext } from '../contexts/PageContext';
import { DotCMSPageContext } from '../models';

const mockContextValue: DotCMSPageContext = {
    components: {},
    isInsideEditor: false,
    pageAsset: {
        containers: {},
        layout: {
            header: false,
            footer: false,
            body: {
                rows: []
            }
        },
        page: {
            title: 'Test Page',
            identifier: 'test-page'
        },
        viewAs: {
            language: {
                id: 'en'
            },
            persona: {
                keyTag: 'persona'
            },
            variantId: 'variant'
        }
    }
};

describe('useDotcmsPageContext', () => {
    it('returns the context value', () => {
        const { result } = renderHook(() => useDotcmsPageContext(), {
            wrapper: ({ children }: { children: ReactNode }) => (
                <PageContext.Provider value={mockContextValue}>{children}</PageContext.Provider>
            )
        });

        expect(result.current).toEqual(mockContextValue);
    });
});

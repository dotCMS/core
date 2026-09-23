import '@testing-library/jest-dom';

import { render, screen } from '@testing-library/react';
import { useContext } from 'react';
import { MockInstance, vi } from 'vitest';

import { DotCMSPageAsset } from '@dotcms/types';
import { ANALYTICS_READY_EVENT } from '@dotcms/uve/internal';

import { DotCMSLayoutBody } from '../../components/DotCMSLayoutBody/DotCMSLayoutBody';
import { DotCMSPageContext } from '../../contexts/DotCMSPageContext';

/**
 * Builds a page whose single container holds `count` contentlets, so the cost of rendering
 * the tree can be measured against the number of contentlets on the page.
 */
const buildPageAsset = (count: number): DotCMSPageAsset => {
    const contentlets = Array.from({ length: count }, (_, index) => ({
        identifier: `contentlet-${index}`,
        inode: `inode-${index}`,
        contentType: 'Banner',
        title: `Contentlet ${index}`
    }));

    return {
        layout: {
            body: {
                rows: [
                    {
                        styleClass: '',
                        columns: [
                            {
                                left: 0,
                                width: 12,
                                leftOffset: 1,
                                styleClass: '',
                                containers: [{ identifier: 'container-1', uuid: '1' }]
                            }
                        ]
                    }
                ]
            }
        },
        containers: {
            'container-1': {
                container: { identifier: 'container-1', maxContentlets: 25 },
                containerStructures: [{ contentTypeVar: 'Banner' }],
                contentlets: { 'uuid-1': contentlets }
            }
        }
    } as unknown as DotCMSPageAsset;
};

const components = {
    Banner: ({ title }: { title: string }) => <div data-testid="banner">{title}</div>
};

describe('DotCMSLayoutBody runtime cost', () => {
    let addEventListenerSpy: MockInstance;
    let getBoundingClientRectSpy: MockInstance;

    beforeEach(() => {
        addEventListenerSpy = vi.spyOn(window, 'addEventListener');
        getBoundingClientRectSpy = vi.spyOn(Element.prototype, 'getBoundingClientRect');
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    const analyticsListenerCount = () =>
        addEventListenerSpy.mock.calls.filter(([event]) => event === ANALYTICS_READY_EVENT).length;

    describe('analytics readiness', () => {
        test('should register a single analytics listener for the whole layout tree', () => {
            render(
                <DotCMSLayoutBody
                    page={buildPageAsset(10)}
                    components={components}
                    mode="production"
                />
            );

            expect(screen.getAllByTestId('banner')).toHaveLength(10);
            expect(analyticsListenerCount()).toBe(1);
        });

        test('should not scale the number of listeners with the number of contentlets', () => {
            const { unmount } = render(
                <DotCMSLayoutBody
                    page={buildPageAsset(3)}
                    components={components}
                    mode="production"
                />
            );
            const withThree = analyticsListenerCount();
            unmount();
            addEventListenerSpy.mockClear();

            render(
                <DotCMSLayoutBody
                    page={buildPageAsset(30)}
                    components={components}
                    mode="production"
                />
            );

            expect(analyticsListenerCount()).toBe(withThree);
        });
    });

    describe('editor placeholder measurement', () => {
        test('should not measure contentlets in production mode', () => {
            render(
                <DotCMSLayoutBody
                    page={buildPageAsset(10)}
                    components={components}
                    mode="production"
                />
            );

            // getBoundingClientRect() forces a synchronous layout. It only feeds the editor's
            // empty-contentlet placeholder, so production must never pay for it.
            expect(getBoundingClientRectSpy).not.toHaveBeenCalled();
        });

        test('should measure contentlets in development mode', () => {
            render(
                <DotCMSLayoutBody
                    page={buildPageAsset(2)}
                    components={components}
                    mode="development"
                />
            );

            expect(getBoundingClientRectSpy).toHaveBeenCalled();
        });
    });
});

describe('page context stability', () => {
    // A fresh context value re-renders every container and contentlet in the tree, so the
    // provider memoizes it. That memo is only as stable as its dependencies: when `components`
    // and `slots` defaulted to inline `{}` literals, each render produced new objects and the
    // memo never held.
    test('should keep one context value across re-renders', () => {
        const values: unknown[] = [];
        // Stable across renders, the way a real app passes a page asset and a module-level
        // component map. The memo can only hold if its inputs do.
        const page = buildPageAsset(1);
        const components = {
            Banner: () => {
                values.push(useContext(DotCMSPageContext));

                return null;
            }
        };

        const { rerender } = render(
            <DotCMSLayoutBody page={page} components={components} mode="production" />
        );
        rerender(<DotCMSLayoutBody page={page} components={components} mode="production" />);

        expect(values.length).toBeGreaterThan(1);
        expect(new Set(values).size).toBe(1);
    });
});

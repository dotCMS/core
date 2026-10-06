import { renderHook } from '@testing-library/react-hooks';
import React from 'react';
import { vi } from 'vitest';

import { DotCMSPageAsset, UVE_MODE, UVEState } from '@dotcms/types';
import * as uve from '@dotcms/uve';

import { useExperimentVariant } from './useExperimentVariant';

import MockDotExperimentsContext from '../contexts/DotExperimentsContext';
import { EXPERIMENT_DEFAULT_VARIANT_NAME } from '../shared/constants';

interface WrapperProps {
    children: React.ReactNode;
}

// The context is built INSIDE the factory. `vi.mock` is hoisted above every import,
// and the hook under test pulls this module in while its own module is evaluating —
// before any top-level statement of this spec has run. A factory calling a top-level
// helper therefore hit its temporal dead zone and the file died with "Cannot access
// 'createMockDotExperimentsContext' before initialization".
vi.mock('../contexts/DotExperimentsContext', async () => {
    const react = await vi.importActual<typeof import('react')>('react');

    const mockGetVariantFromHref = vi.fn().mockImplementation(() => ({ name: 'variant-1' }));

    return {
        __esModule: true,
        default: react.createContext({ getVariantFromHref: mockGetVariantFromHref })
    };
});

const wrapper = ({ children }: WrapperProps) => (
    // eslint-disable-next-line react-hooks/rules-of-hooks
    <MockDotExperimentsContext.Provider value={React.useContext(MockDotExperimentsContext)}>
        {children}
    </MockDotExperimentsContext.Provider>
);

describe('useExperimentVariant', () => {
    describe('shouldWaitForVariant `false`', () => {
        it('if is insideEditor is `false`', () => {
            const mockData = {
                runningExperimentId: '1',
                viewAs: { variantId: '1' }
            } as DotCMSPageAsset;

            vi.spyOn(uve, 'getUVEState').mockReturnValue({ mode: UVE_MODE.EDIT } as UVEState);

            const { result } = renderHook(() => useExperimentVariant(mockData));

            const { shouldWaitForVariant } = result.current;

            expect(shouldWaitForVariant).toBe(false);
        });

        it(' if is inside UVE in PREVIEW mode (not just EDIT)', () => {
            const mockData = {
                runningExperimentId: '1',
                viewAs: { variantId: '1' }
            } as DotCMSPageAsset;

            vi.spyOn(uve, 'getUVEState').mockReturnValue({ mode: UVE_MODE.PREVIEW } as UVEState);

            const { result } = renderHook(() => useExperimentVariant(mockData));

            const { shouldWaitForVariant } = result.current;

            expect(shouldWaitForVariant).toBe(false);
        });

        it(' if data is undefined (e.g. waiting on the UVE editor to resolve a draft page)', () => {
            vi.spyOn(uve, 'getUVEState').mockReturnValue(undefined);

            const { result } = renderHook(() => useExperimentVariant(undefined));

            expect(result.current.shouldWaitForVariant).toBe(false);
        });

        it(' if `runningExperimentId` is undefined', () => {
            const mockData = {
                viewAs: { variantId: EXPERIMENT_DEFAULT_VARIANT_NAME }
            } as DotCMSPageAsset;

            vi.spyOn(uve, 'getUVEState').mockReturnValue(undefined);

            const { result } = renderHook(() => useExperimentVariant(mockData));

            const { shouldWaitForVariant } = result.current;

            expect(shouldWaitForVariant).toBe(false);
        });

        it(' if VariantId get from `PageApi` is same of VariantAssigned', () => {
            vi.spyOn(uve, 'getUVEState').mockReturnValue(undefined);

            const { result } = renderHook(
                () =>
                    useExperimentVariant({
                        runningExperimentId: 'exp-id',
                        viewAs: { variantId: 'variant-1' }
                    } as DotCMSPageAsset),
                { wrapper }
            );

            expect(result.current.shouldWaitForVariant).toBe(false);
        });

        describe('shouldWaitForVariant `true`', () => {
            it(' if VariantId get from `PageApi` is different of VariantAssigned', () => {
                vi.spyOn(uve, 'getUVEState').mockReturnValue(undefined);

                const { result } = renderHook(
                    () =>
                        useExperimentVariant({
                            runningExperimentId: 'exp-id',
                            viewAs: { variantId: EXPERIMENT_DEFAULT_VARIANT_NAME }
                        } as DotCMSPageAsset),
                    { wrapper }
                );

                expect(result.current.shouldWaitForVariant).toBe(true);
            });
        });
    });
});

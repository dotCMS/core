import { renderHook } from '@testing-library/react-hooks';
import React from 'react';

import * as dotcmsClient from '@dotcms/client';

import { useExperimentVariant } from './useExperimentVariant';

import MockDotExperimentsContext from '../contexts/DotExperimentsContext';
import { EXPERIMENT_DEFAULT_VARIANT_NAME } from '../shared/constants';
import { LocationMock } from '../shared/mocks/mock';

interface WrapperProps {
    children: React.ReactNode;
}

const createMockDotExperimentsContext = (variantResponse: unknown) => {
    const mockGetVariantFromHref = jest.fn().mockImplementation(() => variantResponse);

    return React.createContext({
        getVariantFromHref: mockGetVariantFromHref
    });
};

jest.mock('../contexts/DotExperimentsContext', () => ({
    __esModule: true,
    default: createMockDotExperimentsContext({ name: 'variant-1' })
}));

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
            };

            jest.spyOn(dotcmsClient, 'isInsideEditor').mockReturnValue(true);

            const { result } = renderHook(() => useExperimentVariant(mockData));

            const { shouldWaitForVariant } = result.current;

            expect(shouldWaitForVariant).toBe(false);
        });

        it(' if `runningExperimentId` is undefined', () => {
            const mockData = {
                viewAs: { variantId: EXPERIMENT_DEFAULT_VARIANT_NAME }
            };

            jest.spyOn(dotcmsClient, 'isInsideEditor').mockReturnValue(false);

            const { result } = renderHook(() => useExperimentVariant(mockData));

            const { shouldWaitForVariant } = result.current;

            expect(shouldWaitForVariant).toBe(false);
        });

        it(' if VariantId get from `PageApi` is same of VariantAssigned', () => {
            const locationSpy = jest.spyOn(window, 'location', 'get');

            const locationMock = { ...LocationMock, pathname: '/blog' };

            locationSpy.mockReturnValue(locationMock);

            jest.spyOn(dotcmsClient, 'isInsideEditor').mockReturnValue(false);

            const { result } = renderHook(
                () =>
                    useExperimentVariant({
                        runningExperimentId: 'exp-id',
                        viewAs: { variantId: 'variant-1' }
                    }),
                { wrapper }
            );

            expect(result.current.shouldWaitForVariant).toBe(false);
        });

        describe('shouldWaitForVariant `true`', () => {
            it(' if VariantId get from `PageApi` is different of VariantAssigned', () => {
                const locationSpy = jest.spyOn(window, 'location', 'get');

                const locationMock = { ...LocationMock, pathname: '/blog' };

                locationSpy.mockReturnValue(locationMock);

                jest.spyOn(dotcmsClient, 'isInsideEditor').mockReturnValue(false);

                const { result } = renderHook(
                    () =>
                        useExperimentVariant({
                            runningExperimentId: 'exp-id',
                            viewAs: { variantId: EXPERIMENT_DEFAULT_VARIANT_NAME }
                        }),
                    { wrapper }
                );

                expect(result.current.shouldWaitForVariant).toBe(true);
            });
        });
    });
});

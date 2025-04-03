import { expect } from '@jest/globals';
import { Spectator, byTestId, createRoutingFactory } from '@ngneat/spectator/jest';

import * as uve from '@dotcms/uve';
import { UVE_MODE } from '@dotcms/uve/types';

import { PageErrorMessageComponent } from './components/page-error-message/page-error-message.component';
import { RowComponent } from './components/row/row.component';
import { DotCMSLayoutBodyComponent } from './dotcms-layout-body.component';

import { DotCMSPageComponent } from '../../models';
import { DotCMSStore } from '../../store/dotcms.store';
import { PageResponseMock } from '../../utils/testing.utils';

jest.mock('@dotcms/uve', () => ({
    getUVEState: jest.fn()
}));

const components: DotCMSPageComponent = {
    'dotcms-row': RowComponent as unknown as Promise<typeof RowComponent>
};

describe('DotCMSLayoutBodyComponent', () => {
    let spectator: Spectator<DotCMSLayoutBodyComponent>;
    let dotCMSStore: jest.Mocked<DotCMSStore>;
    const getUVEStateMock = uve.getUVEState as jest.Mock;
    const createComponent = createRoutingFactory({
        component: DotCMSLayoutBodyComponent,
        providers: [DotCMSStore]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                page: PageResponseMock,
                components,
                mode: 'development'
            }
        });

        dotCMSStore = spectator.inject(DotCMSStore, true);
    });

    it('should render rows', () => {
        spectator.detectChanges();

        expect(spectator.queryAll('.dot-row-container').length).toBe(3);
    });

    it('should call setStore on changes', () => {
        const setStoreSpy = jest.spyOn(dotCMSStore, 'setStore');

        spectator.component.ngOnChanges();

        expect(setStoreSpy).toHaveBeenCalledWith({
            page: PageResponseMock,
            components,
            mode: 'development'
        });
    });

    it('should show page error message if page is not found and is on development mode', () => {
        spectator.setInput({ page: null, mode: 'development' });
        spectator.detectChanges();

        expect(spectator.query(byTestId('error-message'))).toBeTruthy();
    });

    it('should not show page error message if page is not found and is on production mode', () => {
        spectator.setInput({ page: null, mode: 'production' });
        spectator.detectChanges();

        expect(spectator.query(byTestId('error-message'))).toBeFalsy();
    });

    it('should show page error inside UVE on EDIT_MODE', () => {
        getUVEStateMock.mockReturnValue({
            mode: UVE_MODE.EDIT,
            languageId: 'en',
            persona: 'admin',
            variantName: 'default',
            experimentId: '123',
            publishDate: new Date().toISOString()
        });

        spectator.setInput({ page: null });
        spectator.detectChanges();

        expect(spectator.query(PageErrorMessageComponent)).toBeTruthy();
    });
});

import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { fakeAsync, tick } from '@angular/core/testing';
import { FormControl } from '@angular/forms';

import { Paginator } from 'primeng/paginator';

import { DotMessageService } from '@dotcms/data-access';

import { EditEmaPaletteContentletsComponent } from './edit-ema-palette-contentlets.component';

import { CONTENTLETS_MOCK } from '../../edit-ema-palette.component.spec';
import {
    EditEmaPaletteStoreStatus,
    PALETTE_PAGINATOR_ITEMS_PER_PAGE
} from '../../store/edit-ema-palette.store';

const CONTENTLETS_PROP_MOCK = {
    items: CONTENTLETS_MOCK,
    totalRecords: 10,
    itemsPerPage: PALETTE_PAGINATOR_ITEMS_PER_PAGE
};

describe('EditEmaPaletteContentletsComponent', () => {
    let spectator: Spectator<EditEmaPaletteContentletsComponent>;

    const createComponent = createComponentFactory({
        component: EditEmaPaletteContentletsComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: {
                    get() {
                        return 'Sample';
                    }
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                contentlets: {
                    ...CONTENTLETS_PROP_MOCK
                },
                control: new FormControl(''),
                paletteStatus: EditEmaPaletteStoreStatus.LOADED
            }
        });
    });

    it('should emit showContentTypes event on backToContentTypes', () => {
        const spy = jest.spyOn(spectator.component.showContentTypes, 'emit');
        const button = spectator.query(byTestId('contentlet-back-button'));
        spectator.click(button);
        expect(spy).toHaveBeenCalled();
    });

    it('should emit paginate event with filter on onPaginate', () => {
        const spyEmit = jest.spyOn(spectator.component.paginate, 'emit');

        const mock_filter = {
            query: 'test',
            contentTypeVarName: 'test'
        };
        spectator.setInput('contentlets', {
            ...CONTENTLETS_PROP_MOCK,
            filter: { ...mock_filter }
        });
        spectator.triggerEventHandler(Paginator, 'onPageChange', { page: 1 });
        expect(spyEmit).toHaveBeenCalledWith({ page: 1, ...mock_filter });
    });

    it('should render contentlet list with data-item attribute', () => {
        const dataItem = spectator.query('[data-testId="contentlet-0"]').getAttribute('data-item');
        const data = JSON.parse(dataItem);
        expect(spectator.query('[data-testId="contentlet-0"]')).toBeTruthy();
        expect(spectator.query('[data-testId="contentlet-0"]')).toHaveAttribute('data-item');
        expect(data).toEqual({
            contentlet: {
                identifier: CONTENTLETS_MOCK[0].identifier,
                contentType: CONTENTLETS_MOCK[0].contentType,
                baseType: CONTENTLETS_MOCK[0].baseType,
                title: CONTENTLETS_MOCK[0].title,
                inode: CONTENTLETS_MOCK[0].inode
            },
            move: false
        });
    });

    it('should emit search event on search contentlet', fakeAsync(() => {
        const searchSpy = jest.spyOn(spectator.component.search, 'emit');
        spectator.typeInElement('test', spectator.query(byTestId('contentlet-search')));
        tick(1100); // For debounce time
        expect(searchSpy).toHaveBeenCalledWith('test');
    }));
});

import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';

import { FormControl } from '@angular/forms';

import { DotMessageService } from '@dotcms/data-access';

import { EditEmaPaletteContentletsComponent } from './edit-ema-palette-contentlets.component';

import { PALETTE_PAGINATOR_ITEMS_PER_PAGE } from '../../shared/edit-ema-palette.const';
import { EditEmaPaletteStoreStatus } from '../../shared/edit-ema-palette.enums';
import { CONTENTLETS_MOCK } from '../../shared/edit-ema-palette.mocks';

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
                    items: CONTENTLETS_MOCK,
                    totalRecords: 10,
                    itemsPerPage: PALETTE_PAGINATOR_ITEMS_PER_PAGE
                },
                control: new FormControl(''),
                paletteStatus: EditEmaPaletteStoreStatus.LOADED
            }
        });
    });

    it('should emit dragStart event on drag start', () => {
        const dragSpy = jest.spyOn(spectator.component.dragStart, 'emit');
        spectator.triggerEventHandler('.contentlet-card', 'dragstart', { inode: '123' });
        expect(dragSpy).toHaveBeenCalledWith({ inode: '123' });
    });

    it('should emit dragEnd event on drag end', () => {
        const dragSpy = jest.spyOn(spectator.component.dragEnd, 'emit');
        spectator.triggerEventHandler('.contentlet-card', 'dragend', { inode: '123' });
        expect(dragSpy).toHaveBeenCalledWith({ inode: '123' });
    });

    it('should emit showContentTypes event on backToContentTypes', () => {
        const spy = jest.spyOn(spectator.component.showContentTypes, 'emit');
        const button = spectator.query('.p-button-rounded');
        spectator.click(button);
        expect(spy).toHaveBeenCalled();
    });

    it('should emit paginate event with filter on onPaginate', () => {
        const event = { page: 1 };
        const filter = { query: 'sample', contentTypeVarName: 'sample' };

        const spy = jest.spyOn(spectator.component.paginate, 'emit');
        spectator.component.onPaginate(event, filter);
        expect(spy).toHaveBeenCalledWith({ ...event, ...filter });
    });

    it('should render contentlet list', () => {
        expect(spectator.query('.contentlet-card')).toBeTruthy();
    });

    it('should the contentlet list item have data-item attribute', () => {
        expect(spectator.query('.contentlet-card')).toHaveAttribute('data-item');
    });
});

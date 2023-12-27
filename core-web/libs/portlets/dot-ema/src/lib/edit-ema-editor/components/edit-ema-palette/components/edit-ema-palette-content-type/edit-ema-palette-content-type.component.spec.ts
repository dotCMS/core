import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { FormControl } from '@angular/forms';

import { DotMessageService } from '@dotcms/data-access';

import { EditEmaPaletteContentTypeComponent } from './edit-ema-palette-content-type.component';

import { EditEmaPaletteStoreStatus } from '../../shared/edit-ema-palette.enums';
import { CONTENT_TYPE_MOCK } from '../../shared/edit-ema-palette.mocks';

describe('EditEmaPaletteContentTypeComponent', () => {
    let spectator: Spectator<EditEmaPaletteContentTypeComponent>;

    const createComponent = createComponentFactory({
        component: EditEmaPaletteContentTypeComponent,
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
                contentTypes: CONTENT_TYPE_MOCK,
                filter: '',
                control: new FormControl(''),
                paletteStatus: EditEmaPaletteStoreStatus.LOADED
            }
        });
    });

    it('should emit dragStart event on drag start', () => {
        const dragSpy = jest.spyOn(spectator.component.dragStart, 'emit');

        spectator.triggerEventHandler('.content-type-card', 'dragstart', {
            variable: 'test',
            name: 'Test'
        });
        expect(dragSpy).toHaveBeenCalledWith({ variable: 'test', name: 'Test' });
    });

    it('should emit dragEnd event on drag end', () => {
        const dragSpy = jest.spyOn(spectator.component.dragEnd, 'emit');

        spectator.triggerEventHandler('.content-type-card', 'dragend', {
            variable: 'test',
            name: 'Test'
        });
        expect(dragSpy).toHaveBeenCalledWith({ variable: 'test', name: 'Test' });
    });

    it('should emit showContentlets event with contentTypeName', () => {
        const contentTypeName = 'exampleContentType';

        const spy = jest.spyOn(spectator.component.showContentlets, 'emit');
        spectator.component.showContentletsFromContentType(contentTypeName);
        expect(spy).toHaveBeenCalledWith(contentTypeName);
    });

    it('should render the content type list', () => {
        expect(spectator.query('.content-type-card')).not.toBeNull();
    });

    it('should the content type list hace data-item attribute', () => {
        expect(spectator.query('.content-type-card')).toHaveAttribute('data-item');
    });
});

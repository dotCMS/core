import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { fakeAsync, tick } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';

import { EditEmaPaletteContentTypeComponent } from './edit-ema-palette-content-type.component';

import { EditEmaPaletteStoreStatus } from '../../store/edit-ema-palette.store';

export const CONTENT_TYPE_MOCK = [
    {
        name: 'Test Content Type',
        variable: 'Test1',
        icon: 'icon'
    },
    {
        name: 'Test Content Type 2',
        variable: 'Test2',
        icon: 'icon'
    }
];

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
                paletteStatus: EditEmaPaletteStoreStatus.LOADED
            }
        });
    });

    it('should emit dragStart event on drag start', () => {
        const dragSpy = jest.spyOn(spectator.component.dragStart, 'emit');

        spectator.triggerEventHandler('[data-testId="content-type-0"]', 'dragstart', {
            variable: 'test',
            name: 'Test'
        });
        expect(dragSpy).toHaveBeenCalledWith({ variable: 'test', name: 'Test' });
    });

    it('should emit dragEnd event on drag end', () => {
        const dragSpy = jest.spyOn(spectator.component.dragEnd, 'emit');

        spectator.triggerEventHandler('[data-testId="content-type-0"]', 'dragend', {
            variable: 'test',
            name: 'Test'
        });
        expect(dragSpy).toHaveBeenCalledWith({ variable: 'test', name: 'Test' });
    });

    it('should emit showContentlets event with contentTypeName', () => {
        const spy = jest.spyOn(spectator.component.showContentlets, 'emit');
        spectator.click('[data-testId="content-type-0-button-go-content"]');
        expect(spy).toHaveBeenCalledWith('Test1');
    });

    it('should render the content type list with data-item attribute', () => {
        expect(spectator.query('[data-testId="content-type-0"]')).not.toBeNull();
        expect(spectator.query('[data-testId="content-type-0"]')).toHaveAttribute('data-item');
    });

    it('should emit search event on search', fakeAsync(() => {
        const searchSpy = jest.spyOn(spectator.component.search, 'emit');
        spectator.typeInElement('test', spectator.query(byTestId('content-type-search')));
        tick(1100); // For debounce time
        expect(searchSpy).toHaveBeenCalledWith('test');
    }));
});

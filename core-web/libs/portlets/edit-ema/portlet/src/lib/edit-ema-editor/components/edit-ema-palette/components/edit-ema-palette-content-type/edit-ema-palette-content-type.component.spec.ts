import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { fakeAsync, tick } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';

import { EditEmaPaletteContentTypeComponent } from './edit-ema-palette-content-type.component';

import { EditEmaPaletteStoreStatus } from '../../store/edit-ema-palette.store';

export const CONTENT_TYPE_MOCK = [
    {
        name: 'Test Content Type',
        variable: 'Test1',
        icon: 'icon',
        baseType: 'CONTENT'
    },
    {
        name: 'Test Content Type 2',
        variable: 'Test2',
        icon: 'icon',
        baseType: 'CONTENT'
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

    it('should emit showContentlets event with contentTypeName', () => {
        const spy = jest.spyOn(spectator.component.showContentlets, 'emit');
        spectator.click('[data-testId="content-type-0-button-go-content"]');
        expect(spy).toHaveBeenCalledWith('Test1');
    });

    it('should render the content type list with data-item attribute', () => {
        const dataItem = spectator
            .query('[data-testId="content-type-0"]')
            .getAttribute('data-item');
        const data = JSON.parse(dataItem);
        expect(spectator.query('[data-testId="content-type-0"]')).not.toBeNull();
        expect(spectator.query('[data-testId="content-type-0"]')).toHaveAttribute('data-item');
        expect(data).toEqual({
            contentType: {
                variable: 'Test1',
                name: 'Test Content Type',
                baseType: 'CONTENT'
            },
            move: false
        });
    });

    it('should emit search event on search', fakeAsync(() => {
        const searchSpy = jest.spyOn(spectator.component.search, 'emit');
        spectator.typeInElement('test', spectator.query(byTestId('content-type-search')));
        tick(1100); // For debounce time
        expect(searchSpy).toHaveBeenCalledWith('test');
    }));
});

import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of as observableOf } from 'rxjs';

import { By } from '@angular/platform-browser';

import { SelectItem } from 'primeng/api';

import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentTypeSelectorComponent } from './dot-content-type-selector.component';

class MockDotContentTypeService {
    getContentTypes = jest.fn().mockReturnValue(
        observableOf([
            { name: 'FORM', variable: 'Form' },
            { name: 'WIDGET', variable: 'Widget' }
        ])
    );
}

function getInputValue(instance: unknown, key: string): unknown {
    const value = (instance as Record<string, unknown>)[key];
    return typeof value === 'function' ? value() : value;
}

describe('DotContentTypeSelectorComponent', () => {
    let spectator: Spectator<DotContentTypeSelectorComponent>;
    const allContentTypesItem: SelectItem = { label: 'Any Content Type', value: '' };
    const messageServiceMock = new MockDotMessageService({
        'contenttypes.selector.any.content.type': 'Any Content Type'
    });

    const createComponent = createComponentFactory({
        component: DotContentTypeSelectorComponent,
        providers: [
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: DotContentTypeService, useClass: MockDotContentTypeService }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: true });
    });

    function getSelect(): ReturnType<typeof spectator.debugElement.query> {
        return spectator.debugElement.query(By.css('p-select'));
    }

    it('should emit the selected content type', () => {
        const pSelect = getSelect();
        expect(pSelect).toBeTruthy();
        jest.spyOn(spectator.component.selected, 'emit');
        jest.spyOn(spectator.component, 'change');

        pSelect.triggerEventHandler('onChange', allContentTypesItem);

        expect(spectator.component.change).toHaveBeenCalledWith(allContentTypesItem);
        expect(spectator.component.change).toHaveBeenCalledTimes(1);
        expect(spectator.component.selected.emit).toHaveBeenCalledWith(allContentTypesItem.value);
        expect(spectator.component.selected.emit).toHaveBeenCalledTimes(1);
    });

    it('should add All Content Types option as first position', (done) => {
        spectator.component.options$.subscribe((options) => {
            expect(options[0]).toEqual(allContentTypesItem);
            done();
        });
    });

    it('should set attributes to p-select', () => {
        const pSelectEl = getSelect();
        expect(pSelectEl).toBeTruthy();
        const selectInstance = pSelectEl.componentInstance as Record<string, unknown>;
        expect(getInputValue(selectInstance, 'filter')).toBe(true);
        expect(getInputValue(selectInstance, 'filterBy')).toBe('label');
        expect(getInputValue(selectInstance, 'showClear')).toBe(true);
        expect(getInputValue(selectInstance, 'resetFilterOnHide')).toBe(true);
    });
});

import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of as observableOf } from 'rxjs';

import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { SelectItem } from 'primeng/api';

import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotBaseTypeSelectorComponent } from './dot-base-type-selector.component';

const allContentTypesItem: SelectItem = { label: 'Any Content Type', value: '' };

class MockDotContentTypeService {
    getAllContentTypes = jest.fn().mockReturnValue(
        observableOf([
            { name: 'FORM', label: 'Form' },
            { name: 'WIDGET', label: 'Widget' }
        ])
    );
}

describe('DotBaseTypeSelectorComponent', () => {
    let spectator: Spectator<DotBaseTypeSelectorComponent>;
    const messageServiceMock = new MockDotMessageService({
        'contenttypes.selector.any.content.type': 'Any Content Type'
    });

    const createComponent = createComponentFactory({
        component: DotBaseTypeSelectorComponent,
        imports: [BrowserAnimationsModule],
        providers: [
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: DotContentTypeService, useClass: MockDotContentTypeService }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should emit the selected content type', () => {
        spectator.detectChanges();
        const pSelect = spectator.debugElement.query(By.css('p-select'));
        jest.spyOn(spectator.component.selected, 'emit');
        jest.spyOn(spectator.component, 'change');
        const selectChangeEvent = { value: allContentTypesItem.value };
        pSelect.triggerEventHandler('onChange', selectChangeEvent);

        expect(spectator.component.change).toHaveBeenCalledWith(selectChangeEvent);
        expect(spectator.component.change).toHaveBeenCalledTimes(1);
        expect(spectator.component.selected.emit).toHaveBeenCalledWith(allContentTypesItem.value);
        expect(spectator.component.selected.emit).toHaveBeenCalledTimes(1);
    });

    it('should add All Content Types option as first position', (done) => {
        spectator.detectChanges();

        spectator.component.options.subscribe((options) => {
            expect(options[0]).toEqual(allContentTypesItem);
            done();
        });
    });

    it('should set fixed width to dropdown', () => {
        spectator.detectChanges();
        const pSelectElement = spectator.query('p-select') as HTMLElement;
        expect(pSelectElement).toBeTruthy();
        // Template uses class="w-38.75" for width; in JSDOM we assert the class is present
        expect(pSelectElement?.classList?.contains('w-38.75')).toBe(true);
    });
});

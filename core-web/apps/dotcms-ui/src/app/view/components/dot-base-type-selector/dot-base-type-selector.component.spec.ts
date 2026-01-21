import { of as observableOf } from 'rxjs';

import { DebugElement, Injectable } from '@angular/core';
import { ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { SelectItem } from 'primeng/api';
import { Select } from 'primeng/select';

import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotBaseTypeSelectorComponent } from './dot-base-type-selector.component';

import { DOTTestBed } from '../../../test/dot-test-bed';

@Injectable()
class MockDotContentTypeService {
    getAllContentTypes = jest.fn().mockReturnValue(
        observableOf([
            { name: 'FORM', label: 'Form' },
            { name: 'WIDGET', label: 'Widget' }
        ])
    );
}

describe('DotBaseTypeSelectorComponent', () => {
    let component: DotBaseTypeSelectorComponent;
    let fixture: ComponentFixture<DotBaseTypeSelectorComponent>;
    let de: DebugElement;
    const allContentTypesItem: SelectItem = { label: 'Any Content Type', value: '' };
    const messageServiceMock = new MockDotMessageService({
        'contenttypes.selector.any.content.type': 'Any Content Type'
    });

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            imports: [DotBaseTypeSelectorComponent, BrowserAnimationsModule],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: DotContentTypeService,
                    useClass: MockDotContentTypeService
                }
            ]
        });

        fixture = DOTTestBed.createComponent(DotBaseTypeSelectorComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
    });

    it('should emit the selected content type', () => {
        fixture.detectChanges();
        const pSelect: DebugElement = de.query(By.css('p-select'));
        jest.spyOn(component.selected, 'emit');
        jest.spyOn(component, 'change');
        const selectChangeEvent = { value: allContentTypesItem.value };
        pSelect.triggerEventHandler('onChange', selectChangeEvent);

        expect(component.change).toHaveBeenCalledWith(selectChangeEvent);
        expect(component.change).toHaveBeenCalledTimes(1);
        expect(component.selected.emit).toHaveBeenCalledWith(allContentTypesItem.value);
        expect(component.selected.emit).toHaveBeenCalledTimes(1);
    });

    it('should add All Content Types option as first position', () => {
        fixture.detectChanges();

        component.options.subscribe((options) => {
            expect(options[0]).toEqual(allContentTypesItem);
        });
    });

    it('should set fixed width to dropdown', () => {
        fixture.detectChanges();
        const pSelectElement = de.query(By.css('p-select')).nativeElement;
        expect(pSelectElement.style.width).toBe('155px');
    });
});

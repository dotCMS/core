import { of as observableOf } from 'rxjs';

import { DebugElement, Injectable } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { SelectItem } from 'primeng/api';
import { Dropdown, DropdownModule } from 'primeng/dropdown';

import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentTypeSelectorComponent } from './dot-content-type-selector.component';

@Injectable()
class MockDotContentTypeService {
    getContentTypes = jest.fn().mockReturnValue(
        observableOf([
            { name: 'FORM', variable: 'Form' },
            { name: 'WIDGET', variable: 'Widget' }
        ])
    );
}

describe('DotContentTypeSelectorComponent', () => {
    let component: DotContentTypeSelectorComponent;
    let fixture: ComponentFixture<DotContentTypeSelectorComponent>;
    let de: DebugElement;
    const allContentTypesItem: SelectItem = { label: 'Any Content Type', value: '' };
    const messageServiceMock = new MockDotMessageService({
        'contenttypes.selector.any.content.type': 'Any Content Type'
    });

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotContentTypeSelectorComponent],
            imports: [BrowserAnimationsModule, DropdownModule],
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

        fixture = TestBed.createComponent(DotContentTypeSelectorComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
    });

    it('should emit the selected content type', () => {
        const pDropDown: DebugElement = de.query(By.css('p-dropdown'));
        jest.spyOn(component.selected, 'emit');
        jest.spyOn(component, 'change');
        pDropDown.triggerEventHandler('onChange', allContentTypesItem);

        expect(component.change).toHaveBeenCalledWith(allContentTypesItem);
        expect(component.selected.emit).toHaveBeenCalledWith(allContentTypesItem.value);
    });

    it('should add All Content Types option as first position', () => {
        fixture.detectChanges();

        component.options$.subscribe((options) => {
            expect(options[0]).toEqual(allContentTypesItem);
        });
    });

    it('should set attributes to dropdown', () => {
        fixture.detectChanges();
        const pDropDown: Dropdown = de.query(By.css('p-dropdown')).componentInstance;
        expect(pDropDown.filter).toBeDefined();
        expect(pDropDown.filterBy).toBeDefined();
        expect(pDropDown.showClear).toBeDefined();
        expect(pDropDown.resetFilterOnHide).toBeDefined();
        expect(pDropDown.style).toEqual({ width: '215px' });
    });
});

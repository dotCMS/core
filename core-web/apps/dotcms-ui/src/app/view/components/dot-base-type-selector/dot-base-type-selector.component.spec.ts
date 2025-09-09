import { of as observableOf } from 'rxjs';

import { DebugElement, Injectable } from '@angular/core';
import { ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { SelectItem } from 'primeng/api';
import { Dropdown } from 'primeng/dropdown';

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
            declarations: [DotBaseTypeSelectorComponent],
            imports: [BrowserAnimationsModule],
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
        const pDropDown: DebugElement = de.query(By.css('p-dropdown'));
        jest.spyOn(component.selected, 'emit');
        jest.spyOn(component, 'change');
        pDropDown.triggerEventHandler('onChange', allContentTypesItem);

        expect(component.change).toHaveBeenCalledWith(allContentTypesItem);
        expect(component.selected.emit).toHaveBeenCalledWith(allContentTypesItem.value);
    });

    it('should add All Content Types option as first position', () => {
        fixture.detectChanges();

        component.options.subscribe((options) => {
            expect(options[0]).toEqual(allContentTypesItem);
        });
    });

    it('shoudl set fixed width to dropdown', () => {
        fixture.detectChanges();
        const pDropDown: Dropdown = de.query(By.css('p-dropdown')).componentInstance;
        expect(pDropDown.style).toEqual({ width: '155px' });
    });
});

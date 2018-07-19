import { ComponentFixture } from '@angular/core/testing';
import { DotBaseTypeSelectorComponent } from './dot-base-type-selector.component';
import { DebugElement, Injectable } from '@angular/core';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotMessageService } from '../../../api/services/dot-messages-service';
import { Dropdown, SelectItem } from 'primeng/primeng';
import { By } from '@angular/platform-browser';
import { DotContentletService } from '../../../api/services/dot-contentlet/dot-contentlet.service';
import { Observable } from 'rxjs/Observable';

@Injectable()
class MockDotContentletService {
    getAllContentTypes = jasmine
        .createSpy('getContentTypes')
        .and.returnValue(Observable.of([{ name: 'FORM', label: 'Form' }, { name: 'WIDGET', label: 'Widget' }]));
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
                    provide: DotContentletService,
                    useClass: MockDotContentletService
                }
            ]
        });

        fixture = DOTTestBed.createComponent(DotBaseTypeSelectorComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
    });

    it('should emit the selected content type', () => {
        const pDropDown: DebugElement = de.query(By.css('p-dropdown'));
        spyOn(component.selected, 'emit');
        spyOn(component, 'change').and.callThrough();
        pDropDown.triggerEventHandler('onChange', allContentTypesItem);

        expect(component.change).toHaveBeenCalledWith(allContentTypesItem);
        expect(component.selected.emit).toHaveBeenCalledWith(allContentTypesItem.value);
    });

    it('should add All Content Types option as first position', () => {
        fixture.detectChanges();

        component.options.subscribe(options => {
            expect(options[0]).toEqual(allContentTypesItem);
        });
    });

    it('shoudl set fixed width to dropdown', () => {
        fixture.detectChanges();
        const pDropDown: Dropdown = de.query(By.css('p-dropdown')).componentInstance;
        expect(pDropDown.style).toEqual({ width: '155px' });
    });
});

import { ComponentFixture, async } from '@angular/core/testing';
import {
    DotPersonaSelectorComponent,
    defaultVisitorPersona
} from './dot-persona-selector.component';
import { DebugElement, Component, Input } from '@angular/core';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotMessageService } from '@services/dot-messages-service';
import { By } from '@angular/platform-browser';
import { DotPersona } from '@models/dot-persona/dot-persona.model';
import { mockDotPersona } from '@tests/dot-persona.mock';
import { DotPersonaSelectedItemModule } from '@components/dot-persona-selected-item/dot-persona-selected-item.module';
import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { DotPersonaSelectorOptionModule } from '@components/dot-persona-selector-option/dot-persona-selector-option.module';
import { mockDotPage } from '@tests/dot-rendered-page.mock';
import { of } from 'rxjs';
import { PaginatorService } from '@services/paginator';

@Component({
    selector: 'dot-host-component',
    template: `
        <dot-persona-selector
            (selected)="changePersonaHandler($event)"
            [value]="persona"
            [pageId]="pageId"
        ></dot-persona-selector>
    `
})
class HostTestComponent {
    @Input() pageId: string;
    @Input() persona: DotPersona;
    changePersonaHandler(_$event) {}
}

class TestPaginatorService {
    filter: string;
    url: string;
    paginationPerPage: string;
    totalRecords = [mockDotPersona].length;

    getWithOffset(_offset: number) {
        return of([mockDotPersona]);
    }
}

describe('DotPersonaSelectorComponent', () => {
    let component: DotPersonaSelectorComponent;
    let hostFixture: ComponentFixture<HostTestComponent>;
    let de: DebugElement;
    let paginatorService: PaginatorService;
    let dropdown: DebugElement;
    const defaultPersona: DotPersona = mockDotPersona;
    const messageServiceMock = new MockDotMessageService({
        'modes.persona.no.persona': 'Default Visitor',
        'modes.persona.personalized': 'Personalized'
    });

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotPersonaSelectorComponent, HostTestComponent],
            imports: [
                BrowserAnimationsModule,
                SearchableDropDownModule,
                DotPersonaSelectedItemModule,
                DotPersonaSelectorOptionModule
            ],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                { provide: PaginatorService, useClass: TestPaginatorService }
            ]
        });
    }));

    beforeEach(() => {
        hostFixture = DOTTestBed.createComponent(HostTestComponent);
        de = hostFixture.debugElement.query(By.css('dot-persona-selector'));
        component = de.componentInstance;
        paginatorService = hostFixture.debugElement.injector.get(PaginatorService);

        hostFixture.componentInstance.pageId = mockDotPage.identifier;

        hostFixture.detectChanges();
        dropdown = de.query(By.css('dot-searchable-dropdown'));
    });

    it('should emit the selected persona', () => {
        spyOn(component.selected, 'emit');
        dropdown.triggerEventHandler('change', defaultPersona);
        expect(component.selected.emit).toHaveBeenCalledWith(defaultPersona);
    });

    it('should call filter change with keyword', () => {
        spyOn(paginatorService, 'filter');
        dropdown.triggerEventHandler('filterChange', 'test');
        expect(paginatorService.filter).toBe('test');
    });

    it('should call page change', () => {
        spyOn(paginatorService, 'getWithOffset').and.returnValue(of([mockDotPersona]));
        dropdown.triggerEventHandler('pageChange', { filter: '', first: 5, rows: 5 });
        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(5);
    });

    it('should set dot-searchable-dropdown with right attributes', () => {
        expect(dropdown.componentInstance.labelPropertyName).toBe('name');
        expect(dropdown.componentInstance.cssClass).toBe('dot-persona-selector');
        expect(dropdown.componentInstance.optionsWidth).toBe(448);
        expect(dropdown.componentInstance.data).toEqual([mockDotPersona]);
        expect(dropdown.componentInstance.rows).toBe(5);
        expect(dropdown.componentInstance.totalRecords).toBe(1);
    });

    it('should set "No Persona" (Default Visitor) value when no persona defined', () => {
        expect(de.componentInstance.value).toEqual(defaultVisitorPersona);
    });

    it('should keep persona value when set by parent container', () => {
        hostFixture.componentInstance.persona = mockDotPersona;
        hostFixture.detectChanges();
        expect(de.componentInstance.value).toEqual(mockDotPersona);
    });

    it('should render dot-persona-selected-item template with persona data', () => {
        hostFixture.whenStable().then(() => {
            hostFixture.detectChanges();
            const selectedItem = hostFixture.debugElement.query(By.css('dot-persona-selected-item'));
            expect(selectedItem.componentInstance.persona).toEqual(defaultVisitorPersona);
        });
    });

    it('should call toggle when selected dot-persona-selected-item', () => {
        spyOn(dropdown.componentInstance, 'toggleOverlayPanel');
        hostFixture.whenStable().then(() => {
            hostFixture.detectChanges();
            const selectedItem = hostFixture.debugElement.query(By.css('dot-persona-selected-item'));
            selectedItem.triggerEventHandler('selected', {});
            expect(dropdown.componentInstance.toggleOverlayPanel).toHaveBeenCalled();
        });
    });

    it('should dot-persona-selector-option template with right params', () => {
        hostFixture.componentInstance.persona = { ...mockDotPersona };
        hostFixture.detectChanges();
        hostFixture.whenStable().then(() => {
            const personaSelector = hostFixture.debugElement.query(By.css('dot-persona-selected-item'));
            personaSelector.nativeElement.click();
            hostFixture.detectChanges();
            const mockPersonaData = { ...mockDotPersona, label: 'Global Investor' };
            const personaOption = hostFixture.debugElement.query(By.css('dot-persona-selector-option'))
            expect(personaOption.componentInstance.selected).toBe(true);
            expect(personaOption.componentInstance.persona).toEqual(mockPersonaData);
        });
    });

    it('should execute "change" event from dot-persona-selector-option', () => {
        hostFixture.componentInstance.persona = { ...mockDotPersona };
        hostFixture.detectChanges();
        hostFixture.whenStable().then(() => {
            const personaSelector = hostFixture.debugElement.query(By.css('dot-persona-selected-item'));
            personaSelector.nativeElement.click();
            spyOn(component.selected, 'emit');
            spyOn(dropdown.componentInstance, 'toggleOverlayPanel');
            hostFixture.detectChanges();
            const personaOption = hostFixture.debugElement.query(By.css('dot-persona-selector-option'));
            personaOption.triggerEventHandler('change', defaultPersona);
            expect(component.selected.emit).toHaveBeenCalledWith(defaultPersona);
            expect(dropdown.componentInstance.toggleOverlayPanel).toHaveBeenCalled();
        });
    });

    it('should execute "delete" event from dot-persona-selector-option', () => {
        hostFixture.componentInstance.persona = { ...mockDotPersona };
        hostFixture.detectChanges();
        hostFixture.whenStable().then(() => {
            const personaSelector = hostFixture.debugElement.query(By.css('dot-persona-selected-item'));
            personaSelector.nativeElement.click();
            spyOn(dropdown.componentInstance, 'toggleOverlayPanel');
            hostFixture.detectChanges();
            const personaOption = hostFixture.debugElement.query(By.css('dot-persona-selector-option'));
            personaOption.triggerEventHandler('delete', defaultPersona);
            expect(dropdown.componentInstance.toggleOverlayPanel).toHaveBeenCalled();
        });
    });
});

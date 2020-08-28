import { ComponentFixture, async } from '@angular/core/testing';
import { DotPersonaSelectorComponent } from './dot-persona-selector.component';
import { DebugElement, Component, Input } from '@angular/core';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { By } from '@angular/platform-browser';
import { DotPersona } from '@models/dot-persona/dot-persona.model';
import { mockDotPersona } from '@tests/dot-persona.mock';
import { DotPersonaSelectedItemModule } from '@components/dot-persona-selected-item/dot-persona-selected-item.module';
import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { DotPersonaSelectorOptionModule } from '@components/dot-persona-selector-option/dot-persona-selector-option.module';
import { of } from 'rxjs';
import { PaginatorService } from '@services/paginator';
import { DotAddPersonaDialogModule } from '@components/dot-add-persona-dialog/dot-add-persona-dialog.module';
import { IframeOverlayService } from '@components/_common/iframe/service/iframe-overlay.service';
import { LoginServiceMock } from '@tests/login-service.mock';
import { LoginService, SiteService } from 'dotcms-js';
import { DotAddPersonaDialogComponent } from '@components/dot-add-persona-dialog/dot-add-persona-dialog.component';
import { SiteServiceMock } from '@tests/site-service.mock';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { TooltipModule } from 'primeng/primeng';

@Component({
    selector: 'dot-host-component',
    template: `
        <dot-persona-selector
            [disabled]="disabled"
            (selected)="selectedPersonaHandler($event)"
            (delete)="deletePersonaHandler($event)"
        ></dot-persona-selector>
    `
})
class HostTestComponent {
    @Input() disabled: boolean;
    selectedPersonaHandler(_$event) {}
    deletePersonaHandler(_$event) {}
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

    const openOverlay = () => {
        const personaSelector: DotPersonaSelectorComponent = hostFixture.debugElement.query(
            By.css('dot-persona-selector')
        ).componentInstance;
        personaSelector.disabled = false;
        const personaSelectedItem = hostFixture.debugElement.query(
            By.css('dot-persona-selected-item')
        );
        personaSelectedItem.nativeElement.dispatchEvent(new MouseEvent('click'));
        hostFixture.detectChanges();
    };

    const siteServiceMock = new SiteServiceMock();

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotPersonaSelectorComponent, HostTestComponent],
            imports: [
                BrowserAnimationsModule,
                SearchableDropDownModule,
                DotPersonaSelectedItemModule,
                DotPersonaSelectorOptionModule,
                DotAddPersonaDialogModule,
                TooltipModule,
                DotPipesModule
            ],
            providers: [
                IframeOverlayService,
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                { provide: PaginatorService, useClass: TestPaginatorService },
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: SiteService, useValue: siteServiceMock }
            ]
        });
    }));

    beforeEach(() => {
        hostFixture = DOTTestBed.createComponent(HostTestComponent);
        de = hostFixture.debugElement.query(By.css('dot-persona-selector'));
        component = de.componentInstance;
        paginatorService = hostFixture.debugElement.injector.get(PaginatorService);
        hostFixture.detectChanges();
        dropdown = de.query(By.css('dot-searchable-dropdown'));
    });

    it('should emit the selected persona', () => {
        spyOn(component.selected, 'emit');
        dropdown.triggerEventHandler('change', defaultPersona);
        expect(component.selected.emit).toHaveBeenCalledWith(defaultPersona);
    });

    it('should call filter change with keyword', () => {
        dropdown.triggerEventHandler('filterChange', ' test ');
        expect(paginatorService.filter).toBe('test');
    });

    it('should call page change', () => {
        spyOn(paginatorService, 'getWithOffset').and.returnValue(of([{ ...mockDotPersona }]));
        dropdown.triggerEventHandler('pageChange', { filter: '', first: 10, rows: 10 });
        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(10);
    });

    it('should set dot-searchable-dropdown with right attributes', () => {
        expect(dropdown.componentInstance.labelPropertyName).toBe('name');
        expect(dropdown.componentInstance.optionsWidth).toBe(448);
        expect(dropdown.componentInstance.rows).toBe(10);
        expect(dropdown.componentInstance.totalRecords).toBe(1);
    });

    it('should set dot-persona-selected-item with right attributes', () => {
        const personaSelectedItemDe = de.query(By.css('dot-persona-selected-item'));
        expect(personaSelectedItemDe.attributes.appendTo).toBe('target');
        expect(personaSelectedItemDe.attributes['ng-reflect-text']).toBe('Default Visitor');
        expect(personaSelectedItemDe.attributes['ng-reflect-tooltip-position']).toBe('bottom');
    });

    it('should call toggle when selected dot-persona-selected-item', () => {
        hostFixture.whenStable().then(() => {
            spyOn(dropdown.componentInstance, 'toggleOverlayPanel');
            const selectedItem = hostFixture.debugElement.query(
                By.css('dot-persona-selected-item')
            );
            selectedItem.triggerEventHandler('selected', {});
            expect(dropdown.componentInstance.toggleOverlayPanel).toHaveBeenCalled();
        });
    });

    it('should dot-persona-selector-option template with right params', () => {
        hostFixture.whenStable().then(() => {
            openOverlay();
            const mockPersonaData = { ...mockDotPersona, label: 'Global Investor' };
            const personaOption = hostFixture.debugElement.query(
                By.css('dot-persona-selector-option')
            );
            expect(personaOption.componentInstance.selected).toBe(true);
            expect(personaOption.componentInstance.persona).toEqual(mockPersonaData);
        });
    });

    it('should execute "change" event from dot-persona-selector-option', () => {
        hostFixture.whenStable().then(() => {
            spyOn(component.selected, 'emit');
            openOverlay();
            const personaOption = hostFixture.debugElement.query(
                By.css('dot-persona-selector-option')
            );
            personaOption.triggerEventHandler('change', defaultPersona);
            expect(component.selected.emit).toHaveBeenCalledWith(defaultPersona);
        });
    });

    it('should execute "delete" event from dot-persona-selector-option', () => {
        hostFixture.whenStable().then(() => {
            spyOn(component.delete, 'emit');
            openOverlay();
            const personaOption = hostFixture.debugElement.query(
                By.css('dot-persona-selector-option')
            );
            personaOption.triggerEventHandler('delete', defaultPersona);
            expect(component.delete.emit).toHaveBeenCalledWith(defaultPersona);
        });
    });

    describe('Add Persona Dialog', () => {
        let personaDialog: DotAddPersonaDialogComponent;

        beforeEach(() => {
            personaDialog = de.query(By.css('dot-add-persona-dialog')).componentInstance;
        });

        it('should toggle Overlay Panel, pass the search as name if present and open add form', () => {
            openOverlay();
            const addPersonaIcon = dropdown.query(By.css('dot-icon-button'));

            spyOn(dropdown.componentInstance, 'toggleOverlayPanel');

            dropdown.triggerEventHandler('filterChange', 'Bill');
            addPersonaIcon.nativeElement.click();
            hostFixture.detectChanges();
            expect(dropdown.componentInstance.toggleOverlayPanel).toHaveBeenCalled();
            expect(personaDialog.visible).toBe(true);
            expect(personaDialog.personaName).toBe('Bill');
            personaDialog.visible = false;
            hostFixture.detectChanges();
        });

        it('should emit persona and refresh the list on Add new persona', () => {
            spyOn(component.selected, 'emit');
            spyOn(paginatorService, 'getWithOffset').and.returnValue(of([mockDotPersona]));
            spyOn(dropdown.componentInstance, 'resetPanelMinHeight');

            personaDialog.createdPersona.emit(defaultPersona);

            expect(component.selected.emit).toHaveBeenCalledWith(defaultPersona);
            expect(paginatorService.filter).toEqual('');
            expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
            expect(dropdown.componentInstance.resetPanelMinHeight).toHaveBeenCalled();
        });
    });

    describe('Ifram Overlay Service', () => {
        let iframeOverlayService: IframeOverlayService;

        beforeEach(() => {
            iframeOverlayService = hostFixture.debugElement.injector.get(IframeOverlayService);
        });

        it('should call hide event on hide persona list', () => {
            spyOn(iframeOverlayService, 'hide');
            dropdown.triggerEventHandler('hide', {});

            expect(iframeOverlayService.hide).toHaveBeenCalled();
        });

        it('should call show event on show persona list', () => {
            spyOn(iframeOverlayService, 'show');
            dropdown.triggerEventHandler('show', {});

            expect(iframeOverlayService.show).toHaveBeenCalled();
        });
    });
});

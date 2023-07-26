/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { TooltipModule } from 'primeng/tooltip';

import { IframeOverlayService } from '@components/_common/iframe/service/iframe-overlay.service';
import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { DotAddPersonaDialogComponent } from '@components/dot-add-persona-dialog/dot-add-persona-dialog.component';
import { DotAddPersonaDialogModule } from '@components/dot-add-persona-dialog/dot-add-persona-dialog.module';
import { DotMessageDisplayServiceMock } from '@components/dot-message-display/dot-message-display.component.spec';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotPersonaSelectedItemModule } from '@components/dot-persona-selected-item/dot-persona-selected-item.module';
import { DotPersonaSelectorOptionModule } from '@components/dot-persona-selector-option/dot-persona-selector-option.module';
import { DOTTestBed } from '@dotcms/app/test/dot-test-bed';
import { DotMessageService, PaginatorService } from '@dotcms/data-access';
import { LoginService, SiteService } from '@dotcms/dotcms-js';
import { DotPersona } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import {
    cleanUpDialog,
    LoginServiceMock,
    MockDotMessageService,
    mockDotPersona,
    SiteServiceMock
} from '@dotcms/utils-testing';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotPersonaSelectorComponent } from './dot-persona-selector.component';

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

    beforeEach(waitForAsync(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotPersonaSelectorComponent, HostTestComponent],
            imports: [
                BrowserAnimationsModule,
                SearchableDropDownModule,
                DotPersonaSelectedItemModule,
                DotPersonaSelectorOptionModule,
                DotAddPersonaDialogModule,
                TooltipModule,
                DotPipesModule,
                DotMessagePipe
            ],
            providers: [
                IframeOverlayService,
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                { provide: PaginatorService, useClass: TestPaginatorService },
                { provide: DotMessageDisplayService, useClass: DotMessageDisplayServiceMock },
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
        dropdown.triggerEventHandler('switch', defaultPersona);
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
        expect(dropdown.componentInstance.width).toBe('448px');
        expect(dropdown.componentInstance.overlayWidth).toBe('300px');
        expect(dropdown.componentInstance.rows).toBe(10);
        expect(dropdown.componentInstance.totalRecords).toBe(1);
    });

    it('should set dot-persona-selected-item with right attributes', () => {
        const personaSelectedItemDe = de.query(By.css('dot-persona-selected-item'));
        expect(personaSelectedItemDe.attributes.appendTo).toBe('target');
        expect(personaSelectedItemDe.attributes['ng-reflect-text']).toBe('Default Visitor');
        expect(personaSelectedItemDe.attributes['ng-reflect-tooltip-position']).toBe('bottom');
    });

    it('should call toggle when selected dot-persona-selected-item', async () => {
        spyOn(dropdown.componentInstance, 'toggleOverlayPanel');
        await hostFixture.whenStable();

        const selectedItem = hostFixture.debugElement.query(By.css('dot-persona-selected-item'));
        selectedItem.triggerEventHandler('click', {});
        expect(dropdown.componentInstance.toggleOverlayPanel).toHaveBeenCalled();
    });

    it('should have highlighted persona option once the dropdown in loaded', async () => {
        await hostFixture.whenStable();

        openOverlay();
        const personaOption = hostFixture.debugElement.query(By.css('dot-persona-selector-option'));
        expect(personaOption.classes['highlight']).toEqual(true);
    });

    // TODO: this test fails ramdomly when all tests are ran, a fix needs to be done
    it('should dot-persona-selector-option template with right params', async () => {
        await hostFixture.whenStable();

        openOverlay();
        const mockPersonaData = { ...mockDotPersona, label: 'Global Investor' };
        const personaOption = hostFixture.debugElement.query(By.css('dot-persona-selector-option'));
        expect(personaOption.componentInstance.persona).toEqual(mockPersonaData);
    });

    it('should execute "change" event from dot-persona-selector-option', async () => {
        await hostFixture.whenStable();

        spyOn(component.selected, 'emit');
        openOverlay();
        const personaOption = hostFixture.debugElement.query(By.css('dot-persona-selector-option'));
        personaOption.triggerEventHandler('switch', defaultPersona);
        expect(component.selected.emit).toHaveBeenCalledWith(defaultPersona);
    });

    xit('should execute "delete" event from dot-persona-selector-option', async () => {
        await hostFixture.whenStable();

        spyOn(component.delete, 'emit');
        openOverlay();
        const personaOption = hostFixture.debugElement.query(By.css('dot-persona-selector-option'));
        personaOption.triggerEventHandler('delete', defaultPersona);
        expect<any>(component.delete.emit).toHaveBeenCalledWith({
            ...defaultPersona,
            label: 'Global Investor'
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

    describe('Iframe Overlay Service', () => {
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
            dropdown.triggerEventHandler('display', {});

            expect(iframeOverlayService.show).toHaveBeenCalled();
        });
    });

    afterEach(() => {
        cleanUpDialog(hostFixture);
    });
});

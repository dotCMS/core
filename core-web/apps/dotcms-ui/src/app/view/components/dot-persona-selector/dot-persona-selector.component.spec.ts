/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, Input } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';

import { ConfirmationService } from 'primeng/api';

import {
    DotAlertConfirmService,
    DotEventsService,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotRouterService,
    DotSessionStorageService,
    DotSystemConfigService,
    DotWorkflowActionsFireService,
    PaginatorService
} from '@dotcms/data-access';
import { CoreWebService, LoginService, SiteService } from '@dotcms/dotcms-js';
import { DotPersona, DotSystemConfig } from '@dotcms/dotcms-models';
import {
    cleanUpDialog,
    CoreWebServiceMock,
    DotMessageDisplayServiceMock,
    LoginServiceMock,
    MockDotMessageService,
    mockDotPersona,
    MockDotRouterService,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { DotPersonaSelectorComponent } from './dot-persona-selector.component';

import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { DotAddPersonaDialogComponent } from '../dot-add-persona-dialog/dot-add-persona-dialog.component';

@Component({
    selector: 'dot-host-component',
    template: `
        <dot-persona-selector
            (selected)="selectedPersonaHandler($event)"
            (delete)="deletePersonaHandler($event)"
            [disabled]="disabled"></dot-persona-selector>
    `,
    standalone: false
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
    let spectator: Spectator<HostTestComponent>;
    let component: DotPersonaSelectorComponent;
    let paginatorService: PaginatorService;
    const defaultPersona: DotPersona = mockDotPersona;
    const messageServiceMock = new MockDotMessageService({
        'modes.persona.no.persona': 'Default Visitor',
        'modes.persona.personalized': 'Personalized'
    });

    const siteServiceMock = new SiteServiceMock();

    const mockSystemConfig: DotSystemConfig = {
        logos: {
            loginScreen: '',
            navBar: ''
        },
        colors: {
            primary: '#54428e',
            secondary: '#3a3847',
            background: '#BB30E1'
        },
        releaseInfo: {
            buildDate: 'June 24, 2019',
            version: '5.0.0'
        },
        systemTimezone: {
            id: 'America/Costa_Rica',
            label: 'Costa Rica',
            offset: 360
        },
        languages: [],
        license: {
            level: 100,
            displayServerId: '19fc0e44',
            levelName: 'COMMUNITY EDITION',
            isCommunity: true
        },
        cluster: {
            clusterId: 'test-cluster',
            companyKeyDigest: 'test-digest'
        }
    };

    class MockDotSystemConfigService {
        getSystemConfig() {
            return of(mockSystemConfig);
        }
    }

    const createComponent = createComponentFactory({
        component: HostTestComponent,
        imports: [DotPersonaSelectorComponent],
        componentProviders: [
            { provide: PaginatorService, useClass: TestPaginatorService },
            { provide: IframeOverlayService, useClass: IframeOverlayService }
        ],
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            provideAnimations(),
            DotSessionStorageService,
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            {
                provide: DotMessageDisplayService,
                useClass: DotMessageDisplayServiceMock
            },
            { provide: LoginService, useClass: LoginServiceMock },
            { provide: SiteService, useValue: siteServiceMock },
            { provide: CoreWebService, useClass: CoreWebServiceMock },
            { provide: DotRouterService, useClass: MockDotRouterService },
            { provide: DotSystemConfigService, useClass: MockDotSystemConfigService },
            DotHttpErrorManagerService,
            DotWorkflowActionsFireService,
            ConfirmationService,
            DotAlertConfirmService,
            DotEventsService
        ],
        detectChanges: false
    });

    const openOverlay = () => {
        component.disabled = false;
        const personaSelectedItem = spectator.query('dot-persona-selected-item');
        personaSelectedItem.dispatchEvent(new MouseEvent('click'));
        spectator.detectChanges();
    };

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.query(DotPersonaSelectorComponent);
        paginatorService = component.paginationService;
        spectator.detectChanges();
    });

    it('should emit the selected persona', () => {
        jest.spyOn(component.selected, 'emit');
        spectator.triggerEventHandler('dot-searchable-dropdown', 'switch', defaultPersona);
        expect(component.selected.emit).toHaveBeenCalledWith(defaultPersona);
        expect(component.selected.emit).toHaveBeenCalledTimes(1);
    });

    it('should call filter change with keyword', () => {
        spectator.triggerEventHandler('dot-searchable-dropdown', 'filterChange', ' test ');
        expect(paginatorService.filter).toBe('test');
    });

    it('should call page change', () => {
        jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(of([{ ...mockDotPersona }]));
        spectator.triggerEventHandler('dot-searchable-dropdown', 'pageChange', {
            filter: '',
            first: 10,
            rows: 10
        });
        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(10);
        expect(paginatorService.getWithOffset).toHaveBeenCalledTimes(1);
    });

    it('should set dot-searchable-dropdown with right attributes', () => {
        // Initialize totalRecords
        component.personas = [mockDotPersona];
        component.totalRecords = component.personas.length;
        spectator.detectChanges();

        expect(component.searchableDropdown.labelPropertyName).toBe('name');
        expect(component.searchableDropdown.width).toBe('448px');
        expect(component.searchableDropdown.overlayWidth).toBe('300px');
        expect(component.searchableDropdown.rows).toBe(10);
        expect(component.totalRecords).toBe(1);
    });

    it('should set dot-persona-selected-item with right attributes', () => {
        const personaSelectedItem = spectator.query('dot-persona-selected-item');
        expect(personaSelectedItem.getAttribute('appendTo')).toBe('target');
        // In Angular 20, ng-reflect-* attributes are not available
        // Verify tooltip position attribute (passed as input to the component)
        expect(personaSelectedItem.getAttribute('tooltipPosition')).toBe('bottom');
        // Verify the displayed content (persona name or 'Default Visitor')
        const nameSpan = spectator.query('dot-persona-selected-item .dot-persona-selector__name');
        expect(nameSpan?.textContent?.trim()).toBe('Default Visitor');
    });

    it('should call toggle when selected dot-persona-selected-item', async () => {
        jest.spyOn(component.searchableDropdown, 'toggleOverlayPanel');
        await spectator.fixture.whenStable();

        const selectedItem = spectator.query('dot-persona-selected-item');
        spectator.click(selectedItem);
        expect(component.searchableDropdown.toggleOverlayPanel).toHaveBeenCalled();
    });

    it('should have highlighted persona option once the dropdown in loaded', async () => {
        // Setup personas data
        component.personas = [mockDotPersona];
        component.totalRecords = 1;
        spectator.detectChanges();
        await spectator.fixture.whenStable();

        openOverlay();
        await spectator.fixture.whenStable();
        spectator.detectChanges();

        const personaOption = spectator.query('dot-persona-selector-option');
        expect(personaOption).toBeTruthy();
        expect(personaOption.classList.contains('highlight')).toEqual(true);
    });

    // TODO: this test fails ramdomly when all tests are ran, a fix needs to be done
    it('should dot-persona-selector-option template with right params', async () => {
        // Setup personas data
        component.personas = [mockDotPersona];
        component.totalRecords = 1;
        spectator.detectChanges();
        await spectator.fixture.whenStable();

        openOverlay();
        await spectator.fixture.whenStable();
        spectator.detectChanges();

        const mockPersonaData = { ...mockDotPersona, label: 'Global Investor' };
        const personaOption = spectator.query('dot-persona-selector-option');
        expect(personaOption).toBeTruthy();
        // Access component instance through debugElement
        const personaComponent = spectator.debugElement.query(
            (el) => el.name === 'dot-persona-selector-option'
        );
        if (personaComponent) {
            expect(personaComponent.componentInstance.persona).toEqual(mockPersonaData);
        }
    });

    it('should execute "change" event from dot-persona-selector-option', async () => {
        // Setup personas data
        component.personas = [mockDotPersona];
        component.totalRecords = 1;
        spectator.detectChanges();
        await spectator.fixture.whenStable();

        jest.spyOn(component.selected, 'emit');
        openOverlay();
        await spectator.fixture.whenStable();
        spectator.detectChanges();

        const personaOptionDebugElement = spectator.debugElement.query(
            (el) => el.name === 'dot-persona-selector-option'
        );
        expect(personaOptionDebugElement).toBeTruthy();
        personaOptionDebugElement.triggerEventHandler('switch', defaultPersona);
        expect(component.selected.emit).toHaveBeenCalledWith(defaultPersona);
        expect(component.selected.emit).toHaveBeenCalledTimes(1);
    });

    xit('should execute "delete" event from dot-persona-selector-option', async () => {
        await spectator.fixture.whenStable();

        jest.spyOn(component.delete, 'emit');
        openOverlay();
        spectator.triggerEventHandler('dot-persona-selector-option', 'delete', defaultPersona);
        expect<any>(component.delete.emit).toHaveBeenCalledWith({
            ...defaultPersona,
            label: 'Global Investor'
        });
    });

    describe('Add Persona Dialog', () => {
        let personaDialog: DotAddPersonaDialogComponent;

        beforeEach(() => {
            personaDialog = component.personaDialog;
        });

        it('should toggle Overlay Panel, pass the search as name if present and open add form', () => {
            openOverlay();
            const addPersonaIcon = spectator.query('p-button');

            jest.spyOn(component.searchableDropdown, 'toggleOverlayPanel');

            spectator.triggerEventHandler('dot-searchable-dropdown', 'filterChange', 'Bill');
            spectator.click(addPersonaIcon);
            spectator.detectChanges();
            expect(component.searchableDropdown.toggleOverlayPanel).toHaveBeenCalled();
            expect(personaDialog.visible).toBe(true);
            expect(personaDialog.personaName).toBe('Bill');
            personaDialog.visible = false;
            spectator.detectChanges();
        });

        it('should emit persona and refresh the list on Add new persona', () => {
            jest.spyOn(component.selected, 'emit');
            jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(of([mockDotPersona]));
            jest.spyOn(component.searchableDropdown, 'resetPanelMinHeight');

            spectator.triggerEventHandler(
                'dot-add-persona-dialog',
                'createdPersona',
                defaultPersona
            );

            expect(component.selected.emit).toHaveBeenCalledWith(defaultPersona);
            expect(component.selected.emit).toHaveBeenCalledTimes(1);
            expect(paginatorService.filter).toEqual('');
            expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
            expect(paginatorService.getWithOffset).toHaveBeenCalledTimes(1);
            expect(component.searchableDropdown.resetPanelMinHeight).toHaveBeenCalled();
        });
    });

    describe('Iframe Overlay Service', () => {
        let iframeOverlayService: IframeOverlayService;

        beforeEach(() => {
            iframeOverlayService = component.iframeOverlayService;
        });

        it('should call hide event on hide persona list', () => {
            jest.spyOn(iframeOverlayService, 'hide');
            spectator.triggerEventHandler('dot-searchable-dropdown', 'hide', {});

            expect(iframeOverlayService.hide).toHaveBeenCalled();
        });

        it('should call show event on show persona list', () => {
            jest.spyOn(iframeOverlayService, 'show');
            spectator.triggerEventHandler('dot-searchable-dropdown', 'display', {});

            expect(iframeOverlayService.show).toHaveBeenCalled();
        });
    });

    afterEach(() => {
        cleanUpDialog(spectator.fixture);
    });
});

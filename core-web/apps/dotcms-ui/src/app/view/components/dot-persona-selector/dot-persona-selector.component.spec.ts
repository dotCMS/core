import { createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
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
import { LoginService, SiteService } from '@dotcms/dotcms-js';
import { DotPersona, DotSystemConfig } from '@dotcms/dotcms-models';
import {
    cleanUpDialog,
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
    let spectator: SpectatorHost<DotPersonaSelectorComponent>;
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

    const createHost = createHostFactory({
        component: DotPersonaSelectorComponent,
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
        spectator.component.disabled = false;
        const personaSelectedItem = spectator.query('dot-persona-selected-item');
        personaSelectedItem.dispatchEvent(new MouseEvent('click'));
        spectator.detectChanges();
    };

    beforeEach(() => {
        spectator = createHost(
            `<dot-persona-selector [disabled]="disabled"></dot-persona-selector>`,
            {
                hostProps: {
                    disabled: false
                }
            }
        );
        paginatorService = spectator.component.paginationService;
        spectator.detectChanges();
    });

    it('should emit the selected persona', () => {
        jest.spyOn(spectator.component.selected, 'emit');
        spectator.triggerEventHandler('dot-searchable-dropdown', 'switch', defaultPersona);
        expect(spectator.component.selected.emit).toHaveBeenCalledWith(defaultPersona);
        expect(spectator.component.selected.emit).toHaveBeenCalledTimes(1);
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
        spectator.component.personas = [mockDotPersona];
        spectator.component.totalRecords = spectator.component.personas.length;
        spectator.detectChanges();

        expect(spectator.component.searchableDropdown.labelPropertyName).toBe('name');
        expect(spectator.component.searchableDropdown.width).toBe('448px');
        expect(spectator.component.searchableDropdown.overlayWidth).toBe('300px');
        expect(spectator.component.searchableDropdown.rows).toBe(10);
        expect(spectator.component.totalRecords).toBe(1);
    });

    it('should set dot-persona-selected-item with right attributes', () => {
        const personaSelectedItem = spectator.query('dot-persona-selected-item');
        expect(personaSelectedItem.getAttribute('appendTo')).toBe('target');
        expect(personaSelectedItem.getAttribute('tooltipPosition')).toBe('bottom');
        const nameSpan = spectator.query('dot-persona-selected-item .dot-persona-selector__name');
        expect(nameSpan?.textContent?.trim()).toBe('Default Visitor');
    });

    it('should call toggle when selected dot-persona-selected-item', async () => {
        jest.spyOn(spectator.component.searchableDropdown, 'toggleOverlayPanel');
        await spectator.fixture.whenStable();

        const selectedItem = spectator.query('dot-persona-selected-item');
        spectator.click(selectedItem);
        expect(spectator.component.searchableDropdown.toggleOverlayPanel).toHaveBeenCalled();
    });

    it('should have highlighted persona option once the dropdown in loaded', async () => {
        spectator.component.personas = [mockDotPersona];
        spectator.component.totalRecords = 1;
        spectator.detectChanges();
        await spectator.fixture.whenStable();

        openOverlay();
        await spectator.fixture.whenStable();
        spectator.detectChanges();

        const personaOption = spectator.query('dot-persona-selector-option');
        expect(personaOption).toBeTruthy();
        expect(personaOption.classList.contains('highlight')).toEqual(true);
    });

    it('should dot-persona-selector-option template with right params', async () => {
        spectator.component.personas = [mockDotPersona];
        spectator.component.totalRecords = 1;
        spectator.detectChanges();
        await spectator.fixture.whenStable();

        openOverlay();
        await spectator.fixture.whenStable();
        spectator.detectChanges();

        const mockPersonaData = { ...mockDotPersona, label: 'Global Investor' };
        const personaOption = spectator.query('dot-persona-selector-option');
        expect(personaOption).toBeTruthy();
        const personaComponent = spectator.debugElement.query(
            (el) => el.name === 'dot-persona-selector-option'
        );
        if (personaComponent) {
            expect(personaComponent.componentInstance.persona).toEqual(mockPersonaData);
        }
    });

    it('should execute "change" event from dot-persona-selector-option', async () => {
        spectator.component.personas = [mockDotPersona];
        spectator.component.totalRecords = 1;
        spectator.detectChanges();
        await spectator.fixture.whenStable();

        jest.spyOn(spectator.component.selected, 'emit');
        openOverlay();
        await spectator.fixture.whenStable();
        spectator.detectChanges();

        const personaOptionDebugElement = spectator.debugElement.query(
            (el) => el.name === 'dot-persona-selector-option'
        );
        expect(personaOptionDebugElement).toBeTruthy();
        personaOptionDebugElement.triggerEventHandler('switch', defaultPersona);
        expect(spectator.component.selected.emit).toHaveBeenCalledWith(defaultPersona);
        expect(spectator.component.selected.emit).toHaveBeenCalledTimes(1);
    });

    it('should execute "delete" event from dot-persona-selector-option', async () => {
        await spectator.fixture.whenStable();

        jest.spyOn(spectator.component.delete, 'emit');
        openOverlay();
        spectator.triggerEventHandler('dot-persona-selector-option', 'delete', defaultPersona);
        expect(spectator.component.delete.emit).toHaveBeenCalledWith({
            ...defaultPersona,
            label: 'Global Investor'
        });
    });

    describe('Add Persona Dialog', () => {
        let personaDialog: DotAddPersonaDialogComponent;

        beforeEach(() => {
            personaDialog = spectator.component.personaDialog;
        });

        it('should toggle Overlay Panel, pass the search as name if present and open add form', () => {
            openOverlay();
            const addPersonaIcon = spectator.query('p-button');

            jest.spyOn(spectator.component.searchableDropdown, 'toggleOverlayPanel');

            spectator.triggerEventHandler('dot-searchable-dropdown', 'filterChange', 'Bill');
            spectator.click(addPersonaIcon);
            spectator.detectChanges();
            expect(spectator.component.searchableDropdown.toggleOverlayPanel).toHaveBeenCalled();
            expect(personaDialog.visible).toBe(true);
            expect(personaDialog.personaName).toBe('Bill');
            personaDialog.visible = false;
            spectator.detectChanges();
        });

        it('should emit persona and refresh the list on Add new persona', () => {
            jest.spyOn(spectator.component.selected, 'emit');
            jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(of([mockDotPersona]));
            jest.spyOn(spectator.component.searchableDropdown, 'resetPanelMinHeight');

            spectator.triggerEventHandler(
                'dot-add-persona-dialog',
                'createdPersona',
                defaultPersona
            );

            expect(spectator.component.selected.emit).toHaveBeenCalledWith(defaultPersona);
            expect(spectator.component.selected.emit).toHaveBeenCalledTimes(1);
            expect(paginatorService.filter).toEqual('');
            expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
            expect(paginatorService.getWithOffset).toHaveBeenCalledTimes(1);
            expect(spectator.component.searchableDropdown.resetPanelMinHeight).toHaveBeenCalled();
        });
    });

    describe('Iframe Overlay Service', () => {
        let iframeOverlayService: IframeOverlayService;

        beforeEach(() => {
            iframeOverlayService = spectator.component.iframeOverlayService;
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

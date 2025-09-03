/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ConfirmationService } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import {
    DotAlertConfirmService,
    DotEventsService,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotRouterService,
    DotSessionStorageService,
    DotSystemConfigService,
    PaginatorService
} from '@dotcms/data-access';
import { CoreWebService, LoginService, SiteService } from '@dotcms/dotcms-js';
import { DotPersona, DotSystemConfig } from '@dotcms/dotcms-models';
import { DotAvatarDirective, DotMessagePipe } from '@dotcms/ui';
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
import { SearchableDropDownModule } from '../_common/searchable-dropdown/searchable-dropdown.module';
import { DotAddPersonaDialogComponent } from '../dot-add-persona-dialog/dot-add-persona-dialog.component';
import { DotAddPersonaDialogModule } from '../dot-add-persona-dialog/dot-add-persona-dialog.module';
import { DotPersonaSelectedItemModule } from '../dot-persona-selected-item/dot-persona-selected-item.module';
import { DotPersonaSelectorOptionModule } from '../dot-persona-selector-option/dot-persona-selector-option.module';

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

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [DotPersonaSelectorComponent, HostTestComponent],
            imports: [
                BrowserAnimationsModule,
                SearchableDropDownModule,
                DotPersonaSelectedItemModule,
                DotPersonaSelectorOptionModule,
                DotAddPersonaDialogModule,
                DotMessagePipe,
                HttpClientTestingModule,
                DotAvatarDirective,
                AvatarModule,
                BadgeModule,
                ButtonModule,
                TooltipModule
            ],
            providers: [
                DotSessionStorageService,
                IframeOverlayService,
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                { provide: PaginatorService, useClass: TestPaginatorService },
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
                ConfirmationService,
                DotAlertConfirmService,
                DotEventsService
            ]
        });
    }));

    beforeEach(() => {
        hostFixture = TestBed.createComponent(HostTestComponent);
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
        dropdown.triggerEventHandler('pageChange', {
            filter: '',
            first: 10,
            rows: 10
        });
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
        expect(personaSelectedItemDe.attributes['ng-reflect-content']).toBe('Default Visitor');
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
            const addPersonaIcon = dropdown.query(By.css('p-button'));

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

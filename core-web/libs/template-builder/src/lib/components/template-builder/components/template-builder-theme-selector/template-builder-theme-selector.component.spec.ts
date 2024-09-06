import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { fakeAsync } from '@angular/core/testing';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DataViewModule } from 'primeng/dataview';
import { DropdownModule } from 'primeng/dropdown';
import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';

import { DotEventsService, DotThemesService, PaginatorService } from '@dotcms/data-access';
import { CoreWebService, mockSites, SiteService } from '@dotcms/dotcms-js';
import { DotMessagePipe, DotSiteSelectorDirective } from '@dotcms/ui';
import { CoreWebServiceMock, mockDotThemes, SiteServiceMock } from '@dotcms/utils-testing';

import { TemplateBuilderThemeSelectorComponent } from './template-builder-theme-selector.component';

describe('TemplateBuilderThemeSelectorComponent', () => {
    let spectator: Spectator<TemplateBuilderThemeSelectorComponent>;
    let paginatorService: PaginatorService;

    const siteServiceMock = new SiteServiceMock();
    const createComponent = createComponentFactory({
        component: TemplateBuilderThemeSelectorComponent,
        imports: [
            CommonModule,
            ButtonModule,
            DropdownModule,
            InputTextModule,
            DataViewModule,
            DotMessagePipe,
            DotSiteSelectorDirective,
            HttpClientTestingModule
        ],
        providers: [
            DialogService,
            DotEventsService,
            DynamicDialogRef,
            MessageService,
            PaginatorService,
            DotThemesService,
            {
                provide: DynamicDialogConfig,
                useValue: {
                    data: {
                        themeId: mockDotThemes[0].inode
                    }
                }
            },
            {
                provide: CoreWebService,
                useClass: CoreWebServiceMock
            },
            { provide: SiteService, useValue: siteServiceMock }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        paginatorService = spectator.inject(PaginatorService, true);
    });

    it('should create', () => {
        spectator.detectChanges();
        expect(spectator.component).toBeTruthy();
    });

    describe('On Init', () => {
        it('should set url, the page size and hostid for the pagination service', () => {
            paginatorService.searchParam = 'test';
            jest.spyOn(paginatorService, 'setExtraParams');
            jest.spyOn(paginatorService, 'deleteExtraParams');

            spectator.component.ngOnInit();
            spectator.detectChanges();

            expect(paginatorService.paginationPerPage).toBe(8);
            expect(paginatorService.url).toBe('v1/themes');
            expect(paginatorService.setExtraParams).toHaveBeenCalledWith(
                'hostId',
                mockSites[0].identifier
            );
            expect(paginatorService.deleteExtraParams).toHaveBeenCalled();
        });

        it('should set the current theme id variable based on dialog config', () => {
            const value = mockDotThemes[0].inode;

            spectator.component.ngOnInit();
            spectator.detectChanges();
            expect(spectator.component.themeId).toBe(value);
        });

        it('should call pagination service with offset of 0 when dataview onLazyLoad emit', () => {
            jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(of(mockDotThemes));
            spectator.component.ngOnInit();

            spectator.detectChanges();
            spectator.component.dataView.onLazyLoad.emit({
                first: 0,
                rows: 0,
                sortField: '',
                sortOrder: 0
            });

            expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        });

        it('should show theme image when available', () => {
            const systemTheme = {
                name: 'system Theme',
                title: 'Theme tittle',
                inode: '1',
                themeThumbnail: '/system/theme/url',
                identifier: 'SYSTEM_THEME',
                hostId: '1',
                host: {
                    hostName: 'Test',
                    inode: '3',
                    identifier: '345'
                }
            };

            jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(
                of([...mockDotThemes, systemTheme])
            );

            spectator.component.siteChange(mockSites[0]);
            spectator.detectChanges();

            const themeImages = spectator.queryAll(
                '[data-testId="themeImage"]'
            ) as HTMLImageElement[];

            expect(themeImages[0].src).toContain(
                `/dA/${mockDotThemes[2].themeThumbnail}/130w/130h/thumbnail.png`
            );
            expect(themeImages[1].src).toContain(systemTheme.themeThumbnail);
        });
    });

    describe('User interaction', () => {
        beforeEach(() => {
            jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(of(mockDotThemes));
        });

        it('should set pagination, call endpoint and clear search field on site change event', () => {
            jest.spyOn(paginatorService, 'setExtraParams');
            const site = mockSites[0];
            spectator.component.siteChange(site);
            spectator.detectChanges();

            expect(paginatorService.setExtraParams).toHaveBeenCalledWith('hostId', site.identifier);
            expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
            expect(spectator.component.searchInput.nativeElement.value).toBe('');
        });

        it('should set the current value when the user click a specific theme item', () => {
            jest.spyOn(spectator.component, 'selectTheme');
            spectator.component.paginate({ first: 0 });

            spectator.component.ngOnInit();
            spectator.detectChanges();

            const themeItem = spectator.queryAll('[data-testId="theme-item"]') as HTMLElement[];
            themeItem[0].click();

            expect(spectator.component.currentTheme).toBe(mockDotThemes[0]);
            expect(spectator.component.selectTheme).toHaveBeenCalled();
        });

        it('should call theme enpoint on search', fakeAsync(() => {
            spectator.component.paginate({ first: 0 });
            spectator.component.ngOnInit();
            spectator.detectChanges();

            const searchInput = spectator.component.searchInput.nativeElement;
            searchInput.dispatchEvent(new Event('keyup'));
            searchInput.dispatchEvent(new Event('input'));
            spectator.detectChanges();

            spectator.tick(500);
            expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        }));
    });
});

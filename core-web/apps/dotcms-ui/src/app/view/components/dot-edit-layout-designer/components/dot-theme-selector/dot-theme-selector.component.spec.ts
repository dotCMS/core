import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DataViewModule } from 'primeng/dataview';

import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import {
    DotEventsService,
    DotMessageService,
    DotThemesService,
    PaginatorService
} from '@dotcms/data-access';
import { CoreWebService, Site, SiteService } from '@dotcms/dotcms-js';
import { DotIconModule, DotMessagePipe } from '@dotcms/ui';
import {
    CoreWebServiceMock,
    DotThemesServiceMock,
    MockDotMessageService,
    mockDotThemes,
    mockSites,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { DotThemeSelectorComponent } from './dot-theme-selector.component';

@Component({
    selector: 'dot-site-selector',
    template: `<select>
        <option>Fake site selector</option>
    </select>`
})
class MockDotSiteSelectorComponent {
    @Input() system;
    @Input() archive;
    searchableDropdown = {
        handleClick: () => {
            //
        }
    };
}

describe('DotThemeSelectorComponent', () => {
    let component: DotThemeSelectorComponent;
    let fixture: ComponentFixture<DotThemeSelectorComponent>;
    let de: DebugElement;
    const messageServiceMock = new MockDotMessageService({
        'editpage.layout.theme.header': 'Header',
        'editpage.layout.theme.search': 'Search',
        'dot.common.apply': 'Apply',
        'dot.common.cancel': 'Cancel'
    });
    const siteServiceMock = new SiteServiceMock();
    let dialog;
    let paginatorService: PaginatorService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotThemeSelectorComponent, MockDotSiteSelectorComponent],
            imports: [
                DataViewModule,
                BrowserAnimationsModule,
                DotDialogModule,
                DotIconModule,
                DotMessagePipe,
                HttpClientTestingModule
            ],
            providers: [
                {
                    provide: DotThemesService,
                    useClass: DotThemesServiceMock
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                { provide: SiteService, useValue: siteServiceMock },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                PaginatorService,
                DotEventsService
            ]
        });

        fixture = TestBed.createComponent(DotThemeSelectorComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        dialog = de.query(By.css('dot-dialog')).componentInstance;
        component.value = { ...mockDotThemes[0] };
        paginatorService = de.injector.get(PaginatorService);
    });

    afterEach(() => {
        component.visible = false;
        fixture.detectChanges();
    });

    describe('Dialog', () => {
        beforeEach(() => {
            fixture.detectChanges();
        });

        describe('header', () => {
            it('should have site-selector', () => {
                const siteSelector = de.query(
                    By.css('[data-testId="header"] [data-testId="siteSelector"]')
                );
                expect(siteSelector).not.toBeNull();
                expect(siteSelector.componentInstance.system).toEqual(true);
                expect(siteSelector.componentInstance.archive).toEqual(false);
            });

            it('should have dot-icon', () => {
                const icon = de.query(
                    By.css(
                        '[data-testId="header"] .dot-theme-search-box [data-testId="searchIcon"]'
                    )
                );
                expect(icon.attributes.name).toBe('search');
            });

            it('should have input', () => {
                const input = de.query(
                    By.css('[data-testId="header"]  [data-testId="searchInput"]')
                );
                expect(input.attributes.pInputText).toBeDefined();
                expect(input.attributes.placeholder).toBe('Search');
            });
        });

        it('should be visible on init', () => {
            expect(dialog.visible).toBeTruthy();
        });

        it('should have set dialog actions', () => {
            expect(component.dialogActions).toEqual({
                accept: {
                    label: 'Apply',
                    disabled: true,
                    action: jasmine.any(Function)
                },
                cancel: {
                    label: 'Cancel'
                }
            });
        });
    });

    describe('On Init', () => {
        it('should set url, the page size and hostid for the pagination service', () => {
            paginatorService.searchParam = 'test';
            spyOn(paginatorService, 'setExtraParams');
            spyOn(paginatorService, 'deleteExtraParams');
            fixture.detectChanges();
            expect(paginatorService.paginationPerPage).toBe(8);
            expect(paginatorService.url).toBe('v1/themes');
            expect(paginatorService.setExtraParams).toHaveBeenCalledWith(
                'hostId',
                mockDotThemes[0].hostId
            );
            expect(paginatorService.deleteExtraParams).toHaveBeenCalledWith('searchParam');
        });

        it('should set the current theme variable based on the Input value', () => {
            const value = { ...mockDotThemes[0] };
            component.value = value;
            fixture.detectChanges();
            expect(component.current).toBe(value);
        });

        it('should call pagination service with offset of 0 ', () => {
            spyOn(component.cd, 'detectChanges').and.callThrough();
            spyOn(paginatorService, 'getWithOffset').and.returnValue(of([...mockDotThemes]));
            fixture.detectChanges();

            expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
            expect(component.cd.detectChanges).toHaveBeenCalledTimes(1);
        });

        it('should disable the apply button', () => {
            fixture.detectChanges();
            expect(component.dialogActions.accept.disabled).toBe(true);
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

            spyOn(paginatorService, 'getWithOffset').and.returnValue(
                of([...mockDotThemes, systemTheme])
            );
            component.siteChange(mockSites[0]);
            fixture.detectChanges();
            const themeImages = de.queryAll(By.css('[data-testId="themeImage"]'));
            expect(themeImages[0].nativeElement.src).toContain(
                `/dA/${mockDotThemes[2].themeThumbnail}/130w/130h/thumbnail.png`
            );
            expect(themeImages[1].nativeElement.src).toContain(systemTheme.themeThumbnail);
        });
    });

    describe('User interaction', () => {
        beforeEach(() => {
            spyOn(paginatorService, 'getWithOffset').and.returnValue(of(mockDotThemes));
        });

        it('should set pagination, call endpoint and clear search field on site change ', () => {
            spyOn(component, 'paginate');
            spyOn(paginatorService, 'setExtraParams');
            component.siteChange(mockSites[0]);
            fixture.detectChanges();

            expect(component.searchInput.nativeElement.value).toBe('');
            expect(paginatorService.setExtraParams).toHaveBeenCalledWith(
                'hostId',
                mockSites[0].identifier
            );
            expect(paginatorService.setExtraParams).toHaveBeenCalledWith('searchParam', '');
            expect(component.paginate).toHaveBeenCalledWith({ first: 0 });
        });

        it('should set the current value when the user click a specific theme', () => {
            spyOn(component, 'selectTheme').and.callThrough();
            component.paginate({ first: 0 });
            fixture.detectChanges();
            const themes: DebugElement[] = fixture.debugElement.queryAll(By.css('.dot-theme-item'));
            themes[1].nativeElement.click();

            expect(component.current).toBe(mockDotThemes[1]);
            expect(component.selectTheme).toHaveBeenCalled();
        });

        it('should active the apply button and set active when user select a different theme than the one in value', () => {
            component.paginate({ first: 0 });
            fixture.detectChanges();
            const themes: DebugElement[] = fixture.debugElement.queryAll(By.css('.dot-theme-item'));
            themes[1].nativeElement.click();
            fixture.detectChanges();

            expect(component.dialogActions.accept.disabled).toBe(false);
        });

        it('should call theme enpoint on search', fakeAsync(() => {
            spyOn(component, 'paginate');
            fixture.detectChanges();
            component.searchInput.nativeElement.value = 'test';
            component.searchInput.nativeElement.dispatchEvent(new Event('keyup'));
            tick(550);

            expect(paginatorService.extraParams.get('searchParam')).toBe('test');
            expect(component.paginate).toHaveBeenCalled();
        }));
    });

    describe('User interaction empty', () => {
        let siteService: SiteService;

        beforeEach(() => {
            siteService = TestBed.inject(SiteService);
            spyOn(paginatorService, 'getWithOffset').and.returnValue(of([]));
        });

        it(' should set system host ', () => {
            spyOn(siteService, 'getSiteById').and.returnValue(of({} as Site));
            fixture.detectChanges();
            expect(siteService.getSiteById).toHaveBeenCalledOnceWith('SYSTEM_HOST');
        });

        it(' should set system host just once ', () => {
            spyOn(siteService, 'getSiteById').and.returnValue(of({} as Site));
            fixture.detectChanges();
            setTimeout(() => component.siteChange({ identifier: '123' } as Site), 0); // simulate user site change.
            expect(siteService.getSiteById).toHaveBeenCalledOnceWith('SYSTEM_HOST');
        });
    });
});

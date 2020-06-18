import { of as observableOf } from 'rxjs';
import { ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DotThemeSelectorComponent } from './dot-theme-selector.component';
import { DebugElement } from '@angular/core';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotThemesService } from '@services/dot-themes/dot-themes.service';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { By } from '@angular/platform-browser';
import { mockDotThemes } from '../../../../../test/dot-themes.mock';
import { DataGridModule } from 'primeng/primeng';
import { DotSiteSelectorModule } from '@components/_common/dot-site-selector/dot-site-selector.module';
import { mockSites, SiteServiceMock } from '../../../../../test/site-service.mock';
import { SiteService } from 'dotcms-js';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { PaginatorService } from '@services/paginator/paginator.service';
import { DotThemesServiceMock } from '../../../../../test/dot-themes-service.mock';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';

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
        DOTTestBed.configureTestingModule({
            declarations: [DotThemeSelectorComponent],
            imports: [
                DataGridModule,
                DotSiteSelectorModule,
                BrowserAnimationsModule,
                DotDialogModule,
                DotIconModule
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
                PaginatorService
            ]
        });

        fixture = DOTTestBed.createComponent(DotThemeSelectorComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        dialog = de.query(By.css('dot-dialog')).componentInstance;
        component.value = { ...mockDotThemes[0] };
        paginatorService = de.injector.get(PaginatorService);
    });

    describe('Dialog', () => {
        beforeEach(() => {
            fixture.detectChanges();
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
            spyOn(paginatorService, 'setExtraParams');
            fixture.detectChanges();
            expect(paginatorService.paginationPerPage).toBe(8);
            expect(paginatorService.url).toBe('v1/themes');
            expect(paginatorService.setExtraParams).toHaveBeenCalledWith(
                'hostId',
                '123-xyz-567-xxl'
            );
        });

        it('should set the current theme variable based on the Input value', () => {
            const value = Object.assign({}, mockDotThemes[0]);
            component.value = value;
            fixture.detectChanges();
            expect(component.current).toBe(value);
        });

        it('should call pagination service with offset of 0 ', () => {
            spyOn(component, 'paginate').and.callThrough();
            spyOn(paginatorService, 'getWithOffset');
            fixture.detectChanges();

            expect(component.paginate).toHaveBeenCalledTimes(1);
            expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        });

        it('should disable the apply button', () => {
            fixture.detectChanges();
            expect(component.dialogActions.accept.disabled).toBe(true);
        });

        it('should show theme image when available', () => {
            spyOn(paginatorService, 'getWithOffset').and.returnValue(observableOf(mockDotThemes));
            component.siteChange(mockSites[0]);
            fixture.detectChanges();
            const themeImage: DebugElement = fixture.debugElement.query(
                By.css('.dot-theme-iteme__image')
            );

            expect(themeImage).not.toBeNull();
        });
    });

    describe('User interaction', () => {
        beforeEach(() => {
            spyOn(paginatorService, 'getWithOffset').and.returnValue(observableOf(mockDotThemes));
        });

        it('should set pagination, call endpoint and clear search field on site change ', () => {
            spyOn(component, 'paginate');
            component.siteChange(mockSites[0]);
            fixture.detectChanges();

            expect(component.searchInput.nativeElement.value).toBe('');
            expect(paginatorService.extraParams.get('hostId')).toBe(mockSites[0].identifier);
            expect(paginatorService.extraParams.get('searchParam')).toBe('');
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
});

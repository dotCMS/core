import { ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DotThemeSelectorComponent } from './dot-theme-selector.component';
import { DebugElement } from '@angular/core';
import { DotMessageService } from '../../../../../api/services/dot-messages-service';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotThemesService } from '../../../../../api/services/dot-themes/dot-themes.service';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { Observable } from 'rxjs/Observable';
import { By } from '@angular/platform-browser';
import { mockDotThemes } from '../../../../../test/dot-themes.mock';
import { DataGridModule } from 'primeng/primeng';
import { SiteSelectorModule } from '../../../../../view/components/_common/site-selector/site-selector.module';
import { mockSites, SiteServiceMock } from '../../../../../test/site-service.mock';
import { SiteService } from 'dotcms-js/dotcms-js';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { PaginatorService } from '../../../../../api/services/paginator/paginator.service';
import { DotThemesServiceMock } from '../../../../../test/dot-themes-service.mock';

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
    let dotThemesService: DotThemesService;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotThemeSelectorComponent],
            imports: [DataGridModule, SiteSelectorModule, BrowserAnimationsModule],
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
        dialog = de.query(By.css('p-dialog')).componentInstance;
        component.value = Object.assign({}, mockDotThemes[0]);
        paginatorService = de.injector.get(PaginatorService);
        dotThemesService = de.injector.get(DotThemesService);
    });

    describe('Dialog', () => {
        beforeEach(() => {
            fixture.detectChanges();
        });

        it('should be visible on init', () => {
            expect(dialog.visible).toBeTruthy();
        });

        it('should emit close event when click on cancel button', () => {
            const cancelBtn = de.query(By.css('.cancel'));
            spyOn(component.close, 'emit');
            cancelBtn.triggerEventHandler('click', {});
            expect(component.close.emit).toHaveBeenCalled();
        });

        it('should not be draggable, modal and have dismissable Mask', () => {
            expect(dialog.closable).toBe(true, 'closable');
            expect(dialog.draggable).toBe(false, 'draggable');
            expect(dialog.modal).toBe(true, 'modal');
        });

        it('should call the apply method and emit the selected value', () => {
            const applyBtn = de.query(By.css('.apply'));
            spyOn(component, 'apply').and.callThrough();
            spyOn(component.selected, 'emit');
            component.current = Object.assign({}, mockDotThemes[1]);
            fixture.detectChanges();
            applyBtn.triggerEventHandler('click', {});

            expect(component.selected.emit).toHaveBeenCalledWith(mockDotThemes[1]);
        });
    });

    describe('On Init', () => {
        beforeEach(() => {});

        it('should set url and the page size for the pagination service', () => {
            fixture.detectChanges();
            expect(paginatorService.paginationPerPage).toBe(8);
            expect(paginatorService.url).toBe('v1/themes');
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
            const applyButton: DebugElement = fixture.debugElement.query(By.css('.apply'));

            expect(applyButton.nativeElement.disabled).toBe(true);
        });

        it('should show theme image when available', () => {
            spyOn(paginatorService, 'getWithOffset').and.returnValue(Observable.of(mockDotThemes));
            component.siteChange(mockSites[0]);
            fixture.detectChanges();
            const themeImage: DebugElement = fixture.debugElement.query(By.css('.dot-theme-iteme__image'));

            expect(themeImage).not.toBeNull();
        });
    });

    describe('User interaction', () => {
        beforeEach(() => {
            spyOn(paginatorService, 'getWithOffset').and.returnValue(Observable.of(mockDotThemes));
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
            const applyButton: DebugElement = fixture.debugElement.query(By.css('.apply'));
            themes[1].nativeElement.click();
            fixture.detectChanges();

            expect(applyButton.nativeElement.disabled).toBe(false);
            expect(themes[1].nativeElement.classList.contains('active')).toBe(true);
        });

        it(
            'should call theme enpoint on search',
            fakeAsync(() => {
                spyOn(component, 'paginate');
                fixture.detectChanges();
                component.searchInput.nativeElement.value = 'test';
                component.searchInput.nativeElement.dispatchEvent(new Event('keyup'));
                tick(550);

                expect(paginatorService.extraParams.get('searchParam')).toBe('test');
                expect(component.paginate).toHaveBeenCalled();
            })
        );
    });
});

import { DebugElement } from '@angular/core/src/debug/debug_node';
import { By } from '@angular/platform-browser';
import { DotEditPageNavComponent } from './dot-edit-page-nav.component';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { DotLicenseService } from '../../../../api/services/dot-license/dot-license.service';
import { DotRenderedPageState } from '../../shared/models/dot-rendered-page-state.model';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { TooltipModule } from 'primeng/primeng';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { mockDotRenderedPage } from './../../../../test/dot-rendered-page.mock';
import { mockUser } from './../../../../test/login-service.mock';
import { Injectable, Component, Input } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { ActivatedRoute } from '@angular/router';

@Injectable()
class MockDotLicenseService {
    isEnterpriseLicense(): Observable<boolean> {
        return Observable.of(false);
    }
}

@Component({
    selector: 'dot-test-host-component',
    template: `<dot-edit-page-nav [pageState]="pageState"></dot-edit-page-nav>`
})
class TestHostComponent {
    @Input() pageState: DotRenderedPageState;
}

describe('DotEditPageNavComponent', () => {
    let dotLicenseService: DotLicenseService;
    let component: DotEditPageNavComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let testbed;

    const messageServiceMock = new MockDotMessageService({
        'editpage.toolbar.nav.content': 'Content',
        'editpage.toolbar.nav.layout': 'Layout',
        'editpage.toolbar.nav.code': 'Code',
        'editpage.toolbar.nav.license.enterprise.only': 'Enterprise only',
        'editpage.toolbar.nav.layout.advance.disabled': 'Can’t edit advanced template'
    });

    beforeEach(async(() => {
        testbed = DOTTestBed.configureTestingModule({
            imports: [RouterTestingModule, TooltipModule],
            declarations: [DotEditPageNavComponent, TestHostComponent],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotLicenseService, useClass: MockDotLicenseService },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        snapshot: {
                            firstChild: {
                                url: [
                                    {
                                        path: 'content'
                                    }
                                ]
                            }
                        }
                    }
                }
            ]
        });

        fixture = testbed.createComponent(TestHostComponent);
        de = fixture.debugElement;
        component = de.query(By.css('dot-edit-page-nav')).componentInstance;
        fixture.componentInstance.pageState = new DotRenderedPageState(mockUser, mockDotRenderedPage);
        fixture.detectChanges();
    }));

    describe('basic setup', () => {
        it('should have menu list', () => {
            const menuList = fixture.debugElement.query(By.css('.edit-page-nav'));
            expect(menuList).not.toBeNull();
        });

        it('should have correct item active', () => {
            const activeItem = fixture.debugElement.query(By.css('.edit-page-nav__item--active'));
            expect(activeItem.nativeElement.innerText).toContain('Content');
        });
    });

    describe('model change', () => {
        it('should have basic menu items', () => {
            const menuListItems = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));
            expect(menuListItems.length).toEqual(2);

            const labels = ['Content', 'Layout'];
            const icons = ['fa fa-file-text', 'fa fa-th-large'];
            menuListItems.forEach((item, index) => {
                const iconClass = item.query(By.css('i')).nativeElement.classList.value;
                expect(iconClass).toEqual(icons[index]);
                expect(item.nativeElement.textContent).toContain(labels[index]);
            });
        });

        it('should update menu items when new PageState', () => {
            dotLicenseService = de.injector.get(DotLicenseService);
            const menuListItems = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));
            const { layout, ...noLayoutPage } = mockDotRenderedPage;
            fixture.componentInstance.pageState = new DotRenderedPageState(mockUser, noLayoutPage, null);
            component.model = undefined;
            spyOn(dotLicenseService, 'isEnterpriseLicense').and.returnValue(Observable.of(true));
            fixture.detectChanges();
            const menuListItemsUpdated = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));
            expect(menuListItems[1].nativeElement.classList).toContain('edit-page-nav__item--disabled');
            expect(menuListItemsUpdated[1].nativeElement.classList).not.toContain('edit-page-nav__item--disabled');
        });
    });

    describe('advanced template', () => {
        const mockDotRenderedPageAdvanceTemplate = {
            ...mockDotRenderedPage,
            template: {
                ...mockDotRenderedPage.template,
                drawed: false
            },
            layout: null
        };

        beforeEach(() => {
            dotLicenseService = de.injector.get(DotLicenseService);
        });
        // Disable advance template commit https://github.com/dotCMS/core-web/pull/589
        it('should have menu items: Content and Layout', () => {
            fixture.componentInstance.pageState = new DotRenderedPageState(
                mockUser,
                mockDotRenderedPageAdvanceTemplate,
                null
            );

            fixture.detectChanges();
            const menuListItems: DebugElement[] = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));
            const iconClass = menuListItems[0].query(By.css('i')).nativeElement.classList.value;

            expect(menuListItems.length).toEqual(2);
            expect(iconClass).toEqual('fa fa-file-text');
            expect(menuListItems[0].nativeElement.textContent).toContain('Content');
            expect(menuListItems[1].nativeElement.textContent).toContain('Layout');
        });

        it('should have layout option disabled and cant edit message when template is advance and license is enterprise', () => {
            spyOn(dotLicenseService, 'isEnterpriseLicense').and.returnValue(Observable.of(true));

            component.model = undefined;
            fixture.componentInstance.pageState = new DotRenderedPageState(
                mockUser,
                mockDotRenderedPageAdvanceTemplate,
                null
            );
            fixture.detectChanges();

            const menuListItems = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));
            expect(menuListItems[1].nativeElement.classList).toContain('edit-page-nav__item--disabled');

            const layoutTooltipHTML = menuListItems[1].nativeElement.outerHTML;
            expect(menuListItems[1].nativeElement.getAttribute('ng-reflect-text')).toBe('Can’t edit advanced template');
        });

        it('should have layout option disabled and enterprise only message when template is advance and license is comunity', () => {
            fixture.componentInstance.pageState = new DotRenderedPageState(
                mockUser,
                mockDotRenderedPageAdvanceTemplate,
                null
            );
            fixture.detectChanges();

            const menuListItems = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));
            expect(menuListItems[1].nativeElement.classList).toContain('edit-page-nav__item--disabled');

            const layoutTooltipHTML = menuListItems[1].nativeElement.outerHTML;
            expect(menuListItems[1].nativeElement.getAttribute('ng-reflect-text')).toBe('Enterprise only');
        });

        it('should have code option disabled because user can not edit the page thus the layout or template', () => {
            fixture.componentInstance.pageState = new DotRenderedPageState(
                mockUser,
                {
                    ...mockDotRenderedPageAdvanceTemplate,
                    page: {
                        ...mockDotRenderedPageAdvanceTemplate.page,
                        canEdit: false
                    }
                },
                null
            );
            fixture.detectChanges();

            const menuListItems = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));
            expect(menuListItems[1].nativeElement.classList).toContain('edit-page-nav__item--disabled');

            const labels = ['Content', 'Layout'];
            const icons = ['fa fa-file-text', 'fa fa-th-large'];
            menuListItems.forEach((item, index) => {
                const iconClass = item.query(By.css('i')).nativeElement.classList.value;
                expect(iconClass).toEqual(icons[index]);
                expect(item.nativeElement.textContent).toContain(labels[index]);
            });
        });

        describe('license Community', () => {
            it('should have layout option disabled because user does not has a proper license', () => {
                const menuListItems = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));
                expect(menuListItems[1].nativeElement.classList).toContain('edit-page-nav__item--disabled');
            });

            it('should the layout option have the proper attribute & message key for tooltip', () => {
                const menuListItems = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));
                const layoutTooltipHTML = menuListItems[1].nativeElement.outerHTML;
                expect(layoutTooltipHTML).toContain(messageServiceMock.get('editpage.toolbar.nav.license.enterprise.only'));
            });
        });

        describe('license Enterprise', () => {
            beforeEach(() => {
                dotLicenseService = de.injector.get(DotLicenseService);
                spyOn(dotLicenseService, 'isEnterpriseLicense').and.returnValue(Observable.of(true));
                fixture.detectChanges();
            });

            it('should have layout option enabled because user has an enterprise license', () => {
                const menuListItems = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));
                expect(menuListItems[1].nativeElement.classList).toContain('edit-page-nav__item');
            });
        });
    });
});

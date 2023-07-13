import { Observable, of as observableOf } from 'rxjs';

import { Component, DebugElement, Injectable, Input } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { TooltipModule } from 'primeng/tooltip';

import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotLicenseService, DotMessageService, DotPropertiesService } from '@dotcms/data-access';
import { DotPageRender, DotPageRenderState } from '@dotcms/dotcms-models';
import { DotIconModule, DotMessagePipe } from '@dotcms/ui';
import {
    getExperimentMock,
    MockDotMessageService,
    mockDotRenderedPage,
    mockUser
} from '@dotcms/utils-testing';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotEditPageNavComponent } from './dot-edit-page-nav.component';

class ActivatedRouteMock {
    get snapshot() {
        return {
            firstChild: {
                url: [
                    {
                        path: 'content'
                    }
                ]
            },
            data: { featuredFlagExperiment: true },
            queryParams: { experimentId: EXPERIMENT_MOCK.id }
        };
    }
}

@Injectable()
class MockDotContentletEditorService {
    edit = jasmine.createSpy('edit');
}

@Injectable()
class MockDotLicenseService {
    isEnterprise(): Observable<boolean> {
        return observableOf(false);
    }
}

@Injectable()
export class MockDotPropertiesService {
    getKey(): Observable<true> {
        return observableOf(true);
    }
}

@Component({
    selector: 'dot-test-host-component',
    template: ` <dot-edit-page-nav [pageState]="pageState"></dot-edit-page-nav> `
})
class TestHostComponent {
    @Input()
    pageState: DotPageRenderState;
}

const EXPERIMENT_MOCK = getExperimentMock(1);

describe('DotEditPageNavComponent', () => {
    let dotLicenseService: DotLicenseService;
    let dotContentletEditorService: DotContentletEditorService;
    let component: DotEditPageNavComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let route: ActivatedRoute;

    const messageServiceMock = new MockDotMessageService({
        'editpage.toolbar.nav.content': 'Content',
        'editpage.toolbar.nav.rules': 'Rules',
        'editpage.toolbar.nav.layout': 'Layout',
        'editpage.toolbar.nav.properties': 'Properties',
        'editpage.toolbar.nav.code': 'Code',
        'editpage.toolbar.nav.license.enterprise.only': 'Enterprise only',
        'editpage.toolbar.nav.layout.advance.disabled': 'Can’t edit advanced template',
        'editpage.toolbar.nav.experiments': 'Experiments'
    });

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [
                RouterTestingModule,
                TooltipModule,
                DotIconModule,
                DotPipesModule,
                DotMessagePipe
            ],
            declarations: [DotEditPageNavComponent, TestHostComponent],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotLicenseService, useClass: MockDotLicenseService },
                { provide: DotPropertiesService, useClass: MockDotPropertiesService },
                {
                    provide: DotContentletEditorService,
                    useClass: MockDotContentletEditorService
                },
                {
                    provide: ActivatedRoute,
                    useClass: ActivatedRouteMock
                }
            ]
        });

        fixture = TestBed.createComponent(TestHostComponent);
        de = fixture.debugElement;
        component = de.query(By.css('dot-edit-page-nav')).componentInstance;
        fixture.componentInstance.pageState = new DotPageRenderState(
            mockUser(),
            new DotPageRender(mockDotRenderedPage())
        );
        dotContentletEditorService = TestBed.inject(DotContentletEditorService);
        route = TestBed.inject(ActivatedRoute);
    }));

    describe('basic setup', () => {
        it('should have menu list', () => {
            fixture.detectChanges();
            const menuList = fixture.debugElement.query(By.css('.edit-page-nav'));
            expect(menuList).not.toBeNull();
        });

        it('should have correct item active', () => {
            fixture.detectChanges();
            const activeItem = fixture.debugElement.query(By.css('.edit-page-nav__item--active'));
            expect(activeItem.nativeElement.innerText).toContain('CONTENT');
        });

        it('should call the ContentletEditorService Edit when clicked on Properties button', () => {
            fixture.detectChanges();
            const menuListItems = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));
            menuListItems[3].nativeNode.click();
            expect(dotContentletEditorService.edit).toHaveBeenCalled();
        });
    });

    describe('model change', () => {
        it('should have basic menu items', () => {
            const TOTAL_NAV_ITEMS_SHOWED = 4;
            fixture.detectChanges();
            const menuListItems = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));

            expect(menuListItems.length).toEqual(TOTAL_NAV_ITEMS_SHOWED);

            const labels = ['Content', 'Layout', 'Rules', 'Properties'];
            const icons = ['insert_drive_file', 'view_quilt', 'tune', 'more_horiz'];
            menuListItems.forEach((item, index) => {
                const iconClass = item.query(By.css('i')).nativeElement.innerHTML.trim();
                expect(iconClass).toEqual(icons[index]);
                expect(item.nativeElement.textContent).toContain(labels[index]);
            });
        });

        it('should update menu items when new PageState', () => {
            dotLicenseService = de.injector.get(DotLicenseService);
            fixture.detectChanges();
            const menuListItems = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            const { layout, ...noLayoutPage } = mockDotRenderedPage();
            fixture.componentInstance.pageState = new DotPageRenderState(
                mockUser(),
                new DotPageRender(noLayoutPage)
            );
            component.model = undefined;
            spyOn(dotLicenseService, 'isEnterprise').and.returnValue(observableOf(true));
            fixture.detectChanges();
            const menuListItemsUpdated = fixture.debugElement.queryAll(
                By.css('.edit-page-nav__item')
            );
            expect(menuListItems[1].nativeElement.classList).toContain(
                'edit-page-nav__item--disabled'
            );
            expect(menuListItemsUpdated[1].nativeElement.classList).not.toContain(
                'edit-page-nav__item--disabled'
            );
        });
    });

    describe('advanced template', () => {
        const mockDotRenderedPageAdvanceTemplate = {
            ...mockDotRenderedPage(),
            template: {
                ...mockDotRenderedPage().template,
                drawed: false
            },
            layout: null
        };

        beforeEach(() => {
            dotLicenseService = de.injector.get(DotLicenseService);
        });
        // Disable advance template commit https://github.com/dotCMS/core-web/pull/589
        it('should have menu items: Content and Layout', () => {
            const TOTAL_NAV_ITEMS_SHOWED = 4;
            fixture.componentInstance.pageState = new DotPageRenderState(
                mockUser(),
                new DotPageRender(mockDotRenderedPageAdvanceTemplate)
            );

            fixture.detectChanges();
            const menuListItems: DebugElement[] = fixture.debugElement.queryAll(
                By.css('.edit-page-nav__item')
            );
            const iconClass = menuListItems[0].query(By.css('i')).nativeElement.innerHTML.trim();

            expect(menuListItems.length).toEqual(TOTAL_NAV_ITEMS_SHOWED);
            expect(iconClass).toEqual('insert_drive_file');
            expect(menuListItems[0].nativeElement.textContent).toContain('Content');
            expect(menuListItems[1].nativeElement.textContent).toContain('Layout');
        });

        describe('disabled option', () => {
            it('should have layout option disabled and cant edit message when template is advance and license is enterprise', () => {
                spyOn(dotLicenseService, 'isEnterprise').and.returnValue(observableOf(true));

                component.model = undefined;
                fixture.componentInstance.pageState = new DotPageRenderState(
                    mockUser(),
                    new DotPageRender(mockDotRenderedPageAdvanceTemplate)
                );
                fixture.detectChanges();

                const menuListItems = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));
                expect(menuListItems[1].nativeElement.classList).toContain(
                    'edit-page-nav__item--disabled'
                );
                expect(menuListItems[1].nativeElement.getAttribute('ng-reflect-text')).toBe(
                    'Can’t edit advanced template'
                );
            });

            it('should have layout option disabled when is on a variant of a running experiment', () => {
                spyOn(dotLicenseService, 'isEnterprise').and.returnValue(observableOf(true));

                component.model = undefined;

                fixture.componentInstance.pageState = new DotPageRenderState(
                    mockUser(),
                    new DotPageRender(mockDotRenderedPage()),
                    null,
                    EXPERIMENT_MOCK
                );

                component.isVariantMode = true;

                fixture.detectChanges();

                const menuListItems = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));

                expect(menuListItems[1].nativeElement.classList).toContain(
                    'edit-page-nav__item--disabled'
                );
            });

            it('should have layout and rules option disabled and enterprise only message when template is advance and license is comunity', () => {
                fixture.componentInstance.pageState = new DotPageRenderState(
                    mockUser(),
                    new DotPageRender(mockDotRenderedPageAdvanceTemplate)
                );
                fixture.detectChanges();

                const menuListItems = fixture.debugElement.queryAll(
                    By.css('.edit-page-nav__item--disabled')
                );

                const labels = ['Layout', 'Rules', 'Experiments'];
                menuListItems.forEach((item, index) => {
                    const label = item.query(By.css('.edit-page-nav__item-text'));
                    expect(label.nativeElement.textContent).toBe(labels[index]);

                    expect(item.nativeElement.getAttribute('ng-reflect-text')).toBe(
                        'Enterprise only'
                    );
                });
            });

            it('should have code option disabled because user can not edit the page thus the layout or template', () => {
                fixture.componentInstance.pageState = new DotPageRenderState(
                    mockUser(),
                    new DotPageRender({
                        ...mockDotRenderedPageAdvanceTemplate,
                        page: {
                            ...mockDotRenderedPageAdvanceTemplate.page,
                            canEdit: false
                        }
                    })
                );
                fixture.detectChanges();

                const menuListItems = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));
                expect(menuListItems[1].nativeElement.classList).toContain(
                    'edit-page-nav__item--disabled'
                );

                const labels = ['Content', 'Layout', 'Rules', 'Properties', 'Experiments'];
                const icons = ['insert_drive_file', 'view_quilt', 'tune', 'more_horiz', 'dataset'];
                menuListItems.forEach((item, index) => {
                    const iconClass = item.query(By.css('i')).nativeElement.innerHTML.trim();
                    expect(iconClass).toEqual(icons[index]);
                    expect(item.nativeElement.textContent).toContain(labels[index]);
                });
            });
        });

        describe('license community', () => {
            it('should have layout option disabled because user does not has a proper license', () => {
                fixture.detectChanges();
                const menuListItems = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));
                expect(menuListItems[1].nativeElement.classList).toContain(
                    'edit-page-nav__item--disabled'
                );
            });

            it('should the layout option have the proper attribute & message key for tooltip', () => {
                fixture.detectChanges();
                const menuListItems = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));
                const layoutTooltipHTML = menuListItems[1].nativeElement.outerHTML;
                expect(layoutTooltipHTML).toContain(
                    messageServiceMock.get('editpage.toolbar.nav.license.enterprise.only')
                );
            });
        });

        describe('license enterprise', () => {
            beforeEach(() => {
                dotLicenseService = de.injector.get(DotLicenseService);
                spyOn(dotLicenseService, 'isEnterprise').and.returnValue(observableOf(true));
                fixture.detectChanges();
            });

            it('should have layout option enabled because user has an enterprise license', () => {
                const menuListItems = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));
                expect(menuListItems[1].nativeElement.classList).toContain('edit-page-nav__item');
            });
        });
    });

    describe('experiments feature flag true', () => {
        it('should has Experiments nav item', () => {
            const MATERIAL_ICON_NAME = 'science';
            // eslint-disable-next-line  @typescript-eslint/no-explicit-any
            spyOnProperty<any>(route, 'snapshot', 'get').and.returnValue({
                firstChild: {
                    url: [
                        {
                            path: 'content'
                        }
                    ]
                },
                data: { featuredFlag: true }
            });
            fixture.detectChanges();

            const menuListItems = fixture.debugElement.queryAll(
                By.css('[data-testId="menuListItems"]')
            );
            expect(menuListItems.length).toEqual(5);

            const iconClass = menuListItems[4].query(By.css('i')).nativeElement.innerHTML;
            const label = menuListItems[4].query(By.css('[data-testId="menuListItemText"]'))
                .nativeElement.innerHTML;
            expect(MATERIAL_ICON_NAME).toEqual(iconClass);
            expect('Experiments').toEqual(label);
        });
    });
    describe('experiments feature flag false', () => {
        it('should not has Experiments item', () => {
            // eslint-disable-next-line  @typescript-eslint/no-explicit-any
            spyOnProperty<any>(route, 'snapshot', 'get').and.returnValue({
                firstChild: {
                    url: [
                        {
                            path: 'content'
                        }
                    ]
                },
                data: { featuredFlag: false }
            });
            fixture.detectChanges();

            const menuListItems = de.queryAll(By.css('.edit-page-nav__item'));
            expect(menuListItems.length).toEqual(4);
        });
    });
});

import { of, throwError } from 'rxjs';

import { HttpResponse } from '@angular/common/http';
import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotEditLayoutService } from '@dotcms/app/api/services/dot-edit-layout/dot-edit-layout.service';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotTemplateContainersCacheService } from '@dotcms/app/api/services/dot-template-containers-cache/dot-template-containers-cache.service';
import { EMPTY_TEMPLATE_DESIGN } from '@dotcms/app/portlets/dot-templates/dot-template-create-edit/store/dot-template.store';
import { DotShowHideFeatureDirective } from '@dotcms/app/shared/directives/dot-show-hide-feature/dot-show-hide-feature.directive';
import {
    DotMessageService,
    DotPageLayoutService,
    DotPropertiesService,
    DotSessionStorageService
} from '@dotcms/data-access';
import { DotCMSResponse, HttpCode, ResponseView } from '@dotcms/dotcms-js';
import { DotLayout, DotPageRender, DotTemplateDesigner } from '@dotcms/dotcms-models';
import {
    MockDotMessageService,
    mockDotRenderedPage,
    mockResponseView,
    processedContainers
} from '@dotcms/utils-testing';

import { DotEditLayoutComponent } from './dot-edit-layout.component';

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'dotcms-template-builder',
    template: ''
})
export class MockTemplateBuilderComponent {
    @Input()
    layout: DotLayout;

    @Input()
    themeId: string;

    @Output()
    templateChange: EventEmitter<DotTemplateDesigner> = new EventEmitter();
}

@Component({
    selector: 'dot-edit-layout-designer',
    template: ''
})
export class MockDotEditLayoutDesignerComponent {
    @Input()
    layout: DotLayout;

    @Input()
    title: string;

    @Input()
    theme: string;

    @Input()
    apiLink: string;

    @Input()
    url: string;
}

const PAGE_STATE = new DotPageRender(mockDotRenderedPage());

let fixture: ComponentFixture<DotEditLayoutComponent>;

const messageServiceMock = new MockDotMessageService({
    'dot.common.message.saving': 'Saving',
    'dot.common.message.saved': 'Saved'
});

describe('DotEditLayoutComponent', () => {
    let component: DotEditLayoutComponent;
    let layoutDesignerDe: DebugElement;
    let layoutDesigner: MockDotEditLayoutDesignerComponent;
    let dotPageLayoutService: DotPageLayoutService;
    let dotGlobalMessageService: DotGlobalMessageService;
    let dotTemplateContainersCacheService: DotTemplateContainersCacheService;
    let fakeLayout: DotLayout;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let dotEditLayoutService: DotEditLayoutService;
    let dotSessionStorageService: DotSessionStorageService;
    let dotPropertiesService: DotPropertiesService;
    let router: Router;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [
                MockDotEditLayoutDesignerComponent,
                DotEditLayoutComponent,
                MockTemplateBuilderComponent
            ],
            imports: [DotShowHideFeatureDirective],
            providers: [
                RouterTestingModule,
                DotSessionStorageService,
                DotEditLayoutService,
                {
                    provide: DotHttpErrorManagerService,
                    useValue: {
                        handle: jasmine.createSpy().and.returnValue(of({}))
                    }
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: DotGlobalMessageService,
                    useValue: {
                        loading: jasmine.createSpy(),
                        success: jasmine.createSpy(),
                        error: jasmine.createSpy()
                    }
                },
                {
                    provide: DotPageLayoutService,
                    useValue: {
                        save() {
                            //
                        }
                    }
                },
                {
                    provide: DotTemplateContainersCacheService,
                    useValue: {
                        set: jasmine.createSpy()
                    }
                },
                {
                    provide: DotRouterService,
                    useValue: {
                        goToEditPage: jasmine.createSpy()
                    }
                },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        parent: {
                            parent: {
                                data: of({
                                    content: PAGE_STATE
                                })
                            }
                        }
                    }
                },
                {
                    provide: DotPropertiesService,
                    useValue: {
                        getKey: () => of('false')
                    }
                }
            ]
        });
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotEditLayoutComponent);
        component = fixture.componentInstance;

        dotPageLayoutService = TestBed.inject(DotPageLayoutService);
        dotGlobalMessageService = TestBed.inject(DotGlobalMessageService);
        dotTemplateContainersCacheService = TestBed.inject(DotTemplateContainersCacheService);
        dotHttpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);
        dotEditLayoutService = TestBed.inject(DotEditLayoutService);
        dotSessionStorageService = TestBed.inject(DotSessionStorageService);
        dotPropertiesService = TestBed.inject(DotPropertiesService);
        router = TestBed.inject(Router);
    });

    describe('with data', () => {
        beforeEach(() => {
            fixture.detectChanges();
            layoutDesignerDe = fixture.debugElement.query(By.css('dot-edit-layout-designer'));
            layoutDesigner = layoutDesignerDe.componentInstance;
            fakeLayout = {
                body: null,
                footer: false,
                header: true,
                sidebar: null,
                themeId: '123',
                title: 'Title',
                width: '100'
            };
        });

        it('should be 100% min-width in the host', () => {
            // https://github.com/dotCMS/core/issues/19540
            expect(fixture.debugElement.nativeElement.style.minWidth).toBe('100%');
        });

        it('should pass attr to the dot-edit-layout-designer', () => {
            fixture.detectChanges();
            const state = mockDotRenderedPage();
            expect(layoutDesigner.layout).toEqual(state.layout);
            expect(layoutDesigner.theme).toEqual(state.template.theme);
            expect(layoutDesigner.title).toEqual(state.page.title);
            expect(layoutDesigner.apiLink).toEqual('api/v1/page/render/an/url/test?language_id=1');
            expect(layoutDesigner.url).toEqual(state.page.pageURI);
        });

        describe('save', () => {
            beforeEach(() => {
                fixture.detectChanges();
            });

            it('should save the layout', () => {
                const res: DotPageRender = new DotPageRender(mockDotRenderedPage());
                spyOn(dotPageLayoutService, 'save').and.returnValue(of(res));

                layoutDesignerDe.triggerEventHandler('save', {
                    themeId: '123',
                    layout: fakeLayout,
                    title: null
                });

                expect(dotGlobalMessageService.loading).toHaveBeenCalledWith('Saving');
                expect(dotGlobalMessageService.success).toHaveBeenCalledWith('Saved');
                expect(dotGlobalMessageService.error).not.toHaveBeenCalled();

                expect(dotPageLayoutService.save).toHaveBeenCalledWith('123', {
                    themeId: '123',
                    layout: fakeLayout,
                    title: null
                });
                expect(dotTemplateContainersCacheService.set).toHaveBeenCalledWith({
                    '/default/': processedContainers[0].container,
                    '/banner/': processedContainers[1].container
                });
                expect(component.pageState).toEqual(new DotPageRender(mockDotRenderedPage()));
            });

            it('should save the layout after 10000', fakeAsync(() => {
                const res: DotPageRender = new DotPageRender(mockDotRenderedPage());
                spyOn(dotPageLayoutService, 'save').and.returnValue(of(res));

                layoutDesignerDe.triggerEventHandler('updateTemplate', {
                    themeId: '123',
                    layout: fakeLayout,
                    title: null
                });

                tick(10000);
                expect(dotGlobalMessageService.loading).toHaveBeenCalledWith('Saving');
                expect(dotGlobalMessageService.success).toHaveBeenCalledWith('Saved');
                expect(dotGlobalMessageService.error).not.toHaveBeenCalled();

                expect(dotPageLayoutService.save).toHaveBeenCalledWith('123', {
                    themeId: '123',
                    layout: fakeLayout,
                    title: null
                });
                expect(dotTemplateContainersCacheService.set).toHaveBeenCalledWith({
                    '/default/': processedContainers[0].container,
                    '/banner/': processedContainers[1].container
                });
                expect(component.pageState).toEqual(new DotPageRender(mockDotRenderedPage()));
            }));

            it('should save the layout instantly when closeEditLayout is true', () => {
                const res: DotPageRender = new DotPageRender(mockDotRenderedPage());
                spyOn(dotPageLayoutService, 'save').and.returnValue(of(res));

                layoutDesignerDe.triggerEventHandler('updateTemplate', {
                    themeId: '123',
                    layout: fakeLayout,
                    title: null
                });

                dotEditLayoutService.changeCloseEditLayoutState(true);

                expect(dotGlobalMessageService.loading).toHaveBeenCalledWith('Saving');
                expect(dotGlobalMessageService.success).toHaveBeenCalledWith('Saved');
                expect(dotGlobalMessageService.error).not.toHaveBeenCalled();

                expect(dotPageLayoutService.save).toHaveBeenCalledWith('123', {
                    themeId: '123',
                    layout: fakeLayout,
                    title: null
                });
                expect(dotTemplateContainersCacheService.set).toHaveBeenCalledWith({
                    '/default/': processedContainers[0].container,
                    '/banner/': processedContainers[1].container
                });
                expect(component.pageState).toEqual(new DotPageRender(mockDotRenderedPage()));
            });

            it('should not save the layout when observable is destroy', fakeAsync(() => {
                const res: DotPageRender = new DotPageRender(mockDotRenderedPage());
                spyOn(dotPageLayoutService, 'save').and.returnValue(of(res));

                // Destroy should be true.
                component.destroy$.subscribe((value) => expect(value).toBeTruthy());

                // Trigger the observable
                layoutDesignerDe.triggerEventHandler('updateTemplate', fakeLayout);

                // Destroy the observable
                component.destroy$.next(true);
                component.destroy$.complete();
                tick(10000);

                expect(dotPageLayoutService.save).not.toHaveBeenCalled();
            }));

            it('should handle error when save fail', (done) => {
                spyOn(dotPageLayoutService, 'save').and.returnValue(
                    throwError(
                        new ResponseView(
                            new HttpResponse<DotCMSResponse<unknown>>(
                                mockResponseView(HttpCode.BAD_REQUEST)
                            )
                        )
                    )
                );

                layoutDesignerDe.triggerEventHandler('save', fakeLayout);
                expect(dotGlobalMessageService.error).toHaveBeenCalledWith('Unknown Error');
                expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
                dotEditLayoutService.canBeDesactivated$.subscribe((resp) => {
                    expect(resp).toBeTruthy();
                    done();
                });
            });

            it('should remove variant key from session storage on destoy', () => {
                spyOn(dotSessionStorageService, 'removeVariantId');
                component.ngOnDestroy();
                expect(dotSessionStorageService.removeVariantId).toHaveBeenCalledTimes(1);
            });

            it('should keep variant key from session storage if going to Edit content portlet', () => {
                router.routerState.snapshot.url = '/edit-page/content';
                spyOn(dotSessionStorageService, 'removeVariantId');
                component.ngOnDestroy();
                expect(dotSessionStorageService.removeVariantId).toHaveBeenCalledTimes(0);
            });
        });
    });

    describe('New Template Builder', () => {
        beforeEach(() => {
            spyOn(dotPropertiesService, 'getKey').and.returnValue(of('true'));
            fixture.detectChanges();
        });

        it('should show new template builder component', () => {
            fixture.detectChanges();
            const component: DebugElement = fixture.debugElement.query(
                By.css('[data-testId="new-template-builder"]')
            );

            expect(component).toBeTruthy();
        });

        it('should set the themeId @Input correctly', () => {
            const templateBuilder = fixture.debugElement.query(
                By.css('[data-testId="new-template-builder"]')
            );
            expect(templateBuilder.componentInstance.themeId).toBe(PAGE_STATE.template.theme);
        });

        it('should emit events from new-template-builder when the layout is changed', () => {
            const builder = fixture.debugElement.query(
                By.css('[data-testId="new-template-builder"]')
            );
            const template = {
                layout: EMPTY_TEMPLATE_DESIGN.layout,
                themeId: '123'
            } as DotTemplateDesigner;

            spyOn(component.updateTemplate, 'next');

            builder.triggerEventHandler('templateChange', template);

            expect(component.updateTemplate.next).toHaveBeenCalled();
        });
    });
});

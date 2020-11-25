import { of as observableOf, of } from 'rxjs';
import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { Component, Input } from '@angular/core';
import { By } from '@angular/platform-browser';

import { DotEditLayoutComponent } from './dot-edit-layout.component';
import { mockDotRenderedPage } from '@tests/dot-page-render.mock';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotPageLayoutService } from '@services/dot-page-layout/dot-page-layout.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotTemplateContainersCacheService } from '@services/dot-template-containers-cache/dot-template-containers-cache.service';
import { DotLayout } from '@models/dot-edit-layout-designer';

@Component({
    selector: 'dot-edit-layout-designer',
    template: ''
})
export class DotEditLayoutDesignerComponentMock {
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

let fixture: ComponentFixture<DotEditLayoutComponent>;

const messageServiceMock = new MockDotMessageService({});

describe('DotEditLayoutComponent', () => {
    let layoutDesigner: DotEditLayoutDesignerComponentMock;

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                declarations: [DotEditLayoutDesignerComponentMock, DotEditLayoutComponent],
                providers: [
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
                            save: jasmine.createSpy().and.returnValue(of(mockDotRenderedPage()))
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
                                    data: observableOf({
                                        content: {
                                            ...mockDotRenderedPage()
                                        }
                                    })
                                }
                            }
                        }
                    }
                ]
            });
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotEditLayoutComponent);
        fixture.detectChanges();
        layoutDesigner = fixture.debugElement.query(By.css('dot-edit-layout-designer'))
            .componentInstance;
    });

    it('should be 100% min-width in the host', () => {
        // https://github.com/dotCMS/core/issues/19540
        expect(fixture.debugElement.nativeElement.style.minWidth).toBe('100%');
    });

    it('should pass attr to the dot-edit-layout-designer', () => {
        const state = mockDotRenderedPage();
        expect(layoutDesigner.layout).toEqual(state.layout);
        expect(layoutDesigner.theme).toEqual(state.template.theme);
        expect(layoutDesigner.title).toEqual(state.page.title);
        expect(layoutDesigner.apiLink).toEqual('api/v1/page/render/an/url/test?language_id=1');
        expect(layoutDesigner.url).toEqual(state.page.pageURI);
    });
});

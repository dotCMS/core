import { of } from 'rxjs';

import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import {
    DotHttpErrorManagerService,
    DotMessageService,
    DotPageLayoutService,
    DotRouterService,
    DotSessionStorageService,
    DotGlobalMessageService,
    DotPageStateService
} from '@dotcms/data-access';
import { DotLayout, DotPageRender, DotTemplateDesigner } from '@dotcms/dotcms-models';
import { MockDotMessageService, mockDotRenderedPage } from '@dotcms/utils-testing';

import { DotEditLayoutComponent } from './dot-edit-layout.component';

import { DotTemplateContainersCacheService } from '../../../../api/services/dot-template-containers-cache/dot-template-containers-cache.service';
import { DotShowHideFeatureDirective } from '../../../../shared/directives/dot-show-hide-feature/dot-show-hide-feature.directive';
import { EMPTY_TEMPLATE_DESIGN } from '../../../dot-templates/dot-template-create-edit/store/dot-template.store';

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'dotcms-template-builder-lib',
    template: '',
    standalone: false
})
export class MockTemplateBuilderComponent {
    @Input()
    layout: DotLayout;

    @Input()
    themeId: string;

    @Output()
    templateChange: EventEmitter<DotTemplateDesigner> = new EventEmitter();
}

const PAGE_STATE = new DotPageRender(mockDotRenderedPage());

let fixture: ComponentFixture<DotEditLayoutComponent>;

const messageServiceMock = new MockDotMessageService({
    'dot.common.message.saving': 'Saving',
    'dot.common.message.saved': 'Saved'
});

describe('DotEditLayoutComponent', () => {
    let component: DotEditLayoutComponent;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [DotEditLayoutComponent, MockTemplateBuilderComponent],
            imports: [DotShowHideFeatureDirective, RouterTestingModule],
            providers: [
                RouterTestingModule,
                DotSessionStorageService,
                DotRouterService,
                {
                    provide: DotPageStateService,
                    useValue: {
                        state$: of(PAGE_STATE)
                    }
                },
                {
                    provide: DotHttpErrorManagerService,
                    useValue: {
                        handle: jest.fn().mockReturnValue(of({}))
                    }
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: DotGlobalMessageService,
                    useValue: {
                        loading: jest.fn(),
                        success: jest.fn(),
                        error: jest.fn()
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
                        set: jest.fn()
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
                }
            ]
        });
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotEditLayoutComponent);
        component = fixture.componentInstance;
    });
    describe('New Template Builder', () => {
        it('should show new template builder component', () => {
            fixture.detectChanges();
            const component: DebugElement = fixture.debugElement.query(
                By.css('[data-testId="new-template-builder"]')
            );

            expect(component).toBeTruthy();
        });

        it('should emit events from new-template-builder when the layout is changed', () => {
            const builder = fixture.debugElement.query(
                By.css('[data-testId="new-template-builder"]')
            );
            const template = {
                layout: EMPTY_TEMPLATE_DESIGN.layout,
                themeId: '123'
            } as DotTemplateDesigner;

            jest.spyOn(component.updateTemplate, 'next');

            builder.triggerEventHandler('templateChange', template);

            expect(component.updateTemplate.next).toHaveBeenCalled();
        });
    });
});

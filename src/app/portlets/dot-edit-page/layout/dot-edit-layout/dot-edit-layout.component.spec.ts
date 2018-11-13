import { of as observableOf } from 'rxjs';
import { DotRenderedPage } from './../../shared/models/dot-rendered-page.model';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotEditLayoutAdvancedModule } from '../dot-edit-layout-advanced/dot-edit-layout-advanced.module';
import { DotEditLayoutComponent } from './dot-edit-layout.component';
import { DotEditLayoutDesignerModule } from '../dot-edit-layout-designer/dot-edit-layout-designer.module';
import { LoginService, SiteService } from 'dotcms-js';
import { LoginServiceMock } from '../../../../test/login-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { mockDotRenderedPage } from '../../../../test/dot-rendered-page.mock';
import { mockDotPageState } from '../../content/dot-edit-content.component.spec';
import { DotPageStateServiceMock } from '../../../../test/dot-page-state.service.mock';
import { DotPageStateService } from '../../content/services/dot-page-state/dot-page-state.service';
import { SiteServiceMock } from '../../../../test/site-service.mock';

const getTestingModule = (dotRenderedPage?: DotRenderedPage) => {
    return {
        declarations: [DotEditLayoutComponent],
        imports: [
            BrowserAnimationsModule,
            DotEditLayoutAdvancedModule,
            DotEditLayoutDesignerModule,
            RouterTestingModule
        ],
        providers: [
            {
                provide: LoginService,
                useClass: LoginServiceMock
            },
            {
                provide: ActivatedRoute,
                useValue: {
                    parent: {
                        parent: {
                            data: observableOf({
                                content: {
                                    ...(dotRenderedPage || mockDotRenderedPage),
                                    state: mockDotPageState
                                }
                            })
                        }
                    }
                }
            },
            {
                provide: DotPageStateService,
                useClass: DotPageStateServiceMock
            },
            {
                provide: SiteService,
                useClass: SiteServiceMock
            }
        ]
    };
};

let fixture: ComponentFixture<DotEditLayoutComponent>;

describe('DotEditLayoutComponent with Layout Designer', () => {
    beforeEach(async(() => {
        DOTTestBed.configureTestingModule(getTestingModule());
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotEditLayoutComponent);
        fixture.detectChanges();
    });

    it('should have dot-edit-layout-designer', () => {
        const layoutDesigner: DebugElement = fixture.debugElement.query(
            By.css('dot-edit-layout-designer')
        );
        expect(layoutDesigner).toBeTruthy();
    });

    it('should pass pageView to the dot-edit-layout-designer', () => {
        const layoutDesigner: DebugElement = fixture.debugElement.query(
            By.css('dot-edit-layout-designer')
        );
        expect(layoutDesigner.componentInstance.pageState).toEqual({
            ...mockDotRenderedPage,
            state: mockDotPageState
        });
    });
});

// Advance template support was removed on commit https://github.com/dotCMS/core-web/pull/589

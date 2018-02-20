import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs/Observable';

import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotEditLayoutAdvancedModule } from '../dot-edit-layout-advanced/dot-edit-layout-advanced.module';
import { DotEditLayoutComponent } from './dot-edit-layout.component';
import { DotEditLayoutDesignerModule } from '../dot-edit-layout-designer/dot-edit-layout-designer.module';
import { fakePageView } from '../../../../test/page-view.mock';
import { LoginService } from 'dotcms-js/dotcms-js';
import { LoginServiceMock } from '../../../../test/login-service.mock';
import { FormatDateService } from '../../../../api/services/format-date-service';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { DotPageView } from '../../shared/models/dot-page-view.model';

const getTestingModule = (pageView?: DotPageView) => {
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
                            data: Observable.of({ pageView: pageView || fakePageView })
                        }
                    }
                }
            }
        ]
    };
};

let component: DotEditLayoutComponent;
let fixture: ComponentFixture<DotEditLayoutComponent>;

describe('DotEditLayoutComponent with Layout Designer', () => {
    beforeEach(
        async(() => {
            DOTTestBed.configureTestingModule(getTestingModule());
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotEditLayoutComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should have dot-edit-layout-designer', () => {
        const layoutDesigner: DebugElement = fixture.debugElement.query(By.css('dot-edit-layout-designer'));
        expect(layoutDesigner).toBeTruthy();
    });

    it('should pass pageView to the dot-edit-layout-designer', () => {
        const layoutDesigner: DebugElement = fixture.debugElement.query(By.css('dot-edit-layout-designer'));
        expect(layoutDesigner.componentInstance.pageView).toEqual(fakePageView);
    });
});

const advancedTemplateFakePageView: DotPageView = {
    ...fakePageView,
    template: {
        ...fakePageView.template,
        drawed: false
    }
};

describe('DotEditLayoutComponent with Edit Advanced Layout', () => {
    beforeEach(
        async(() => {
            DOTTestBed.configureTestingModule(getTestingModule(advancedTemplateFakePageView));
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotEditLayoutComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should have dot-edit-layout-advanced', () => {
        const layoutDesigner: DebugElement = fixture.debugElement.query(By.css('dot-edit-layout-advanced'));
        expect(layoutDesigner).toBeTruthy();
    });

    it('should pass templateInode to the dot-edit-layout-advanced', () => {
        const layoutEditorAdvanced: DebugElement = fixture.debugElement.query(By.css('dot-edit-layout-advanced'));
        expect(layoutEditorAdvanced.componentInstance.templateInode).toEqual('123');
    });
});

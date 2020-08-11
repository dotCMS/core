/* tslint:disable:no-unused-variable */
import { async, ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';

import { DotPortletDetailComponent } from './dot-portlet-detail.component';
import { DOTTestBed } from '../../test/dot-test-bed';
import { DotWorkflowTaskModule } from './dot-workflow-task/dot-workflow-task.module';
import { DotMenuService } from '@services/dot-menu.service';
import { LoginService } from 'dotcms-js';
import { LoginServiceMock } from '../../test/login-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotContentletsModule } from './dot-contentlets/dot-contentlets.module';
import { ActivatedRoute } from '@angular/router';

describe('DotPortletDetailComponent', () => {
    let fixture: ComponentFixture<DotPortletDetailComponent>;
    let de: DebugElement;
    let router: ActivatedRoute;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            providers: [
                DotMenuService,
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                }
            ],
            declarations: [DotPortletDetailComponent],
            imports: [
                DotWorkflowTaskModule,
                DotContentletsModule,
                RouterTestingModule,
                BrowserAnimationsModule
            ]
        });
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotPortletDetailComponent);
        de = fixture.debugElement;
        router = de.injector.get(ActivatedRoute);
    });

    it('should not have dot-workflow-task', () => {
        spyOnProperty(router, 'parent', 'get').and.returnValue({
            parent: {
                snapshot: {
                    params: {
                        id: ''
                    }
                }
            }
        });

        fixture.detectChanges();
        expect(de.query(By.css('dot-workflow-task')) === null).toBe(true);
        expect(de.query(By.css('dot-contentlets')) === null).toBe(false);
    });

    it('should have dot-workflow-task', () => {
        spyOnProperty(router, 'parent', 'get').and.returnValue({
            parent: {
                snapshot: {
                    params: {
                        id: 'workflow'
                    }
                }
            }
        });

        fixture.detectChanges();
        expect(de.query(By.css('dot-workflow-task'))).toBeTruthy();
        expect(de.query(By.css('dot-contentlets')) === null).toBe(true);
    });

    it('should have dot-contentlets', () => {
        spyOnProperty(router, 'parent', 'get').and.returnValue({
            parent: {
                snapshot: {
                    params: {
                        id: 'content'
                    }
                }
            }
        });

        fixture.detectChanges();
        expect(de.query(By.css('dot-contentlets'))).toBeTruthy();
        expect(de.query(By.css('dot-workflow-task')) === null).toBe(true);
    });
});

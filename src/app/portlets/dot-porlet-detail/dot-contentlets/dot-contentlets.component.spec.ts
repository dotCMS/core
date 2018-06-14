import { DOTTestBed } from '../../../test/dot-test-bed';
import { Injectable, DebugElement } from '@angular/core';
import { DotNavigationService } from '../../../view/components/dot-navigation/dot-navigation.service';
import { ActivatedRoute } from '@angular/router';
import { DotContentletsComponent } from './dot-contentlets.component';
import { DotContentletEditorService } from '../../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotContentletEditorModule } from '../../../view/components/dot-contentlet-editor/dot-contentlet-editor.module';
import { ComponentFixture, async } from '@angular/core/testing';
import { LoginService } from 'dotcms-js/dotcms-js';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { By } from '@angular/platform-browser';
import { DotRouterService } from '../../../api/services/dot-router/dot-router.service';
import { RouterTestingModule } from '@angular/router/testing';
import { DotIframeService } from '../../../view/components/_common/iframe/service/dot-iframe/dot-iframe.service';

@Injectable()
class MockDotNavigationService {
    goToFirstPortlet = jasmine.createSpy('goToFirstPortlet');
}

@Injectable()
class MockDotContentletEditorService {
    edit = jasmine.createSpy('edit');
}

describe('DotContentletsComponent', () => {
    let fixture: ComponentFixture<DotContentletsComponent>;
    let de: DebugElement;

    let dotRouterService: DotRouterService;
    let dotIframeService: DotIframeService;
    let dotContentletEditorService: DotContentletEditorService;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotContentletsComponent],
            imports: [DotContentletEditorModule, RouterTestingModule],
            providers: [
                DotContentletEditorService,
                DotIframeService,
                {
                    provide: ActivatedRoute,
                    useValue: {
                        snapshot: {
                            params: {
                                asset: '5cd3b647-e465-4a6d-a78b-e834a7a7331a'
                            }
                        }
                    }
                },
                {
                    provide: DotContentletEditorService,
                    useClass: MockDotContentletEditorService
                },

                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                }
            ]
        });

        fixture = DOTTestBed.createComponent(DotContentletsComponent);
        de = fixture.debugElement;
        dotRouterService = de.injector.get(DotRouterService);
        dotIframeService = de.injector.get(DotIframeService);
        dotContentletEditorService = de.injector.get(DotContentletEditorService);

        spyOn(dotRouterService, 'gotoPortlet');
        spyOnProperty(dotRouterService, 'currentPortlet', 'get').and.returnValue({
            id: 'current-portlet'
        });

        spyOn(dotIframeService, 'reloadData');
        fixture.detectChanges();
    });

    it('should call contentlet modal', async(() => {
        const params = {
            data: {
                inode: '5cd3b647-e465-4a6d-a78b-e834a7a7331a'
            }
        };
        setTimeout(() => {
            expect(dotContentletEditorService.edit).toHaveBeenCalledWith(params);
        }, 0);
    }));

    it('should go current portlet and reload data when modal closed', () => {
        const edit = de.query(By.css('dot-edit-contentlet'));
        edit.triggerEventHandler('close', {});
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/c/current-portlet');
        expect(dotIframeService.reloadData).toHaveBeenCalledWith('current-portlet');
    });
});

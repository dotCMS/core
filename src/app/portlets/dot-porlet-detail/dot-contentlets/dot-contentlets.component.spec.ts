import { DOTTestBed } from '../../../test/dot-test-bed';
import { Injectable, DebugElement } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DotContentletsComponent } from './dot-contentlets.component';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotContentletEditorModule } from '@components/dot-contentlet-editor/dot-contentlet-editor.module';
import { ComponentFixture } from '@angular/core/testing';
import { LoginService } from 'dotcms-js';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { By } from '@angular/platform-browser';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { RouterTestingModule } from '@angular/router/testing';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';

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
    let dotCustomEventHandlerService: DotCustomEventHandlerService;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotContentletsComponent],
            imports: [DotContentletEditorModule, RouterTestingModule],
            providers: [
                DotContentletEditorService,
                DotIframeService,
                DotCustomEventHandlerService,
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
        dotCustomEventHandlerService = de.injector.get(DotCustomEventHandlerService);

        spyOn(dotIframeService, 'reloadData');
        fixture.detectChanges();
    });

    it('should call contentlet modal', () => {
        const params = {
            data: {
                inode: '5cd3b647-e465-4a6d-a78b-e834a7a7331a'
            }
        };
        fixture.whenStable().then(() => {
            expect(dotContentletEditorService.edit).toHaveBeenCalledWith(params);
        });
    });

    it('should go current portlet and reload data when modal closed', () => {
        const edit = de.query(By.css('dot-edit-contentlet'));
        edit.triggerEventHandler('close', {});
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/c/123-567');
        expect(dotIframeService.reloadData).toHaveBeenCalledWith('123-567');
    });

    it('should call dotCustomEventHandlerService on customEvent', () => {
        spyOn(dotCustomEventHandlerService, 'handle');
        const edit = de.query(By.css('dot-edit-contentlet'));
        edit.triggerEventHandler('custom', { data: 'test' });
        expect(dotCustomEventHandlerService.handle).toHaveBeenCalledWith({ data: 'test' });
    });
});

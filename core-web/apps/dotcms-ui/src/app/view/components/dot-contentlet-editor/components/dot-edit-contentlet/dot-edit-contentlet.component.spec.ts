import { of as observableOf } from 'rxjs';

import { DebugElement } from '@angular/core';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { DotCustomEventHandlerService } from '@dotcms/app/api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotMessageDisplayService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotMessageDisplayServiceMock, LoginServiceMock } from '@dotcms/utils-testing';

import { DotEditContentletComponent } from './dot-edit-contentlet.component';

import { DotMenuService } from '../../../../../api/services/dot-menu.service';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { IframeOverlayService } from '../../../_common/iframe/service/iframe-overlay.service';
import { DotIframeDialogComponent } from '../../../dot-iframe-dialog/dot-iframe-dialog.component';
import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';
import { DotContentletWrapperComponent } from '../dot-contentlet-wrapper/dot-contentlet-wrapper.component';

describe('DotEditContentletComponent', () => {
    let component: DotEditContentletComponent;
    let de: DebugElement;
    let fixture: ComponentFixture<DotEditContentletComponent>;
    let dotEditContentletWrapper: DebugElement;
    let dotEditContentletWrapperComponent: DotContentletWrapperComponent;
    let dotContentletEditorService: DotContentletEditorService;

    beforeEach(waitForAsync(() => {
        DOTTestBed.configureTestingModule({
            imports: [
                DotEditContentletComponent,
                DotContentletWrapperComponent,
                DotIframeDialogComponent,
                BrowserAnimationsModule,
                RouterTestingModule
            ],
            providers: [
                DotContentletEditorService,
                IframeOverlayService,
                {
                    provide: DotMessageDisplayService,
                    useClass: DotMessageDisplayServiceMock
                },
                {
                    provide: DotMenuService,
                    useValue: {
                        getDotMenuId() {
                            return observableOf('999');
                        }
                    }
                },
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                {
                    provide: DotCustomEventHandlerService,
                    useValue: {
                        handle: jest.fn()
                    }
                }
            ]
        });
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotEditContentletComponent);
        de = fixture.debugElement;
        component = de.componentInstance;
        dotContentletEditorService = de.injector.get(DotContentletEditorService);

        jest.spyOn(component.shutdown, 'emit');

        fixture.detectChanges();

        dotEditContentletWrapper = de.query(By.css('dot-contentlet-wrapper'));
        dotEditContentletWrapperComponent = dotEditContentletWrapper.componentInstance;
    });

    describe('default', () => {
        it('should have dot-contentlet-wrapper', () => {
            expect(dotEditContentletWrapper).toBeTruthy();
        });

        it('should emit shutdown', () => {
            dotEditContentletWrapper.triggerEventHandler('shutdown', {});
            expect(component.shutdown.emit).toHaveBeenCalledTimes(1);
        });

        it('should have url in null', () => {
            expect(dotEditContentletWrapperComponent.url).toEqual(null);
        });

        it('should set url', () => {
            dotContentletEditorService.edit({
                header: 'This is a header for edit',
                data: {
                    inode: '999'
                }
            });

            fixture.detectChanges();

            expect(dotEditContentletWrapperComponent.url).toEqual(
                [
                    '/c/portal/layout',
                    '?p_p_id=content',
                    '&p_p_action=1',
                    '&p_p_state=maximized',
                    '&p_p_mode=view',
                    '&_content_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet',
                    '&_content_cmd=edit',
                    '&inode=999'
                ].join('')
            );
        });
    });
});

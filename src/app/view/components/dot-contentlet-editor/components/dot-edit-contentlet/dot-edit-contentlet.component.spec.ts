import { of as observableOf } from 'rxjs';
import { By } from '@angular/platform-browser';
import { ComponentFixture, async } from '@angular/core/testing';
import { DebugElement } from '@angular/core';

import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';
import { DotContentletWrapperComponent } from '../dot-contentlet-wrapper/dot-contentlet-wrapper.component';
import { DotEditContentletComponent } from './dot-edit-contentlet.component';
import { DotIframeDialogModule } from '../../../dot-iframe-dialog/dot-iframe-dialog.module';
import { DotMenuService } from '@services/dot-menu.service';
import { LoginService } from 'dotcms-js';
import { LoginServiceMock } from '../../../../../test/login-service.mock';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

describe('DotEditContentletComponent', () => {
    let component: DotEditContentletComponent;
    let de: DebugElement;
    let fixture: ComponentFixture<DotEditContentletComponent>;
    let dotEditContentletWrapper: DebugElement;
    let dotEditContentletWrapperComponent: DotContentletWrapperComponent;
    let dotContentletEditorService: DotContentletEditorService;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotEditContentletComponent, DotContentletWrapperComponent],
            providers: [
                DotContentletEditorService,
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
                }
            ],
            imports: [DotIframeDialogModule, BrowserAnimationsModule, RouterTestingModule]
        });
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotEditContentletComponent);
        de = fixture.debugElement;
        component = de.componentInstance;
        dotContentletEditorService = de.injector.get(DotContentletEditorService);

        spyOn(component.close, 'emit');

        fixture.detectChanges();

        dotEditContentletWrapper = de.query(By.css('dot-contentlet-wrapper'));
        dotEditContentletWrapperComponent = dotEditContentletWrapper.componentInstance;
    });

    describe('default', () => {
        it('should have dot-contentlet-wrapper', () => {
            expect(dotEditContentletWrapper).toBeTruthy();
        });

        it('should emit close', () => {
            dotEditContentletWrapper.triggerEventHandler('close', {});
            expect(component.close.emit).toHaveBeenCalledTimes(1);
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

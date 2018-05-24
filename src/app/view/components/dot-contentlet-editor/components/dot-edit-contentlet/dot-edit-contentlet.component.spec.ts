import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { async, ComponentFixture } from '@angular/core/testing';

import { Observable } from 'rxjs/Observable';

import { LoginService } from 'dotcms-js/dotcms-js';

import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';
import { DotEditContentletComponent } from './dot-edit-contentlet.component';
import { DotIframeDialogComponent } from '../../../dot-iframe-dialog/dot-iframe-dialog.component';
import { DotIframeDialogModule } from '../../../dot-iframe-dialog/dot-iframe-dialog.module';
import { DotMenuService } from '../../../../../api/services/dot-menu.service';
import { LoginServiceMock } from '../../../../../test/login-service.mock';

describe('DotEditContentletComponent', () => {
    let component: DotEditContentletComponent;
    let de: DebugElement;
    let fixture: ComponentFixture<DotEditContentletComponent>;

    let dotIframeDialog: DebugElement;
    let dotIframeDialogComponent: DotIframeDialogComponent;

    let dotAddContentletService: DotContentletEditorService;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotEditContentletComponent],
            providers: [
                DotContentletEditorService,
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                {
                    provide: DotMenuService,
                    useValue: {
                        getDotMenuId() {
                            return Observable.of('999');
                        }
                    }
                }
            ],
            imports: [DotIframeDialogModule, RouterTestingModule, BrowserAnimationsModule]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotEditContentletComponent);
        de = fixture.debugElement;
        component = de.componentInstance;
        dotAddContentletService = de.injector.get(DotContentletEditorService);
        spyOn(dotAddContentletService, 'clear');
        spyOn(dotAddContentletService, 'load');
        spyOn(dotAddContentletService, 'keyDown');
        fixture.detectChanges();

        dotIframeDialog = de.query(By.css('dot-iframe-dialog'));
        dotIframeDialogComponent = dotIframeDialog.componentInstance;
    });

    it('should have dot-iframe-dialog', () => {
        expect(dotIframeDialog).toBeTruthy();
    });

    describe('with data', () => {
        beforeEach(() => {
            dotAddContentletService.edit({
                data: {
                    inode: '123',
                },
                events: {
                    load: jasmine.createSpy(),
                    keyDown: jasmine.createSpy()
                }
            });

            spyOn(component, 'onClose').and.callThrough();
            spyOn(component, 'onLoad').and.callThrough();
            spyOn(component, 'onKeyDown').and.callThrough();
            spyOn(component.close, 'emit');
            fixture.detectChanges();
        });

        it('should have dot-iframe-dialog url set', () => {
            expect(dotIframeDialogComponent.url).toEqual(
                [
                    `/c/portal/layout`,
                    `?p_l_id=999`,
                    `&p_p_id=content`,
                    `&p_p_action=1`,
                    `&p_p_state=maximized`,
                    `&p_p_mode=view`,
                    `&_content_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet`,
                    `&_content_cmd=edit&inode=123`
                ].join('')
            );
        });

        describe('events', () => {
            it('should call clear and emit close', () => {
                dotIframeDialog.triggerEventHandler('close', {});
                expect(dotAddContentletService.clear).toHaveBeenCalledTimes(1);
                expect(component.close.emit).toHaveBeenCalledTimes(1);
            });

            it('should call load', () => {
                dotIframeDialog.triggerEventHandler('load', { hello: 'world' });
                expect(dotAddContentletService.load).toHaveBeenCalledWith({ hello: 'world' });
            });

            it('should call keyDown', () => {
                dotIframeDialog.triggerEventHandler('keydown', { hello: 'world' });
                expect(dotAddContentletService.keyDown).toHaveBeenCalledWith({ hello: 'world' });
            });

            it('should close the dialog', () => {
                dotIframeDialog.triggerEventHandler('custom', {
                    detail: {
                        name: 'close'
                    }
                });
                expect(dotAddContentletService.clear).toHaveBeenCalledTimes(1);
            });
        });
    });
});

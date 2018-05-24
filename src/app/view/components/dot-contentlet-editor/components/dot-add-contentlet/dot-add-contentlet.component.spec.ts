import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { async, ComponentFixture } from '@angular/core/testing';

import { LoginService } from 'dotcms-js/dotcms-js';

import { Observable } from 'rxjs/Observable';

import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';
import { DotAddContentletComponent } from './dot-add-contentlet.component';
import { DotIframeDialogComponent } from '../../../dot-iframe-dialog/dot-iframe-dialog.component';
import { DotIframeDialogModule } from '../../../dot-iframe-dialog/dot-iframe-dialog.module';
import { DotMenuService } from '../../../../../api/services/dot-menu.service';
import { LoginServiceMock } from '../../../../../test/login-service.mock';

describe('DotAddContentletComponent', () => {
    let component: DotAddContentletComponent;
    let de: DebugElement;
    let fixture: ComponentFixture<DotAddContentletComponent>;

    let dotIframeDialog: DebugElement;
    let dotIframeDialogComponent: DotIframeDialogComponent;

    let dotAddContentletService: DotContentletEditorService;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotAddContentletComponent],
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
        fixture = DOTTestBed.createComponent(DotAddContentletComponent);
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
            dotAddContentletService.add({
                header: 'Add dome content',
                data: {
                    container: '123',
                    baseTypes: 'content,form'
                },
                events: {
                    load: jasmine.createSpy(),
                    keyDown: jasmine.createSpy()
                }
            });

            spyOn(component, 'onClose').and.callThrough();
            spyOn(component, 'onLoad').and.callThrough();
            spyOn(component, 'onKeyDown').and.callThrough();
            fixture.detectChanges();
        });

        it('should have dot-iframe-dialog url set', () => {
            expect(dotIframeDialogComponent.url).toEqual('/html/ng-contentlet-selector.jsp?ng=true&container_id=123&add=content,form');
        });

        it('should have dot-iframe-dialog header set', () => {
            expect(dotIframeDialogComponent.header).toEqual('Add dome content');
        });

        describe('events', () => {
            it('should call clear', () => {
                dotIframeDialog.triggerEventHandler('close', {});
                expect(dotAddContentletService.clear).toHaveBeenCalledTimes(1);
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

import { By } from '@angular/platform-browser';
import { ComponentFixture, async } from '@angular/core/testing';
import { DebugElement } from '@angular/core';

import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';
import { DotContentletWrapperComponent } from '../dot-contentlet-wrapper/dot-contentlet-wrapper.component';
import { DotAddContentletComponent } from './dot-add-contentlet.component';
import { DotIframeDialogModule } from '../../../dot-iframe-dialog/dot-iframe-dialog.module';
import { DotMenuService } from '@services/dot-menu.service';
import { LoginService } from 'dotcms-js';
import { LoginServiceMock } from '../../../../../test/login-service.mock';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

describe('DotAddContentletComponent', () => {
    let component: DotAddContentletComponent;
    let de: DebugElement;
    let fixture: ComponentFixture<DotAddContentletComponent>;
    let dotAddContentletWrapper: DebugElement;
    let dotAddContentletWrapperComponent: DotContentletWrapperComponent;
    let dotContentletEditorService: DotContentletEditorService;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotAddContentletComponent, DotContentletWrapperComponent],
            providers: [
                DotContentletEditorService,
                DotMenuService,
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                }
            ],
            imports: [DotIframeDialogModule, BrowserAnimationsModule, RouterTestingModule]
        });
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotAddContentletComponent);
        de = fixture.debugElement;
        component = de.componentInstance;
        dotContentletEditorService = de.injector.get(DotContentletEditorService);

        spyOn(component.close, 'emit');

        fixture.detectChanges();

        dotAddContentletWrapper = de.query(By.css('dot-contentlet-wrapper'));
        dotAddContentletWrapperComponent = dotAddContentletWrapper.componentInstance;
    });

    describe('default', () => {
        it('should have dot-contentlet-wrapper', () => {
            expect(dotAddContentletWrapper).toBeTruthy();
        });

        it('should emit close', () => {
            dotAddContentletWrapper.triggerEventHandler('close', {});
            expect(component.close.emit).toHaveBeenCalledTimes(1);
        });

        it('should have url in null', () => {
            expect(dotAddContentletWrapperComponent.url).toEqual(null);
        });

        it('should set url', () => {
            dotContentletEditorService.add({
                header: 'Add some content',
                data: {
                    container: '123',
                    baseTypes: 'content,form'
                },
                events: {
                    load: jasmine.createSpy('load'),
                    keyDown: jasmine.createSpy('keyDown')
                }
            });

            fixture.detectChanges();

            expect(dotAddContentletWrapperComponent.url).toEqual(
                '/html/ng-contentlet-selector.jsp?ng=true&container_id=123&add=content,form'
            );

            expect(dotAddContentletWrapperComponent.header).toEqual('Add some content');
        });
    });
});

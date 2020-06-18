import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotReorderMenuComponent } from './dot-reorder-menu.component';
import { DotIframeDialogModule } from '../../../dot-iframe-dialog/dot-iframe-dialog.module';
import { LoginService } from 'dotcms-js';
import { LoginServiceMock } from '../../../../../test/login-service.mock';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { RouterTestingModule } from '@angular/router/testing';

describe('DotReorderMenuComponent', () => {
    let component: DotReorderMenuComponent;
    let de: DebugElement;
    let fixture: ComponentFixture<DotReorderMenuComponent>;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'editpage.content.contentlet.menu.reorder.title': 'Menu order Title'
        });

        DOTTestBed.configureTestingModule({
            declarations: [DotReorderMenuComponent],
            providers: [
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ],
            imports: [DotIframeDialogModule, BrowserAnimationsModule, RouterTestingModule]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotReorderMenuComponent);
        de = fixture.debugElement;
        component = de.componentInstance;
        component.url = 'test';
    });

    describe('default', () => {
        it('should have right params', () => {
            fixture.detectChanges();
            const dotIframeDialogElement = de.query(By.css('dot-iframe-dialog')).componentInstance;
            expect(dotIframeDialogElement).not.toBe(null);
            expect(dotIframeDialogElement.header).toBe('Menu order Title');
            expect(dotIframeDialogElement.url).toBe('test');
        });

        it('should emit close', () => {
            spyOn(component.close, 'emit');
            fixture.detectChanges();
            const dotIframeDialogElement = de.query(By.css('dot-iframe-dialog')).componentInstance;
            dotIframeDialogElement.close.emit();
            expect(component.close.emit).toHaveBeenCalledTimes(1);
        });
    });
});

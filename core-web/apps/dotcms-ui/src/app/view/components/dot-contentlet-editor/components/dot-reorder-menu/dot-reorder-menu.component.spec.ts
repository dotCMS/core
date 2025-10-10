import { DebugElement } from '@angular/core';
import { ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { DotMessageService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import { LoginServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotReorderMenuComponent } from './dot-reorder-menu.component';

import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotIframeDialogModule } from '../../../dot-iframe-dialog/dot-iframe-dialog.module';

describe('DotReorderMenuComponent', () => {
    let component: DotReorderMenuComponent;
    let de: DebugElement;
    let fixture: ComponentFixture<DotReorderMenuComponent>;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'editpage.content.contentlet.menu.reorder.title': 'Menu order Title'
        });

        DOTTestBed.configureTestingModule({
            imports: [DotReorderMenuComponent],
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
            imports: [
                DotIframeDialogModule,
                BrowserAnimationsModule,
                RouterTestingModule,
                DotMessagePipe
            ]
        });
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

        it('should emit shutdown', () => {
            jest.spyOn(component.shutdown, 'emit');
            fixture.detectChanges();
            const dotIframeDialogElement = de.query(By.css('dot-iframe-dialog')).componentInstance;
            dotIframeDialogElement.shutdown.emit();
            expect(component.shutdown.emit).toHaveBeenCalledTimes(1);
        });
    });
});

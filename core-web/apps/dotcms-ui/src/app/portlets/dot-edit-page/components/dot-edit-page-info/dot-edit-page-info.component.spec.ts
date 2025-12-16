import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';
import { DotApiLinkComponent, DotCopyButtonComponent, DotMessagePipe } from '@dotcms/ui';

import { DotEditPageInfoComponent } from './dot-edit-page-info.component';

import { DotLinkComponent } from '../../../../view/components/dot-link/dot-link.component';

@Component({
    template: `
        <dot-edit-page-info [title]="title" [url]="url" [apiLink]="apiLink"></dot-edit-page-info>
    `,
    standalone: false
})
class TestHostComponent {
    title = 'A title';
    url = 'http://demo.dotcms.com:9876/an/url/test';
    apiLink = 'api/v1/page/render/an/url/test?language_id=1';
}

describe('DotEditPageInfoComponent', () => {
    let hostComp: TestHostComponent;
    let hostFixture: ComponentFixture<TestHostComponent>;
    let hostDebug: DebugElement;
    let de: DebugElement;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent],
            imports: [
                DotEditPageInfoComponent,
                DotApiLinkComponent,
                DotCopyButtonComponent,
                DotLinkComponent,
                DotMessagePipe
            ],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: {
                        get() {
                            return 'Copy url page';
                        }
                    }
                }
            ]
        }).compileComponents();
    }));

    beforeEach(() => {
        hostFixture = TestBed.createComponent(TestHostComponent);
        hostDebug = hostFixture.debugElement;
        hostComp = hostDebug.componentInstance;

        de = hostDebug.query(By.css('dot-edit-page-info'));
    });

    describe('default', () => {
        beforeEach(() => {
            hostFixture.detectChanges();
        });

        it('should set page title', () => {
            const pageTitleEl: HTMLElement = de.query(By.css('h2')).nativeElement;
            expect(pageTitleEl.textContent).toContain('A title');
        });

        it('should have copy button', () => {
            const button: DebugElement = de.query(By.css('dot-copy-button '));

            expect(button).not.toBeNull();
        });

        it('should have api link', () => {
            const apiLink: DebugElement = de.query(By.css('dot-api-link'));

            expect(apiLink.componentInstance.href).toBe(
                'api/v1/page/render/an/url/test?language_id=1'
            );
        });

        it('should have preview link', () => {
            const previewLink: DebugElement = de.query(By.css('dot-link[icon="pi-eye"] a'));

            expect(previewLink.nativeElement.href).toContain(
                '/an/url/test?language_id=1&disabledNavigateMode=true'
            );
        });
    });

    describe('hidden', () => {
        beforeEach(() => {
            hostComp.title = 'A title';
            hostComp.apiLink = '';
            hostComp.url = '';

            hostFixture.detectChanges();
        });

        it('should not have api link', () => {
            const apiLink: DebugElement = de.query(By.css('dot-api-link'));
            expect(apiLink).toBeNull();
        });

        it('should not have copy button', () => {
            const button: DebugElement = de.query(By.css('dot-copy-button '));
            expect(button).toBeNull();
        });

        it('should not have preview button', () => {
            const previewButton: DebugElement = de.query(By.css('dot-link[icon="pi-eye"]'));
            expect(previewButton).toBeNull();
        });
    });
});

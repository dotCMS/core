import { Observable, of } from 'rxjs';

import { Component, DebugElement, Injectable } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotApiLinkModule } from '@components/dot-api-link/dot-api-link.module';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotMessageService, DotPropertiesService } from '@dotcms/data-access';
import { DotMessagePipeModule } from '@dotcms/ui';

import { DotEditPageInfoComponent } from './dot-edit-page-info.component';

@Injectable()
export class MockDotPropertiesService {
    getKey(): Observable<true> {
        return of(true);
    }
}

@Component({
    template: `<dot-edit-page-info
        [title]="title"
        [url]="url"
        [apiLink]="apiLink"
    ></dot-edit-page-info>`
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
    let dotPropertiesService: DotPropertiesService;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent, DotEditPageInfoComponent],
            imports: [DotApiLinkModule, DotCopyButtonModule, DotMessagePipeModule],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: {
                        get() {
                            return 'Copy url page';
                        }
                    }
                },
                { provide: DotPropertiesService, useClass: MockDotPropertiesService }
            ]
        }).compileComponents();
    }));

    beforeEach(() => {
        hostFixture = TestBed.createComponent(TestHostComponent);
        dotPropertiesService = TestBed.inject(DotPropertiesService);
        hostDebug = hostFixture.debugElement;
        hostComp = hostDebug.componentInstance;
        de = hostDebug.query(By.css('dot-edit-page-info'));
    });

    describe('default', () => {
        beforeEach(() => {
            spyOn(dotPropertiesService, 'getKey').and.returnValue(of('false'));
            hostFixture.detectChanges();
        });

        it('should set page title', () => {
            const pageTitleEl: HTMLElement = de.query(By.css('h2')).nativeElement;
            expect(pageTitleEl.textContent).toContain('A title');
        });

        it('should have copy button', () => {
            const button: DebugElement = de.query(By.css('dot-copy-button '));
            expect(button.componentInstance.copy).toBe('http://demo.dotcms.com:9876/an/url/test');
            expect(button.componentInstance.tooltipText).toBe('Copy url page');
        });

        it('should have api link', () => {
            const apiLink: DebugElement = de.query(By.css('dot-api-link'));
            expect(apiLink.componentInstance.href).toBe(
                'api/v1/page/render/an/url/test?language_id=1'
            );
        });

        it('should have preview link', () => {
            const previewLink: DebugElement = de.query(By.css('dot-link[icon="pi-eye"]'));

            expect(previewLink.nativeElement.href).toBe(
                '/an/url/test?language_id=1&disabledNavigateMode=true'
            );
        });
    });

    describe('hidden', () => {
        beforeEach(() => {
            hostComp.title = 'A title';
            hostComp.apiLink = '';
            hostComp.url = '';
            spyOn(dotPropertiesService, 'getKey').and.returnValue(of('false'));
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

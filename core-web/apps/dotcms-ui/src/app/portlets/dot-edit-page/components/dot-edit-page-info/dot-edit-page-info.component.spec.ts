import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';

import { DotApiLinkModule } from '@components/dot-api-link/dot-api-link.module';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotEditPageInfoComponent } from './dot-edit-page-info.component';

import { DotMessagePipe } from '@pipes/dot-message/dot-message.pipe';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

describe('DotEditPageInfoComponent', () => {
    let component: DotEditPageInfoComponent;
    let fixture: ComponentFixture<DotEditPageInfoComponent>;
    let de: DebugElement;

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                declarations: [DotEditPageInfoComponent, DotMessagePipe],
                imports: [DotApiLinkModule, DotCopyButtonModule],
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
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotEditPageInfoComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
    });

    describe('default', () => {
        beforeEach(() => {
            component.apiLink = '/api/v1/page/render/an/url/test?language_id=1';
            component.title = 'A title';
            component.url = 'http://demo.dotcms.com:9876/an/url/test';
            fixture.detectChanges();
        });

        it('should set page title', () => {
            const pageTitleEl: HTMLElement = de.query(By.css('h2')).nativeElement;
            expect(pageTitleEl.textContent).toContain('A title');
        });

        it('should have api link', () => {
            const apiLink: DebugElement = de.query(By.css('dot-api-link'));
            expect(apiLink.componentInstance.link).toBe(
                '/api/v1/page/render/an/url/test?language_id=1'
            );
        });

        it('should have copy button', () => {
            const button: DebugElement = de.query(By.css('dot-copy-button '));
            expect(button.componentInstance.copy).toBe('http://demo.dotcms.com:9876/an/url/test');
            expect(button.componentInstance.tooltipText).toBe('Copy url page');
        });
    });

    describe('hidden', () => {
        beforeEach(() => {
            component.title = 'A title';
            component.apiLink = '';
            component.url = '';

            fixture.detectChanges();
        });

        it('should not have api link', () => {
            const apiLink: DebugElement = de.query(By.css('dot-api-link'));
            expect(apiLink).toBeNull();
        });

        it('should not have copy button', () => {
            const button: DebugElement = de.query(By.css('dot-copy-button '));
            expect(button).toBeNull();
        });
    });
});

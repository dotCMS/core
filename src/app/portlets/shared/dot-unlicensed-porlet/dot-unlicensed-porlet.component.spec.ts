import { async, ComponentFixture } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { DotUnlicensedPorletComponent } from '.';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DOTTestBed } from '@tests/dot-test-bed';
import { By } from '@angular/platform-browser';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';

describe('DotUnlicensedPorletComponent', () => {
    let component: DotUnlicensedPorletComponent;
    let fixture: ComponentFixture<DotUnlicensedPorletComponent>;
    let de: DebugElement;

    beforeEach(async(() => {
        const messageServiceMock = new MockDotMessageService({
            'request.a.trial.license': 'Request'
        });

        DOTTestBed.configureTestingModule({
            declarations: [DotUnlicensedPorletComponent],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ],
            imports: [DotIconModule]
        });
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotUnlicensedPorletComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        component.data = {
            title: 'Hello World',
            description: 'This is a description',
            links: [
                {
                    text: 'Link 1',
                    link: 'http://dotcms.com'
                }
            ]
        };

        fixture.detectChanges();
    });

    it('should have dot-icon', () => {
        expect(de.query(By.css('dot-icon')).componentInstance.size).toBe(120);
    });

    it('should have title', () => {
        expect(de.query(By.css('h4')).nativeElement.textContent).toBe('Hello World');
    });

    it('should have description', () => {
        expect(de.query(By.css('p')).nativeElement.textContent).toBe('This is a description');
    });

    it('should have links', () => {
        const links = de.queryAll(By.css('li a'));
        const link = links[0].nativeElement;

        expect(links.length).toBe(1);
        expect(link.textContent).toBe('Link 1');
        expect(link.href).toBe('http://dotcms.com/');
    });

    it('should have trial button', () => {
        const link = de.query(By.css('p a')).nativeElement;
        expect(link.textContent).toBe('Request');
        expect(link.href).toBe('https://dotcms.com/licensing/request-a-license-3/index');
    });
});

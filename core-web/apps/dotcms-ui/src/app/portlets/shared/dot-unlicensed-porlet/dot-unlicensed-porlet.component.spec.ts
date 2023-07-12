import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';
import { DotIconModule, DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotUnlicensedPorletComponent } from '.';

describe('DotUnlicensedPorletComponent', () => {
    let component: DotUnlicensedPorletComponent;
    let fixture: ComponentFixture<DotUnlicensedPorletComponent>;
    let de: DebugElement;

    beforeEach(waitForAsync(() => {
        const messageServiceMock = new MockDotMessageService({
            'request.a.trial.license': 'Request'
        });

        TestBed.configureTestingModule({
            declarations: [DotUnlicensedPorletComponent],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ],
            imports: [DotIconModule, DotPipesModule, DotMessagePipe]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotUnlicensedPorletComponent);
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

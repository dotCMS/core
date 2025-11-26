import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotInfoPageComponent } from './dot-info-page.component';

const messageServiceMock = new MockDotMessageService({
    title: 'Access denied',
    description: 'Not access',
    'button.text': 'Go To Pages'
});

describe('DotInfoPageComponent', () => {
    let component: DotInfoPageComponent;
    let fixture: ComponentFixture<DotInfoPageComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotInfoPageComponent, RouterTestingModule],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        }).compileComponents();

        fixture = TestBed.createComponent(DotInfoPageComponent);
        component = fixture.componentInstance;
    });

    it('should create', () => {
        const info = {
            icon: 'compass',
            title: 'title',
            description: 'description',
            buttonPath: '/pages',
            buttonText: 'button.text'
        };
        component.info = info;
        fixture.detectChanges();
        const element = fixture.debugElement;
        expect(element.query(By.css('[data-testid="icon"]')).classes).toEqual({
            pi: true,
            'pi-compass': true
        });
        expect(element.query(By.css('[data-testid="title"]')).nativeElement.innerText).toEqual(
            'Access denied'
        );
        expect(
            element.query(By.css('[data-testid="description"]')).nativeElement.innerText
        ).toContain('Not access');
        expect(
            element.query(By.css('[data-testid="button"]')).nativeElement.innerText.trim()
        ).toEqual('Go To Pages');
    });
});

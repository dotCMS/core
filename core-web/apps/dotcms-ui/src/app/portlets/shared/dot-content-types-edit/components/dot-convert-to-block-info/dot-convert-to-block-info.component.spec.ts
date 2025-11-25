import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotConvertToBlockInfoComponent } from './dot-convert-to-block-info.component';

const messageServiceMock = new MockDotMessageService({
    'contenttypes.field.properties.wysiwyg.info.content': 'Info Content',
    'contenttypes.field.properties.wysiwyg.info.button': 'Info Button',
    'learn-more': 'Learn More'
});

describe('DotConvertToBlockInfoComponent', () => {
    let de: DebugElement;
    let fixture: ComponentFixture<DotConvertToBlockInfoComponent>;
    let component: DotConvertToBlockInfoComponent;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotConvertToBlockInfoComponent],
            imports: [DotMessagePipe, ButtonModule],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotConvertToBlockInfoComponent);
        de = fixture.debugElement;
        component = fixture.componentInstance;
    });

    it('should render info and learn more button', () => {
        fixture.detectChanges();

        const infoContent = de.query(By.css('[data-testId="infoContent"]')).nativeElement;
        const learnMore = de.query(By.css('[data-testId="learnMore"]')).nativeElement;

        expect(infoContent.innerText).toBe('Info Content');
        expect(learnMore.innerText).toBe('Learn More');
    });
    it('should render info and info button', () => {
        component.currentField = {
            id: '123'
        };

        fixture.detectChanges();

        const infoContent = de.query(By.css('[data-testId="infoContent"]')).nativeElement;
        const button = de.query(By.css('[data-testId="button"]')).nativeElement;

        expect(infoContent.innerText).toBe('Info Content');
        expect(button.innerText).toBe('Info Button');
    });
});

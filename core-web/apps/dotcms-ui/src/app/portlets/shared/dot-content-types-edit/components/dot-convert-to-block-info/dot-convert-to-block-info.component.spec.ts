import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotConvertToBlockInfoComponent } from './dot-convert-to-block-info.component';

const messageServiceMock = new MockDotMessageService({
    'contenttypes.field.properties.wysiwyg.info.content': 'Info Content',
    'contenttypes.field.properties.wysiwyg.info.button': 'Info Button',
    'learn-more': 'Learn More'
});

describe('DotConvertToBlockInfoComponent', () => {
    beforeEach(async () => {
        TestBed.configureTestingModule({
            imports: [DotConvertToBlockInfoComponent],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ]
        });

        await TestBed.compileComponents();
    });

    it('should render info and learn more button', () => {
        const fixture = TestBed.createComponent(DotConvertToBlockInfoComponent);
        const de = fixture.debugElement;

        fixture.detectChanges();

        const infoContent = de.query(By.css('[data-testId="infoContent"]')).nativeElement;
        const learnMore = de.query(By.css('[data-testId="learnMore"]')).nativeElement;

        expect(infoContent.textContent?.trim()).toBe('Info Content');
        expect(learnMore.textContent?.trim()).toBe('Learn More');
    });

    it('should render info and info button', () => {
        const fixture = TestBed.createComponent(DotConvertToBlockInfoComponent);
        const de = fixture.debugElement;

        fixture.componentRef.setInput('$currentField', {
            id: '123'
        });

        fixture.detectChanges();

        const infoContent = de.query(By.css('[data-testId="infoContent"]'));
        const button = de.query(By.css('[data-testId="button"]'));

        expect(infoContent).toBeTruthy();
        expect(infoContent.nativeElement.textContent?.trim()).toBe('Info Content');
        expect(button).toBeTruthy();
        expect(button.nativeElement.textContent?.trim()).toBe('Info Button');
    });
});

import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DotMessageService } from '@dotcms/app/api/services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@dotcms/app/test/dot-message-service.mock';
import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';
import { DotConvertWysiwygToBlockComponent } from './dot-convert-wysiwyg-to-block.component';

const messageServiceMock = new MockDotMessageService({
    'contenttypes.field.properties.wysiwyg.convert.info.header': 'Info Header',
    'contenttypes.field.properties.wysiwyg.convert.info.content': 'Info Content',
    'contenttypes.field.properties.wysiwyg.convert.header': 'Header',
    'contenttypes.field.properties.wysiwyg.convert.content': 'Content',
    'contenttypes.field.properties.wysiwyg.convert.iunderstand': 'I understand',
    'contenttypes.field.properties.wysiwyg.convert.button': 'Button'
});

describe('DotConvertWysiwygToBlockComponent', () => {
    let fixture: ComponentFixture<DotConvertWysiwygToBlockComponent>;
    let de: DebugElement;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotConvertWysiwygToBlockComponent],
            imports: [DotMessagePipeModule],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotConvertWysiwygToBlockComponent);
        de = fixture.debugElement;
        fixture.detectChanges();
    });

    it('should render all the static content', () => {
        const infoHeader = de.query(By.css('[data-testId="infoHeader"]')).nativeElement;
        const infoContent = de.query(By.css('[data-testId="infoContent"]')).nativeElement;
        const header = de.query(By.css('[data-testId="header"]')).nativeElement;
        const content = de.query(By.css('[data-testId="content"]')).nativeElement;
        const iUnderstand = de.query(By.css('[data-testId="iUnderstand"]')).nativeElement;
        const buttonConvert = de.query(By.css('[data-testId="buttonConvert"]')).nativeElement;

        expect(infoHeader.innerText).toBe('Info Header');
        expect(infoContent.innerText).toBe('Info Content');
        expect(header.innerText).toBe('Header');
        expect(content.innerText).toBe('Content');
        expect(iUnderstand.innerText).toBe('I understand');
        expect(buttonConvert.innerText).toBe('Button');
    });
});

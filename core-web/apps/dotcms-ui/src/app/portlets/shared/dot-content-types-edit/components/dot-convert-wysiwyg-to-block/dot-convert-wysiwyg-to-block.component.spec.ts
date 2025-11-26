import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

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
            imports: [DotMessagePipe, FormsModule, CheckboxModule, ButtonModule],
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

        expect(infoHeader.textContent.trim()).toBe('Info Header');
        expect(infoContent.textContent.trim()).toBe('Info Content');
        expect(header.textContent.trim()).toBe('Header');
        expect(content.textContent.trim()).toBe('Content');
        expect(iUnderstand.textContent.trim()).toBe('I understand');
        expect(buttonConvert.textContent.trim()).toBe('Button');
    });
});

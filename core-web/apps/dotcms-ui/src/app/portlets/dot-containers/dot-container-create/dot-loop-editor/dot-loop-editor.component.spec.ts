import { DotLoopEditorComponent } from './dot-loop-editor.component';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { Component, CUSTOM_ELEMENTS_SCHEMA, DebugElement, forwardRef, Input } from '@angular/core';
import {
    ControlValueAccessor,
    FormControl,
    NG_VALUE_ACCESSOR,
    ReactiveFormsModule
} from '@angular/forms';
import { MockDotMessageService } from '@dotcms/app/test/dot-message-service.mock';
import { DotMessageService } from '@dotcms/app/api/services/dot-message/dot-messages.service';
import { ButtonModule } from 'primeng/button';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

@Component({
    selector: `dot-host-component`,
    template: `<dot-loop-editor
        [formControl]="editor"
        [label]="label"
        [isEditorVisible]="isEditorVisible"
        (buttonClick)="showLoopInput()"
    ></dot-loop-editor>`
})
class DotTestHostComponent {
    isEditorVisible = true;
    label = 'pre_loop';
    editor = new FormControl('');
    showLoopInput() {
        this.isEditorVisible = true;
    }
}

@Component({
    selector: 'dot-textarea-content',
    template: '',
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotTextareaContentMockComponent)
        }
    ]
})
export class DotTextareaContentMockComponent implements ControlValueAccessor {
    @Input() show;
    @Input() height;

    propagateChange = (_: unknown) => {
        //
    };
    registerOnChange(fn): void {
        this.propagateChange = fn;
    }
    registerOnTouched(): void {
        //
    }
    writeValue(): void {
        // no-op
    }
}

const messages = {
    'message.containers.create.pre_loop': 'Pre-loop',
    'message.containers.create.post_loop': 'Post-loop',
    'message.containers.create.add_pre_post': 'Add PRE POST'
};

describe('DotLoopEditorComponent', () => {
    let component: DotTestHostComponent;
    let fixture: ComponentFixture<DotTestHostComponent>;
    let de: DebugElement;
    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [
                DotLoopEditorComponent,
                DotTextareaContentMockComponent,
                DotTestHostComponent
            ],
            imports: [
                DotMessagePipeModule,
                ButtonModule,
                ReactiveFormsModule,
                BrowserAnimationsModule
            ],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }],
            schemas: [CUSTOM_ELEMENTS_SCHEMA]
        }).compileComponents();

        fixture = TestBed.createComponent(DotTestHostComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
        de = fixture.debugElement.query(By.css('dot-loop-editor'));
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should show pre_loop', () => {
        const label = de.query(By.css('[data-testId="label"]')).nativeElement;
        expect(label.innerText).toBe('Pre-loop');
    });

    it('should show post_loop', () => {
        component.label = 'post_loop';
        fixture.detectChanges();
        de = fixture.debugElement.query(By.css('dot-loop-editor'));
        const label = de.query(By.css('[data-testId="label"]')).nativeElement;
        expect(label.innerText).toBe('Post-loop');
    });

    it('should show pre post loop button when Editor is not visible', () => {
        component.isEditorVisible = false;
        fixture.detectChanges();
        de = fixture.debugElement.query(By.css('dot-loop-editor'));
        const showEditorBtn = de.query(By.css('[data-testId="showEditorBtn"]'));
        spyOn(de.componentInstance.buttonClick, 'emit');
        showEditorBtn.triggerEventHandler('click');
        expect(showEditorBtn).toBeDefined();
        expect(de.componentInstance.buttonClick.emit).toHaveBeenCalled();
    });
});

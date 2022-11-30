import { DotLoopEditorComponent } from './dot-loop-editor.component';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
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

@Component({
    selector: `dot-host-component`,
    template: `<dot-loop-editor
        [formControl]="editor"
        [label]="label"
        [isEditorVisible]="isEditorVisible"
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
    'message.containers.create.post_loop': 'Post-loop'
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
            imports: [DotMessagePipeModule, ButtonModule, ReactiveFormsModule],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }],
            schemas: [CUSTOM_ELEMENTS_SCHEMA]
        }).compileComponents();

        fixture = TestBed.createComponent(DotTestHostComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement.query(By.css('dot-loop-editor'));
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should show pre_loop', () => {
        const label = de.query(By.css('[data-testId="label"]')).nativeElement;
        expect(label.innerText).toBe('Pre-loop');
    });

    it('should show pre_loop', fakeAsync(() => {
        component.label = 'post_loop';
        fixture.detectChanges();
        tick();
        de = fixture.debugElement.query(By.css('dot-loop-editor'));
        const label = de.query(By.css('[data-testId="label"]')).nativeElement;
        expect(label.innerText).toBe('Post-loop');
    }));
});

import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';

import { EditorDirective } from './editor.directive';

@Component({
    template: '<div tiptap [editor]="editor"></div>'
})
class TestComponent {
    @Input() editor!: Editor;
}

describe('NgxTiptapDirective', () => {
    let fixture: ComponentFixture<TestComponent>;

    beforeEach(async () => {
        TestBed.configureTestingModule({
            declarations: [TestComponent, EditorDirective],
            teardown: { destroyAfterEach: false }
        });

        await TestBed.compileComponents();

        fixture = TestBed.createComponent(TestComponent);

        const editor = new Editor({
            extensions: [StarterKit]
        });

        fixture.componentRef.setInput('editor', editor);
        fixture.detectChanges();
    });

    it('should create an instance', () => {
        // Resolved through the injector rather than constructed: the directive takes its
        // ElementRef and Renderer2 via inject(), so it has no constructor parameters.
        const directiveEl = fixture.debugElement.query(By.directive(EditorDirective));

        expect(directiveEl.injector.get(EditorDirective)).toBeTruthy();
    });
});

@Component({
    template: '<div tiptap [editor]="editor" [(ngModel)]="value"></div>'
})
class TestFormComponent {
    @Input() editor!: Editor;
    value = 'Default Text';
}

describe('NgxTiptapDirective FormsModule', () => {
    let component: TestFormComponent;
    let fixture: ComponentFixture<TestFormComponent>;
    let directiveEl: DebugElement;
    let directiveInstance: EditorDirective;

    beforeEach(async () => {
        TestBed.configureTestingModule({
            declarations: [TestFormComponent, EditorDirective],
            imports: [FormsModule],
            teardown: { destroyAfterEach: false }
        });

        await TestBed.compileComponents();

        fixture = TestBed.createComponent(TestFormComponent);
        component = fixture.componentInstance;

        const editor = new Editor({
            extensions: [StarterKit]
        });

        fixture.componentRef.setInput('editor', editor);

        directiveEl = fixture.debugElement.query(By.directive(EditorDirective));
        directiveInstance = directiveEl.injector.get(EditorDirective);

        fixture.detectChanges();
    });

    it('should create an instance', () => {
        expect(directiveInstance).toBeTruthy();
    });

    it('should attach the editor to the div', () => {
        expect(directiveEl).not.toBeNull();
        expect(fixture.debugElement.query(By.css('.ProseMirror'))).toBeTruthy();
    });

    it('should bind to the model correctly', async () => {
        directiveInstance.writeValue('Hi.');
        await fixture.whenStable();
        fixture.detectChanges();

        expect(component.value).toContain('Hi.');
    });

    it('should the model when editor is directly updated', () => {
        component.editor.chain().setContent('Hello World!').run();
        fixture.detectChanges();
        expect(component.value).toContain('Hello World!');
    });

    it('should disable the editor correctly', async () => {
        directiveInstance.setDisabledState(true);
        await fixture.whenStable();
        fixture.detectChanges();

        expect(directiveEl.query(By.css('.ProseMirror[contenteditable=false]'))).toBeTruthy();
    });
});

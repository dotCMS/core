import { Component, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';

import { BubbleMenuDirective } from './bubble-menu.directive';
import { EditorDirective } from './editor.directive';

@Component({
  template: `
    <tiptap-editor [editor]="editor"></tiptap-editor>
    <tiptap-bubble-menu [editor]="editor"></tiptap-bubble-menu>
  `
})
class TestComponent {
  @Input() editor!: Editor
}

describe('BubbleMenuDirective', () => {
  let component: TestComponent;
  let fixture: ComponentFixture<TestComponent>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      declarations: [
        TestComponent,
        EditorDirective,
        BubbleMenuDirective
      ]
    });

    await TestBed.compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    component = fixture.componentInstance;

    const editor = new Editor({
      extensions: [StarterKit]
    });

    component.editor = editor;
    fixture.detectChanges();
  });

  it('should create an instance', () => {
    const hostEl = fixture.debugElement.query(By.css('tiptap-bubble-menu'));
    const directive = new BubbleMenuDirective(hostEl);
    expect(directive).toBeTruthy();
  });

  it('should create bubble menu', () => {
    expect(fixture.debugElement.query(By.css('[data-tippy-root]'))).toBeFalsy();

    component.editor.chain().setContent('Hello world').focus().selectAll().run();
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('[data-tippy-root]'))).toBeTruthy();
  });
});

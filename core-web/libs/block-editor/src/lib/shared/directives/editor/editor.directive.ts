import { Transaction } from 'prosemirror-state';

import {
    Directive,
    ElementRef,
    forwardRef,
    Input,
    OnDestroy,
    OnInit,
    Renderer2
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { Content, Editor, JSONContent } from '@tiptap/core';

@Directive({
    // eslint-disable-next-line @angular-eslint/directive-selector
    selector: 'tiptap[editor], [tiptap][editor], tiptap-editor[editor], [tiptapEditor][editor]',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => EditorDirective),
            multi: true
        }
    ],
    standalone: false
})
export class EditorDirective implements OnInit, ControlValueAccessor, OnDestroy {
    @Input() editor!: Editor;

    constructor(
        private el: ElementRef<HTMLElement>,
        private _renderer: Renderer2
    ) {}

    private onChange: (value: Content) => void = () => {
        /** */
    };
    private onTouched: () => void = () => {
        /** */
    };

    // Writes a new value to the element.
    // This methods is called when programmatic changes from model to view are requested.
    writeValue(value: Content): void {
        if (!value) {
            return;
        }

        this.editor.chain().setContent(value, true).run();
    }

    // Registers a callback function that is called when the control's value changes in the UI.
    registerOnChange(fn: () => void): void {
        this.onChange = fn;
    }

    // Registers a callback function that is called by the forms API on initialization to update the form model on blur.
    registerOnTouched(fn: () => void): void {
        this.onTouched = fn;
    }

    // Called by the forms api to enable or disable the element
    setDisabledState(isDisabled: boolean): void {
        this.editor.setEditable(!isDisabled);
        this._renderer.setProperty(this.el.nativeElement, 'disabled', isDisabled);
    }

    private handleChange = ({ transaction }: { transaction: Transaction }): void => {
        if (!transaction.docChanged) {
            return;
        }

        this.onChange(this.editor.getJSON() as JSONContent);
    };

    ngOnInit(): void {
        if (!this.editor) {
            throw new Error('Required: Input `editor`');
        }

        // take the inner contents and clear the block
        const innerHTML = this.el.nativeElement.innerHTML;
        this.el.nativeElement.innerHTML = '';

        // insert the editor in the dom
        this.el.nativeElement.appendChild(this.editor.options.element.firstChild as ChildNode);

        // update the options for the editor
        this.editor.setOptions({ element: this.el.nativeElement });

        // update content to the editor
        if (innerHTML) {
            this.editor.chain().setContent(innerHTML, false).run();
        }

        // register blur handler to update `touched` property
        this.editor.on('blur', () => {
            this.onTouched();
        });

        // register transaction handler to emit changes on update
        this.editor.on('transaction', this.handleChange);
    }

    ngOnDestroy(): void {
        this.editor.destroy();
    }
}

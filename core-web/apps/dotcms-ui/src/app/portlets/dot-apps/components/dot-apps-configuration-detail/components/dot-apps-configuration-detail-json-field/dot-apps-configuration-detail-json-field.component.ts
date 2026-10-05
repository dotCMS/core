import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    forwardRef,
    inject,
    input,
    signal
} from '@angular/core';
import {
    AbstractControl,
    ControlValueAccessor,
    NG_VALIDATORS,
    NG_VALUE_ACCESSOR,
    ValidationErrors,
    Validator
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DOT_MONACO_BASE_OPTIONS, DotMessagePipe } from '@dotcms/ui';

/** The part of the Monaco editor instance this field uses. */
interface JsonEditor {
    getValue(): string;
    setValue(value: string): void;
    onDidChangeModelContent(listener: () => void): { dispose(): void };
}

/** Spaces used to indent the JSON when the user formats it. */
const JSON_INDENT = 4;

/**
 * Returns the parse error message for a JSON string, or `null` when it is valid.
 * Empty values are considered valid here: the `required` validator owns that case.
 *
 * @param value the raw text typed by the user
 */
export function getJsonParseError(value: string | null | undefined): string | null {
    if (!value || !value.trim()) {
        return null;
    }

    try {
        JSON.parse(value);

        return null;
    } catch (error) {
        return error instanceof Error ? error.message : String(error);
    }
}

/**
 * Form control for App params of type `JSON`. It renders a Monaco editor in JSON mode,
 * so the user gets syntax highlighting, bracket matching and inline error markers,
 * and it marks the control invalid while the text can't be parsed as JSON.
 */
@Component({
    selector: 'dot-apps-configuration-detail-json-field',
    imports: [MonacoEditorModule, ButtonModule, DotMessagePipe],
    templateUrl: './dot-apps-configuration-detail-json-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotAppsConfigurationDetailJsonFieldComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => DotAppsConfigurationDetailJsonFieldComponent),
            multi: true
        }
    ]
})
export class DotAppsConfigurationDetailJsonFieldComponent
    implements ControlValueAccessor, Validator
{
    /** Id given to the editor wrapper, so the field label can point at it. */
    $fieldId = input.required<string>({ alias: 'fieldId' });

    /** Accessible name of the editor; a `<label for>` can't name the Monaco editor's `<div>`. */
    $label = input<string>('', { alias: 'label' });

    readonly $value = signal<string>('');
    readonly $isDisabled = signal<boolean>(false);
    readonly $parseError = computed(() => getJsonParseError(this.$value()));

    readonly $editorOptions = computed(() => ({
        ...DOT_MONACO_BASE_OPTIONS,
        language: 'json',
        tabSize: JSON_INDENT,
        formatOnPaste: true,
        wordWrap: 'on',
        readOnly: this.$isDisabled(),
        ariaLabel: this.$label()
    }));

    /** Set once Monaco has created the editor. */
    readonly #editor = signal<JsonEditor | null>(null);
    #contentListener?: { dispose(): void };

    constructor() {
        // The editor value is synced by hand instead of with ngModel: when Monaco is already
        // loaded, the editor is created before ngModel writes its first value, and that stale
        // empty write came back as a content change that wiped the field.
        effect(() => {
            const editor = this.#editor();
            const value = this.$value();
            if (editor && editor.getValue() !== value) {
                editor.setValue(value);
            }
        });

        inject(DestroyRef).onDestroy(() => this.#contentListener?.dispose());
    }

    #onChange = (_value: string) => {
        // Replaced by registerOnChange
    };

    #onTouched = () => {
        // Replaced by registerOnTouched
    };

    writeValue(value: string): void {
        this.$value.set(value || '');
    }

    registerOnChange(fn: (value: string) => void): void {
        this.#onChange = fn;
    }

    registerOnTouched(fn: () => void): void {
        this.#onTouched = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        this.$isDisabled.set(isDisabled);
    }

    /**
     * Marks the control invalid while its value isn't parseable JSON.
     *
     * @param control the form control bound to this component
     */
    validate(control: AbstractControl): ValidationErrors | null {
        const error = getJsonParseError(control.value);

        return error ? { invalidJson: { message: error } } : null;
    }

    /**
     * Takes the editor instance once Monaco has created it, loads the current value into it
     * and starts listening for edits.
     *
     * @param editor the Monaco editor instance
     */
    protected onEditorInit(editor: JsonEditor): void {
        editor.setValue(this.$value());
        this.#contentListener = editor.onDidChangeModelContent(() =>
            this.onEditorChange(editor.getValue())
        );
        this.#editor.set(editor);
    }

    /**
     * Pushes the text typed in the editor to the form.
     *
     * @param value the current editor content
     */
    protected onEditorChange(value: string): void {
        if (value === this.$value()) {
            // Our own setValue echoing back, not a user edit.
            return;
        }

        this.$value.set(value);
        this.#onChange(value);
        this.#onTouched();
    }

    /** Re-indents the current JSON. Does nothing while the JSON is invalid. */
    protected format(): void {
        if (this.$parseError() || !this.$value().trim()) {
            return;
        }

        const formatted = JSON.stringify(JSON.parse(this.$value()), null, JSON_INDENT);
        this.onEditorChange(formatted);
    }
}

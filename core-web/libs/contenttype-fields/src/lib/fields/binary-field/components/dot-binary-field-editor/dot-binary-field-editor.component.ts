import {
    MonacoEditorComponent,
    MonacoEditorConstructionOptions,
    MonacoEditorModule
} from '@materia-ui/ngx-monaco-editor';
import { from, of } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    OnInit,
    Output,
    ViewChild,
    inject
} from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { debounceTime, switchMap, tap } from 'rxjs/operators';

import { DotUploadService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

const EDITOR_CONFIG: MonacoEditorConstructionOptions = {
    theme: 'vs',
    minimap: {
        enabled: false
    },
    cursorBlinking: 'solid',
    overviewRulerBorder: false,
    mouseWheelZoom: false,
    lineNumbers: 'on',
    roundedSelection: false,
    automaticLayout: true
};

@Component({
    selector: 'dot-dot-binary-field-editor',
    standalone: true,
    imports: [
        CommonModule,
        MonacoEditorModule,
        FormsModule,
        ReactiveFormsModule,
        InputTextModule,
        ButtonModule,
        DotMessagePipe
    ],
    templateUrl: './dot-binary-field-editor.component.html',
    styleUrls: ['./dot-binary-field-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryFieldEditorComponent implements OnInit, AfterViewInit {
    @Output() readonly tempFile = new EventEmitter<DotCMSTempFile>();

    @ViewChild('editorRef', { static: true }) editorRef!: MonacoEditorComponent;

    private readonly cd: ChangeDetectorRef = inject(ChangeDetectorRef);
    private readonly dotUploadService: DotUploadService = inject(DotUploadService);

    private editor: monaco.editor.IStandaloneCodeEditor;
    readonly form = new FormGroup({
        name: new FormControl(''),
        content: new FormControl('')
    });

    editorOptions = EDITOR_CONFIG;
    mimeType = 'plain/text';

    get name(): FormControl {
        return this.form.get('name') as FormControl;
    }

    get content(): FormControl {
        return this.form.get('content') as FormControl;
    }

    ngOnInit(): void {
        this.name.valueChanges.pipe(debounceTime(500)).subscribe((name) => {
            this.setEditorLanguage(name);
        });
    }

    ngAfterViewInit(): void {
        this.editor = this.editorRef.editor;
    }

    onSubmit(): void {
        const file = new File([this.content.value], this.name.value, {
            type: this.mimeType
        });
        this.uploadFile(file);
    }

    private uploadFile(file: File) {
        const obs$ = from(this.dotUploadService.uploadFile({ file }));

        const loading$ = of(null).pipe(
            tap(() => this.disableEditor()),
            switchMap(() => obs$)
        );

        loading$.subscribe((tempFile) => {
            this.enableEditor();
            this.tempFile.emit(tempFile);
        });
    }

    private setEditorLanguage(fileName: string = '') {
        const fileExtension = fileName?.split('.').pop();
        const { id, mimetypes } = this.getLanguage(fileExtension) || {};
        this.mimeType = mimetypes?.[0] || 'plain/text';

        this.updateEditorLanguage(id);
        this.cd.detectChanges();
    }

    private getLanguage(fileExtension: string) {
        return monaco.languages
            .getLanguages()
            .find((language) => language.extensions?.includes(`.${fileExtension}`));
    }

    private updateEditorLanguage(languageId: string = 'text') {
        this.editorOptions = {
            ...this.editorOptions,
            language: languageId
        };
    }

    private disableEditor() {
        this.form.disable();
        this.editor.updateOptions({
            readOnly: true
        });
    }

    private enableEditor() {
        this.form.enable();
        this.editor.updateOptions({
            readOnly: false
        });
    }
}

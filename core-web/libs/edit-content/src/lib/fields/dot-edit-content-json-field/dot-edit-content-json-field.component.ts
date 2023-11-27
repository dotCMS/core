import { MonacoEditorConstructionOptions, MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnDestroy,
    OnInit,
    inject
} from '@angular/core';
import { ControlContainer, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { takeUntil } from 'rxjs/operators';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-edit-content-json-field',
    standalone: true,
    imports: [FormsModule, ReactiveFormsModule, MonacoEditorModule],
    templateUrl: './dot-edit-content-json-field.component.html',
    styleUrls: ['./dot-edit-content-json-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentJsonFieldComponent implements OnInit, OnDestroy {
    @Input() field!: DotCMSContentTypeField;

    private readonly cd = inject(ChangeDetectorRef);
    private readonly controlContainer = inject(ControlContainer);
    private readonly destroy$: Subject<boolean> = new Subject<boolean>();

    public readonly editorOptions: MonacoEditorConstructionOptions = {
        theme: 'vs',
        minimap: {
            enabled: false
        },
        cursorBlinking: 'solid',
        overviewRulerBorder: false,
        mouseWheelZoom: false,
        lineNumbers: 'on',
        roundedSelection: false,
        automaticLayout: true,
        language: 'json'
    };

    ngOnInit(): void {
        const form = this.controlContainer.control;
        const control = form.get(this.field.variable);

        /*
         * This is a workaround to force the change detection to run when the value of the control changes.
         * This is needed because the Monaco Editor does not play well with the change detection strategy of the component.
         */
        control.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => this.cd.markForCheck());
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}

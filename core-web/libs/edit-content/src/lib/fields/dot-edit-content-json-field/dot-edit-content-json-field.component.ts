import { MonacoEditorConstructionOptions, MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { Subject } from 'rxjs';

import { JsonPipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    computed,
    inject,
    Input,
    OnDestroy,
    OnInit,
    signal,
    Signal
} from '@angular/core';
import { ControlContainer, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { takeUntil } from 'rxjs/operators';

import { DotCMSContentTypeField, DotCMSContentTypeFieldVariable } from '@dotcms/dotcms-models';

import { DEFAULT_MONACO_CONFIG } from '../../models/dot-edit-content-field.constant';
import { getFieldVariablesParsed, stringToJson } from '../../utils/functions.util';

export const DEFAULT_JSON_FIELD_EDITOR_CONFIG: MonacoEditorConstructionOptions = {
    ...DEFAULT_MONACO_CONFIG,
    language: 'json'
};

@Component({
    selector: 'dot-edit-content-json-field',
    standalone: true,
    imports: [FormsModule, ReactiveFormsModule, MonacoEditorModule, JsonPipe],
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
    contentTypeField = signal<DotCMSContentTypeField>({} as DotCMSContentTypeField);
    // Monaco options
    monacoEditorOptions: Signal<MonacoEditorConstructionOptions> = computed(() => {
        return {
            ...DEFAULT_JSON_FIELD_EDITOR_CONFIG,
            ...this.parseCustomMonacoOptions(this.contentTypeField().fieldVariables)
        };
    });
    private readonly cd = inject(ChangeDetectorRef);
    private readonly controlContainer = inject(ControlContainer);
    private readonly destroy$: Subject<boolean> = new Subject<boolean>();

    @Input({ required: true })
    set field(contentTypeField: DotCMSContentTypeField) {
        this.contentTypeField.set(contentTypeField);
    }

    ngOnInit(): void {
        const form = this.controlContainer.control;
        const control = form.get(this.contentTypeField().variable);

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

    /**
     * Parses the custom Monaco options for a given field of a DotCMSContentTypeField.
     *
     * @returns {Record<string, string>} Returns the parsed custom Monaco options as a key-value pair object.
     * @private
     * @param fieldVariables
     */
    private parseCustomMonacoOptions(
        fieldVariables: DotCMSContentTypeFieldVariable[]
    ): Record<string, string> {
        const { monacoOptions } = getFieldVariablesParsed<{ monacoOptions: string }>(
            fieldVariables
        );

        return stringToJson(monacoOptions);
    }
}

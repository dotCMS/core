import { Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    inject
} from '@angular/core';
import { FormsModule, UntypedFormControl, UntypedFormGroup } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { takeUntil } from 'rxjs/operators';

import { DotMessagePipe, DotThemeComponent } from '@dotcms/ui';

import { DotTemplateLayoutProperties } from '../../models/models';
import { DotTemplateBuilderStore } from '../../store/template-builder.store';
import { DotLayoutPropertiesComponent } from '../dot-layout-properties/dot-layout-properties.component';

@Component({
    selector: 'dotcms-template-builder-actions',
    host: {
        class: 'flex gap-2'
    },
    imports: [
        ButtonModule,
        FormsModule,
        DotLayoutPropertiesComponent,
        DotMessagePipe,
        DotThemeComponent
    ],
    templateUrl: './template-builder-actions.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TemplateBuilderActionsComponent implements OnInit, OnDestroy {
    private store = inject(DotTemplateBuilderStore);

    @Input() set layoutProperties(layoutProperties: DotTemplateLayoutProperties) {
        this.group?.patchValue(
            {
                ...layoutProperties
            },
            { emitEvent: false }
        );

        this._layoutProperties = { ...layoutProperties };
    }

    @Input() set themeId(themeId: string | null) {
        this.selectedThemeId = themeId;
    }

    selectedThemeId: string | null = null;

    private _layoutProperties: DotTemplateLayoutProperties;

    @Output() selectTheme: EventEmitter<string> = new EventEmitter();

    @Output() layoutPropertiesChange: EventEmitter<DotTemplateLayoutProperties> =
        new EventEmitter();

    group: UntypedFormGroup;

    destroy$: Subject<boolean> = new Subject<boolean>();

    onThemeChange(themeId: string | null): void {
        this.selectedThemeId = themeId;
        // Parent expects `string` (see TemplateBuilderComponent.updateTheme), so ignore clears.
        if (themeId) {
            this.selectTheme.emit(themeId);
        }
    }

    ngOnInit(): void {
        this.group = new UntypedFormGroup({
            header: new UntypedFormControl(this._layoutProperties.header ?? true),
            footer: new UntypedFormControl(this._layoutProperties.footer ?? true),
            sidebar: new UntypedFormControl(
                this._layoutProperties.sidebar ?? {
                    location: '',
                    width: 'medium',
                    containers: []
                }
            )
        });

        this.group.valueChanges.pipe(takeUntil(this.destroy$)).subscribe((value) => {
            this.store.updateLayoutProperties(value);
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}

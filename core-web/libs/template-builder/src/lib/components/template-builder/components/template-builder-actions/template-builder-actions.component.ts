import { Subject } from 'rxjs';

import { CommonModule, NgFor } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output
} from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipeModule } from '@dotcms/ui';

import { DotTemplateLayoutProperties } from '../../models/models';
import { DotLayoutPropertiesModule } from '../dot-layout-properties/dot-layout-properties.module';

@Component({
    selector: 'dotcms-template-builder-actions',
    standalone: true,
    imports: [CommonModule, ButtonModule, NgFor, DotLayoutPropertiesModule, DotMessagePipeModule],
    templateUrl: './template-builder-actions.component.html',
    styleUrls: ['./template-builder-actions.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TemplateBuilderActionsComponent implements OnInit, OnDestroy {
    @Input() layoutProperties: DotTemplateLayoutProperties;

    @Output() selectTheme: EventEmitter<void> = new EventEmitter();

    @Output() layoutPropertiesChange: EventEmitter<DotTemplateLayoutProperties> =
        new EventEmitter();

    group: UntypedFormGroup;

    destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnInit(): void {
        this.group = new UntypedFormGroup({
            header: new UntypedFormControl(this.layoutProperties.header ?? true),
            footer: new UntypedFormControl(this.layoutProperties.footer ?? true),
            sidebar: new UntypedFormControl(
                this.layoutProperties.sidebar ?? {
                    location: ''
                }
            )
        });

        this.group.valueChanges.subscribe((value) => {
            this.layoutPropertiesChange.emit(value);
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}

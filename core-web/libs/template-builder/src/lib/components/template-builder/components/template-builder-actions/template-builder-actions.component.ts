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

import { takeUntil } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { DotTemplateLayoutProperties } from '../../models/models';
import { DotTemplateBuilderStore } from '../../store/template-builder.store';
import { DotLayoutPropertiesModule } from '../dot-layout-properties/dot-layout-properties.module';

@Component({
    selector: 'dotcms-template-builder-actions',
    standalone: true,
    imports: [CommonModule, ButtonModule, NgFor, DotLayoutPropertiesModule, DotMessagePipe],
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

    constructor(private store: DotTemplateBuilderStore) {}

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

        this.group.valueChanges.pipe(takeUntil(this.destroy$)).subscribe((value) => {
            this.store.updateLayoutProperties(value);
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}

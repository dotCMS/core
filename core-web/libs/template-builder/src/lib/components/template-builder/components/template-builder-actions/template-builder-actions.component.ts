import { Subject } from 'rxjs';

import { CommonModule, NgFor } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    OnDestroy,
    OnInit,
    Output
} from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipeModule } from '@dotcms/ui';

import { SidebarPosition } from '../../models/models';
import { DotLayoutPropertiesModule } from '../dot-layout-properties/dot-layout-properties.module';

interface TemplateData {
    header: boolean;
    footer: boolean;
    sidebarPosition?: SidebarPosition;
}

@Component({
    selector: 'dotcms-template-builder-actions',
    standalone: true,
    imports: [CommonModule, ButtonModule, NgFor, DotLayoutPropertiesModule, DotMessagePipeModule],
    templateUrl: './template-builder-actions.component.html',
    styleUrls: ['./template-builder-actions.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TemplateBuilderActionsComponent implements OnInit, OnDestroy {
    @Output() selectTheme: EventEmitter<void> = new EventEmitter();

    @Output() valueChanges: EventEmitter<TemplateData> = new EventEmitter();

    group: UntypedFormGroup;

    destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnInit(): void {
        this.group = new UntypedFormGroup({
            header: new UntypedFormControl(true),
            footer: new UntypedFormControl(true),
            sidebar: new UntypedFormControl({
                location: 'left',
                containers: [],
                width: 'small'
            })
        });

        this.group.valueChanges.subscribe((value) => {
            this.valueChanges.emit(value);
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}

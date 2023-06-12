import { CommonModule, NgFor } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, OnInit, Output } from '@angular/core';
import {
    FormBuilder,
    FormControl,
    FormGroup,
    FormsModule,
    ReactiveFormsModule
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { RadioButtonModule } from 'primeng/radiobutton';

import { map } from 'rxjs/operators';

import { DotMessagePipeModule } from '@dotcms/ui';

import { SidebarPosition } from '../../models/models';

interface ActionsForm {
    header: FormControl<Addon[]>;
    footer: FormControl<Addon[]>;
    sidebarPosition?: FormControl<SidebarPosition>;
}

interface TemplateData {
    header: boolean;
    footer: boolean;
    sidebarPosition?: SidebarPosition;
}

interface Addon {
    label: string;
    name: string;
}

@Component({
    selector: 'dotcms-template-builder-actions',
    standalone: true,
    imports: [
        CommonModule,
        ButtonModule,
        DotMessagePipeModule,
        OverlayPanelModule,
        RadioButtonModule,
        CheckboxModule,
        FormsModule,
        ReactiveFormsModule,
        NgFor
    ],
    templateUrl: './template-builder-actions.component.html',
    styleUrls: ['./template-builder-actions.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TemplateBuilderActionsComponent implements OnInit {
    @Output() selectTheme: EventEmitter<void> = new EventEmitter();

    @Output() valueChanges: EventEmitter<TemplateData> = new EventEmitter();

    private lastPosition: SidebarPosition | null = null;

    readonly addons = [
        { label: 'Header', name: 'header' },
        { label: 'Footer', name: 'footer' }
    ];

    readonly posibleSidebarPositions = [SidebarPosition.left, SidebarPosition.right];

    formGroup: FormGroup<ActionsForm>;

    ngOnInit(): void {
        this.formGroup = new FormBuilder().group({
            header: [],
            footer: [],
            sidebarPosition: undefined
        }) as FormGroup<ActionsForm>;

        // This is not working well I need to find a way to toggle off the sidebar
        this.formGroup.valueChanges
            .pipe(
                map((value) => ({
                    header: !!value.header?.length,
                    footer: !!value.footer?.length,
                    sidebarPosition: value.sidebarPosition
                }))
            )
            .subscribe((value) => {
                // If it's the same we toggle off the sidebar
                if (this.lastPosition === value.sidebarPosition) {
                    this.formGroup.patchValue(
                        {
                            sidebarPosition: null
                        },
                        { emitEvent: false }
                    );
                    this.lastPosition = undefined;
                } else {
                    this.valueChanges.emit(value);
                    this.lastPosition = value.sidebarPosition;
                }
            });
    }
}

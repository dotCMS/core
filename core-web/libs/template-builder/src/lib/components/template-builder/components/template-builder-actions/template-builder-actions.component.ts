import { Observable, merge } from 'rxjs';

import { CommonModule, NgFor } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { RadioButtonModule } from 'primeng/radiobutton';

import { map } from 'rxjs/operators';

import { DotMessagePipeModule } from '@dotcms/ui';

import { SidebarPosition } from '../../models/models';

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

    selectedAddons: FormControl<Addon[]>;
    sidebarPosition: FormControl<SidebarPosition>;

    addons$: Observable<Record<string, boolean>>;
    sidebarPosition$: Observable<SidebarPosition>;

    ngOnInit(): void {
        this.selectedAddons = new FormControl<Addon[]>([]);

        this.sidebarPosition = new FormControl();

        this.addons$ = this.selectedAddons.valueChanges.pipe(
            map((value) => ({
                header: !!value.find((addon) => addon.name === 'header'),
                footer: !!value.find((addon) => addon.name === 'footer')
            }))
        );

        this.sidebarPosition$ = this.sidebarPosition.valueChanges.pipe(
            map((position) => {
                if (this.lastPosition === position) {
                    this.sidebarPosition.patchValue(null, { emitEvent: false });
                    this.lastPosition = undefined;

                    return null;
                }

                this.lastPosition = position;

                return position;
            })
        );

        merge(this.addons$, this.sidebarPosition$).subscribe(() => {
            /** */
        });
    }
}

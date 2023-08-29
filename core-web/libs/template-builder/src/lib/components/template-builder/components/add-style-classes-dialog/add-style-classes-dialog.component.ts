import { AsyncPipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AutoComplete, AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotMessagePipe } from '@dotcms/ui';

import { JsonClassesService } from './services/json-classes.service';

@Component({
    selector: 'dotcms-add-style-classes-dialog',
    standalone: true,
    imports: [AutoCompleteModule, FormsModule, ButtonModule, DotMessagePipe, NgIf, AsyncPipe],
    templateUrl: './add-style-classes-dialog.component.html',
    styleUrls: ['./add-style-classes-dialog.component.scss'],
    providers: [JsonClassesService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AddStyleClassesDialogComponent implements OnInit {
    @ViewChild(AutoComplete) autoComplete: AutoComplete;
    suggestions: string[] = [];
    selectedClasses: string[] = [];

    constructor(
        private jsonClassesService: JsonClassesService,
        public dynamicDialogConfig: DynamicDialogConfig<{
            selectedClasses: string[];
        }>
    ) {}

    ngOnInit() {
        const { selectedClasses } = this.dynamicDialogConfig.data;
        this.selectedClasses = selectedClasses;

        this.jsonClassesService.getClasses().subscribe((res) => {
            this.suggestions = res as string[];
        });
    }

    save() {
        console.log(this.selectedClasses);
    }

    onKeyUp(event: KeyboardEvent) {
        const target: HTMLInputElement = event.target as unknown as HTMLInputElement;

        if (event.key === 'Enter' && !!target.value) {
            this.autoComplete.selectItem(target.value);
        }
    }
}

import { Observable, of } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AutoComplete, AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { catchError, map, shareReplay, tap } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { JsonClassesService } from './services/json-classes.service';

@Component({
    selector: 'dotcms-add-style-classes-dialog',
    standalone: true,
    imports: [AutoCompleteModule, FormsModule, ButtonModule, DotMessagePipe, AsyncPipe],
    templateUrl: './add-style-classes-dialog.component.html',
    styleUrls: ['./add-style-classes-dialog.component.scss'],
    providers: [JsonClassesService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AddStyleClassesDialogComponent implements OnInit {
    @ViewChild(AutoComplete) autoComplete: AutoComplete;
    filteredSuggestions = null;
    selectedClasses: string[] = [];

    isJsonClasses$: Observable<boolean>;
    classes: string[] = [];

    constructor(
        private jsonClassesService: JsonClassesService,
        public dynamicDialogConfig: DynamicDialogConfig<{
            selectedClasses: string[];
        }>,
        private ref: DynamicDialogRef
    ) {}

    ngOnInit() {
        const { selectedClasses } = this.dynamicDialogConfig.data;
        this.selectedClasses = selectedClasses;

        this.isJsonClasses$ = this.jsonClassesService.getClasses().pipe(
            tap(({ classes }) => {
                if (classes?.length) {
                    this.classes = classes;
                } else {
                    this.classes = [];
                }
            }),
            map(({ classes }) => {
                return !!classes?.length;
            }),
            catchError(() => {
                this.classes = [];

                return of(false);
            }),
            shareReplay(1)
        );
    }

    /**
     * Filter the suggestions based on the query
     *
     * @param {{ query: string }} { query }
     * @return {*}
     * @memberof AddStyleClassesDialogComponent
     */
    filterClasses({ query }: { query: string }): void {
        /*
            https://github.com/primefaces/primeng/blob/master/src/app/components/autocomplete/autocomplete.ts#L739

            Sadly we need to pass suggestions all the time, even if they are empty because on the set is where the primeng remove the loading icon
        */

        // PrimeNG autocomplete doesn't support async pipe in the suggestions
        this.filteredSuggestions = this.classes.filter((item) => item.includes(query));
    }

    /**
     * Save the selected classes
     *
     * @memberof AddStyleClassesDialogComponent
     */
    save() {
        this.ref.close(this.selectedClasses);
    }
}

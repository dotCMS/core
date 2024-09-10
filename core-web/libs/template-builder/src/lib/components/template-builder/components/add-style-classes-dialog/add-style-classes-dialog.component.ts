import { of } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    OnInit,
    signal
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { catchError, map, shareReplay, tap } from 'rxjs/operators';

import { DotMessagePipe, DotSelectItemDirective } from '@dotcms/ui';

import { JsonClassesService } from './services/json-classes.service';

@Component({
    selector: 'dotcms-add-style-classes-dialog',
    standalone: true,
    imports: [
        AutoCompleteModule,
        FormsModule,
        ButtonModule,
        DotMessagePipe,
        DotSelectItemDirective
    ],
    templateUrl: './add-style-classes-dialog.component.html',
    styleUrls: ['./add-style-classes-dialog.component.scss'],
    providers: [JsonClassesService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AddStyleClassesDialogComponent implements OnInit {
    $selectedClasses = signal<string[]>([]);
    $classes = signal<string[]>([]);
    $query = signal<string>('');

    $filteredSuggestions = computed(() => {
        const classes = this.$classes();
        const query = this.$query();

        return classes.filter((item) => item.includes(query));
    });

    readonly #jsonClassesService = inject(JsonClassesService);
    isJsonClasses$ = this.#jsonClassesService.getClasses().pipe(
        tap(({ classes }) => {
            if (classes?.length) {
                this.$classes.set(classes);
            } else {
                this.$classes.set([]);
            }
        }),
        map(({ classes }) => !!classes?.length),
        catchError(() => {
            this.$classes.set([]);

            return of(false);
        }),
        shareReplay(1)
    );
    $isJsonClasses = toSignal(this.isJsonClasses$, {
        initialValue: false
    });

    constructor(
        public dynamicDialogConfig: DynamicDialogConfig<{
            selectedClasses: string[];
        }>,
        private ref: DynamicDialogRef
    ) {}

    ngOnInit() {
        const { selectedClasses } = this.dynamicDialogConfig.data;
        this.$selectedClasses.set(selectedClasses);
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
        this.$query.set(query);
    }

    /**
     * Save the selected classes
     *
     * @memberof AddStyleClassesDialogComponent
     */
    save() {
        this.ref.close(this.$selectedClasses());
    }
}

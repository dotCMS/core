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

import { AutoCompleteCompleteEvent, AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessagePipe } from '@dotcms/ui';

import { JsonClassesService } from './services/json-classes.service';

const UNIQUE_CLASSES = true;

@Component({
    selector: 'dotcms-add-style-classes-dialog',
    imports: [AutoCompleteModule, FormsModule, ButtonModule, DotMessagePipe],
    templateUrl: './add-style-classes-dialog.component.html',
    styleUrls: ['./add-style-classes-dialog.component.css'],
    providers: [JsonClassesService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AddStyleClassesDialogComponent implements OnInit {
    /**
     * Service to get the classes
     *
     * @memberof AddStyleClassesDialogComponent
     */
    readonly #jsonClassesService = inject(JsonClassesService);
    /**
     * Dialog reference
     *
     * @memberof AddStyleClassesDialogComponent
     */
    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #dynamicDialogConfig = inject(DynamicDialogConfig<{ selectedClasses: string[] }>);
    /**
     * Selected classes to be added
     * @memberof AddStyleClassesDialogComponent
     */
    $selectedClasses = signal<string[]>([]);
    /**
     * Check if the JSON file has classes
     *
     * @memberof AddStyleClassesDialogComponent
     */
    $classes = toSignal(this.#jsonClassesService.getClasses(), {
        initialValue: []
    });
    /**
     * Filtered suggestions based on the query
     *
     * @memberof AddStyleClassesDialogComponent
     */
    $filteredSuggestions = signal<string[]>(this.$classes());

    /**
     * Check if the JSON file has classes
     *
     * @memberof AddStyleClassesDialogComponent
     */
    $hasClasses = computed(() => this.$classes().length > 0);

    protected readonly UNIQUE_CLASSES = UNIQUE_CLASSES;

    /**
     * Set the selected classes
     *
     * @memberof AddStyleClassesDialogComponent
     */
    ngOnInit() {
        this.$selectedClasses.set(this.#dynamicDialogConfig?.data?.selectedClasses || []);
    }

    /**
     * Filter the suggestions based on the query
     *
     * @param {{ query: string }} { query }
     * @return {*}
     * @memberof AddStyleClassesDialogComponent
     */
    filterClasses({ query }: AutoCompleteCompleteEvent): void {
        /*
            https://github.com/primefaces/primeng/blob/master/src/app/components/autocomplete/autocomplete.ts#L541
            Sadly we need to pass suggestions all the time, even if they are empty because on the set is where the primeng remove the loading icon
        */
        const classes = this.$classes();
        const filteredClasses = query ? classes.filter((item) => item.includes(query)) : classes;
        this.$filteredSuggestions.set([...filteredClasses]);
    }

    /**
     * Save the selected classes
     *
     * @memberof AddStyleClassesDialogComponent
     */
    save() {
        this.#dialogRef.close(this.$selectedClasses());
    }

    /**
     * Add the current input value to selected classes when Enter is pressed
     *
     * @param {Event} event
     * @memberof AddStyleClassesDialogComponent
     */
    onEnterKey(event: Event): void {
        const input = event.target as HTMLInputElement;
        const value = input.value.trim();

        if (value) {
            const currentClasses = this.$selectedClasses();
            if (!UNIQUE_CLASSES || !currentClasses.includes(value)) {
                this.$selectedClasses.update((classes) => [...classes, value]);
            }

            input.value = '';
        }
    }
}

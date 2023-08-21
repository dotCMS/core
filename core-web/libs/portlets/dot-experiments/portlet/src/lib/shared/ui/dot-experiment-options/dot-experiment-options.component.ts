import { animate, state, style, transition, trigger } from '@angular/animations';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ContentChildren,
    forwardRef,
    inject,
    Provider,
    QueryList
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { DotExperimentOptionsItemDirective } from './directives/dot-experiment-options-item.directive';

const SELECT_VALUE_ACCESSOR: Provider = {
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => DotExperimentOptionsComponent),
    multi: true
};

@Component({
    selector: 'dot-experiment-options',
    templateUrl: './dot-experiment-options.component.html',
    styleUrls: ['./dot-experiment-options.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [SELECT_VALUE_ACCESSOR],
    animations: [
        trigger('contentExpansion', [
            state('expanded', style({ height: '*', opacity: 1, visibility: 'visible' })),
            state('collapsed', style({ height: '0px', opacity: 0, visibility: 'hidden' })),
            transition('expanded <=> collapsed', animate('200ms cubic-bezier(.37,1.04,.68,.98)'))
        ])
    ]
})
export class DotExperimentOptionsComponent implements ControlValueAccessor {
    value: string;
    expanded = new Set<number>();
    @ContentChildren(forwardRef(() => DotExperimentOptionsItemDirective))
    itemList: QueryList<DotExperimentOptionsItemDirective>;
    private cdr: ChangeDetectorRef = inject(ChangeDetectorRef);

    onChange = (_: unknown) => {
        //
    };
    onTouched = (_: unknown) => {
        //
    };

    /**
     * Toggle the option selected
     * @param index
     * @memberOf DotExperimentOptionsComponent
     */
    toggleOption = (index: number) => {
        if (this.expanded.has(index)) {
            this.expanded.delete(index);
        } else {
            this.expanded.clear();
            this.expanded.add(index);
        }

        this.cdr.detectChanges();
    };

    /**
     * Set the value selected to the control
     * @param {string} value
     * @memberOf DotExperimentOptionsComponent
     */
    setOptionSelected(value: string) {
        this.value = value;
        this.onChange(this.value);
    }

    /**
     * Writes a new value to the element.
     * @param {string} value
     */
    writeValue(value: string): void {
        this.value = value;
    }

    /**
     * Registers a callback function that is called when the control's value changes in the UI.
     * @param onChangefn
     * @memberOf DotExperimentOptionsComponent
     */
    registerOnChange(onChangefn: never) {
        this.onChange = onChangefn;
    }

    /**
     * Registers a callback function that is called by the forms API on initialization to update the form model on blur.
     * @param onTouchedfn
     * @memberOf DotExperimentOptionsComponent
     */
    registerOnTouched(onTouchedfn: never) {
        this.onTouched = onTouchedfn;
    }
}

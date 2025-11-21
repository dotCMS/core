import { Observable, of } from 'rxjs';

import { ChangeDetectorRef, Directive, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { Select } from 'primeng/select';

import { catchError, debounceTime, map, switchMap } from 'rxjs/operators';

import { DotContainersService, DotMessageService } from '@dotcms/data-access';
import {
    DotContainer,
    DotDropdownGroupSelectOption,
    DotDropdownSelectOption
} from '@dotcms/dotcms-models';

const DEFAULT_LABEL_NAME_INDEX = 'label';
const DEFAULT_VALUE_NAME_INDEX = 'value';

/**
 * Directive to set an element's options from dotCMS's containers
 *
 * @export
 * @class DotContainerOptionsDirective
 */
@Directive({
    selector: 'p-select[dotContainerOptions]'
})
export class DotContainerOptionsDirective implements OnInit {
    private readonly primeDropdown = inject(Select, { optional: true, self: true });
    private readonly dotContainersService = inject(DotContainersService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly changeDetectorRef = inject(ChangeDetectorRef);

    private readonly control: Select;
    private readonly maxOptions = 10;
    private readonly loadErrorMessage: string;

    constructor() {
        this.control = this.primeDropdown;
        this.loadErrorMessage = this.dotMessageService.get(
            'dot.template.builder.box.containers.error'
        );

        if (this.control) {
            this.control.group = true;
            this.control.optionLabel = DEFAULT_LABEL_NAME_INDEX;
            this.control.optionValue = DEFAULT_VALUE_NAME_INDEX;
            this.control.optionDisabled = 'inactive';
            this.control.filterBy = 'value.friendlyName,value.title';

            this.control.onFilter
                .pipe(
                    takeUntilDestroyed(),
                    debounceTime(500),
                    switchMap((event: { filter: string }) => {
                        return this.fetchContainerOptions(event.filter);
                    })
                )
                .subscribe((options) => this.setOptions(options));
        } else {
            console.warn('ContainerOptionsDirective is for use with PrimeNg Dropdown');
        }
    }

    ngOnInit() {
        this.fetchContainerOptions().subscribe((options) => {
            this.control.options = this.control.options || options; // avoid overwriting if they were already set
        });
    }

    private fetchContainerOptions(
        filter = ''
    ): Observable<DotDropdownGroupSelectOption<DotContainer>[]> {
        return this.dotContainersService.getFiltered(filter, this.maxOptions, true).pipe(
            map((containerEntities) => {
                const options = containerEntities
                    .map((container) => ({
                        label: container.title,
                        value: container,
                        inactive: false
                    }))
                    .sort((a, b) => a.label.localeCompare(b.label));

                return this.getOptionsGroupedByHost(options);
            }),
            catchError(() => {
                return this.handleContainersLoadError();
            })
        );
    }

    private handleContainersLoadError() {
        // Note: disabled is an InputSignal (read-only) in PrimeNG 20, so we can't set it directly
        // The control should be disabled via template binding or component input
        // For now, we'll use the fallback approach
        (this.control as unknown as { disabled: boolean }).disabled = true;

        return of([]);
    }

    private setOptions(options: Array<DotDropdownGroupSelectOption<DotContainer>>) {
        this.control.options = [...options];
        this.changeDetectorRef.detectChanges();
    }

    /**
     * Group options by host
     *
     * @private
     * @param {DotDropdownSelectOption<DotContainer>[]} options
     * @return {*}
     * @memberof DotContainerOptionsDirective
     */
    private getOptionsGroupedByHost(options: DotDropdownSelectOption<DotContainer>[]) {
        const groupByHost = this.getContainerGroupedByHost(options);

        return Object.keys(groupByHost).map((key) => {
            return {
                label: key,
                items: groupByHost[key].items
            };
        });
    }

    /**
     * Group containers by host
     *
     * @private
     * @param {DotDropdownSelectOption<DotContainer>[]} options
     * @return {*}  {{
     *         [key: string]: { items: DotDropdownSelectOption<DotContainer>[] };
     *     }}
     * @memberof DotContainerOptionsDirective
     */
    private getContainerGroupedByHost(options: DotDropdownSelectOption<DotContainer>[]): {
        [key: string]: { items: DotDropdownSelectOption<DotContainer>[] };
    } {
        return options.reduce((acc, option) => {
            const { hostname } = option.value.parentPermissionable;

            if (!acc[hostname]) {
                acc[hostname] = { items: [] };
            }

            acc[hostname].items.push(option);

            return acc;
        }, {});
    }
}

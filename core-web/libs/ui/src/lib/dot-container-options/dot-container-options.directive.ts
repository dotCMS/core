import { Observable, of, Subject } from 'rxjs';

import { ChangeDetectorRef, Directive, OnDestroy, OnInit, Optional, Self } from '@angular/core';

import { Dropdown } from 'primeng/dropdown';

import { catchError, debounceTime, map, switchMap, takeUntil } from 'rxjs/operators';

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
    selector: 'p-dropdown[dotContainerOptions]',
    standalone: true
})
export class DotContainerOptionsDirective implements OnInit, OnDestroy {
    private readonly control: Dropdown;
    private readonly maxOptions = 10;
    private readonly loadErrorMessage: string;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        @Optional() @Self() private readonly primeDropdown: Dropdown,
        private readonly dotContainersService: DotContainersService,
        private readonly dotMessageService: DotMessageService,
        private readonly changeDetectorRef: ChangeDetectorRef
    ) {
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
        } else {
            console.warn('ContainerOptionsDirective is for use with PrimeNg Dropdown');
        }
    }

    ngOnInit() {
        this.fetchContainerOptions().subscribe((options) => {
            this.control.options = this.control.options || options; // avoid overwriting if they were already set
        });
        this.control.onFilter
            .pipe(
                takeUntil(this.destroy$),
                debounceTime(500),
                switchMap((event: { filter: string }) => {
                    return this.fetchContainerOptions(event.filter);
                })
            )
            .subscribe((options) => this.setOptions(options));
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
        this.control.disabled = true;

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

    ngOnDestroy() {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}

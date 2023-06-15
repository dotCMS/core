import { Observable, of, Subject } from 'rxjs';

import { ChangeDetectorRef, Directive, OnDestroy, OnInit, Optional, Self } from '@angular/core';

import { Dropdown } from 'primeng/dropdown';

import { catchError, debounceTime, map, switchMap, take, takeUntil } from 'rxjs/operators';

import { DotContainersService, DotMessageService } from '@dotcms/data-access';
import { DotContainer, DotDropdownSelectOption } from '@dotcms/dotcms-models';

const DEFAULT_LABEL_NAME_INDEX = 'label';
const DEFAULT_VALUE_NAME_INDEX = 'value';

/**
 * Directive to set an element's options from dotCMS's containers
 *
 * @export
 * @class ContainerOptionsDirective
 */
@Directive({
    selector: 'p-dropdown[dotcmsContainerOptions]',
    standalone: true
})
export class ContainerOptionsDirective implements OnInit, OnDestroy {
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
            this.control.optionLabel = DEFAULT_LABEL_NAME_INDEX;
            this.control.optionValue = DEFAULT_VALUE_NAME_INDEX;
            this.control.optionDisabled = 'inactive';
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
        filter: string = ''
    ): Observable<DotDropdownSelectOption<DotContainer>[]> {
        return this.dotContainersService.getFiltered(filter, this.maxOptions).pipe(
            take(1),
            map((containerEntities) => {
                return containerEntities.map((container) => ({
                    label: container.friendlyName,
                    value: container,
                    inactive: false
                }));
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

    private setOptions(options: Array<DotDropdownSelectOption<DotContainer>>) {
        this.control.options = [...options];
        this.changeDetectorRef.detectChanges();
    }

    ngOnDestroy() {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}

import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    EventEmitter,
    Output,
    inject,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { TableModule } from 'primeng/table';

import { DotContentTypeService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

const PER_PAGE = 40;

@Component({
    selector: 'dot-ema-form-selector',
    imports: [
        ButtonModule,
        DotMessagePipe,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        PaginatorModule,
        ReactiveFormsModule,
        TableModule
    ],
    templateUrl: './ema-form-selector.component.html',
    styleUrls: ['./ema-form-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [DotContentTypeService]
})
export class EmaFormSelectorComponent {
    @Output() selected = new EventEmitter<string>();

    private readonly contentTypesService = inject(DotContentTypeService);
    private readonly destroyRef = inject(DestroyRef);

    protected readonly perPage = PER_PAGE;
    protected readonly searchControl = new FormControl('', { nonNullable: true });
    protected readonly $forms = signal<DotCMSContentType[]>([]);
    protected readonly $totalRecords = signal(0);

    private currentPage = 1;
    private readonly fetch$ = new Subject<void>();

    constructor() {
        this.fetch$
            .pipe(
                switchMap(() =>
                    this.contentTypesService.getContentTypesWithPagination({
                        type: 'form',
                        filter: this.searchControl.value,
                        page: this.currentPage,
                        per_page: PER_PAGE
                    })
                ),
                takeUntilDestroyed(this.destroyRef)
            )
            .subscribe(({ contentTypes, pagination }) => {
                this.$forms.set(contentTypes);
                this.$totalRecords.set(pagination?.totalEntries ?? 0);
            });

        this.fetch$.next();

        this.searchControl.valueChanges
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
            .subscribe(() => {
                this.currentPage = 1;
                this.fetch$.next();
            });
    }

    protected onPageChange(event: PaginatorState): void {
        this.currentPage = (event.page ?? 0) + 1;
        this.fetch$.next();
    }
}

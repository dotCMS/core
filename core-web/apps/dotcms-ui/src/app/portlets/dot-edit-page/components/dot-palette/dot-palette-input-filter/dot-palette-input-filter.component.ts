import {
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';
import { debounceTime, takeUntil } from 'rxjs/operators';
import { fromEvent as observableFromEvent, Subject } from 'rxjs';

@Component({
    selector: 'dot-palette-input-filter',
    templateUrl: './dot-palette-input-filter.component.html',
    styleUrls: ['./dot-palette-input-filter.component.scss']
})
export class DotPaletteInputFilterComponent implements OnInit, OnDestroy {
    @Input() goBackBtn: boolean;
    @Input() value: string;
    @Output() goBack: EventEmitter<boolean> = new EventEmitter();
    @Output() filter: EventEmitter<string> = new EventEmitter();

    @ViewChild('searchInput', { static: true })
    searchInput: ElementRef;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnInit() {
        observableFromEvent(this.searchInput.nativeElement, 'keyup')
            .pipe(debounceTime(500), takeUntil(this.destroy$))
            .subscribe(() => {
                this.filter.emit(this.value);
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Focus on the search input
     *
     * @memberof DotPaletteInputFilterComponent
     */
    focus() {
        this.searchInput.nativeElement.focus();
    }
}

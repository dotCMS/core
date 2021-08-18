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
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { fromEvent as observableFromEvent, Subject } from 'rxjs';
import { debounceTime, takeUntil } from 'rxjs/operators';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';

@Component({
    selector: 'dot-content-palette',
    templateUrl: './dot-content-palette.component.html',
    styleUrls: ['./dot-content-palette.component.scss']
})
export class DotContentPaletteComponent implements OnInit, OnDestroy {
    @Input() items: DotCMSContentType[] = [];
    @Output() filterChange: EventEmitter<string> = new EventEmitter();

    @ViewChild('searchInput', { static: true }) searchInput: ElementRef;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(private dotContentletEditorService: DotContentletEditorService) {}

    ngOnInit(): void {
        observableFromEvent(this.searchInput.nativeElement, 'keyup')
            .pipe(debounceTime(500), takeUntil(this.destroy$))
            .subscribe((keyboardEvent: Event) => {
                this.filterChange.emit(keyboardEvent.target['value']);
            });
    }

    /**
     * Notify the dragging element to the service, and finally to the edit iframe.
     *
     * @param DotCMSContentType contentType
     * @memberof DotContentPaletteComponent
     */
    dragStart(contentType: DotCMSContentType): void {
        this.dotContentletEditorService.setDraggedContentType(contentType);
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}

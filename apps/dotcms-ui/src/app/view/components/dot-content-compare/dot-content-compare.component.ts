import { Component, Input } from '@angular/core';
import {
    DotContentCompareState,
    DotContentCompareStore
} from '@components/dot-content-compare/store/dot-content-compare.store';
import { Observable } from 'rxjs';

export interface DotContentCompareEvent {
    inode: string;
    identifier: string;
    language: string;
}

@Component({
    selector: 'dot-content-compare',
    templateUrl: './dot-content-compare.component.html',
    styleUrls: ['./dot-content-compare.component.scss'],
    providers: [DotContentCompareStore]
})
export class DotContentCompareComponent {
    @Input() set data(data: DotContentCompareEvent) {
        if (data != null) {
            this.store.loadData(data);
        }
    }
    vm$: Observable<DotContentCompareState> = this.store.vm$;

    constructor(private store: DotContentCompareStore) {}
}

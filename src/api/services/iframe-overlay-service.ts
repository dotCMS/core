import {Injectable} from '@angular/core';
import {Subject, Observable} from 'rxjs/Rx';

@Injectable()
export class IframeOverlayService {
    public $overlay: Subject<boolean> = new Subject<boolean>();

    constructor() {}

    show(): void {
        this.$overlay.next(true);
    }

    hide(): void {
        this.$overlay.next(false);
    }

    get overlay(): Observable<boolean> {
        return this.$overlay.asObservable();
    }
}
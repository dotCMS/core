import {Component, ViewEncapsulation, Input, Inject} from '@angular/core';
import {FormatDate} from "../../../../api/services/format-date-service";
import {Subject} from 'rxjs/Subject';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';

@Component({
    directives: [],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName,
    pipes: [],
    providers: [FormatDate],
    selector: 'custom-time',
    styleUrls: ['custom-time.css'],
    templateUrl: ['custom-time.html'],

})
export class CustomTimeComponent {
    @Input() time;

    private formattedTime:Subject<string> = new BehaviorSubject('');

    constructor(private formatDate: FormatDate) {

    }

    ngOnInit(): void {
        this.formattedTime.next(this.formatDate.getRelative(this.time));
    }

    ngAfterViewChecked() {
        // TODO: this is triggering even when open other dropdown component instance, need to check that.
        this.formattedTime.next(this.formatDate.getRelative(this.time));
    }
}
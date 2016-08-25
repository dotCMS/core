import {Component, ViewEncapsulation, Input, Inject} from '@angular/core';
import {FormatDate} from "../../../../api/services/format-date-service";

// Angular Material components
import {MdIcon} from '@angular2-material/icon/icon';

@Component({
    directives: [MdIcon],
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

    private formattedTime: string;

    constructor(@Inject('dotcmsConfig') private dotcmsConfig, private formatDate: FormatDate) {
    }

    ngOnInit(): void {
        this.formattedTime = this.formatDate.getRelative(this.time);
    }

    // This will trigger every time this component shows or hide
    ngAfterViewChecked(): void {
        this.formattedTime = this.formatDate.getRelative(this.time);
    }
}
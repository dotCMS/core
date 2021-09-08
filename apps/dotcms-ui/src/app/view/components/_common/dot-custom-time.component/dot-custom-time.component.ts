import { Component, ViewEncapsulation, Input, OnInit, AfterViewChecked } from '@angular/core';
import { DotFormatDateService } from '@services/dot-format-date-service';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-custom-time',
    styleUrls: ['./dot-custom-time.component.scss'],
    templateUrl: 'dot-custom-time.component.html'
})
export class CustomTimeComponent implements OnInit, AfterViewChecked {
    @Input()
    time;

    formattedTime = '';

    constructor(private dotFormatDateService: DotFormatDateService) {}

    ngOnInit(): void {
        this.formattedTime = this.dotFormatDateService.getRelative(this.time);
    }

    // TODO: this it's running every time the UI changes no matter where, need to fix it, should only run when custom-time shows
    ngAfterViewChecked(): void {
        // TODO: this is triggering even when open other dropdown component instance, need to check that.
        this.formattedTime = this.dotFormatDateService.getRelative(this.time);
    }
}

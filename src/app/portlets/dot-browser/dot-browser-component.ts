import {Component, OnInit, ViewEncapsulation} from '@angular/core';

@Component({
    encapsulation: ViewEncapsulation.None,
    providers: [],
    selector: 'dot-browser',
    styles: [require('./dot-browser.scss')],
    templateUrl: 'dot-browser.html'
})

export class DotBrowserComponent implements OnInit {
    constructor() {}
    ngOnInit(): void {}
}

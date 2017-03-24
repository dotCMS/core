import {Component, ViewEncapsulation} from '@angular/core';

@Component({
    directives: [],
    encapsulation: ViewEncapsulation.None,
    providers: [],
    selector: 'dot-main-component',
    styleUrls: ['main-component.css'],
    templateUrl: 'main-component.html',
})
export class MainComponent {
    private messages: any = {};
    private label = '';
    private isMenuCollapsed = false;

    constructor() {
    }

    ngOnInit(): void {
        document.body.style.backgroundColor = '';
        document.body.style.backgroundImage = '';
    }

    ngOnDestroy(): void {
        this.messages = null;
        this.label = null;
    }

    toggleSidenav(): void {
        this.isMenuCollapsed = !this.isMenuCollapsed;
    }
}
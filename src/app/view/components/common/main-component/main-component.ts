import {Component, ViewEncapsulation} from '@angular/core';

@Component({
    directives: [],
    encapsulation: ViewEncapsulation.None,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    providers: [],
    selector: 'dot-main-component',
    styleUrls: ['main-component.css'],
    templateUrl: ['main-component.html'],
})
export class MainComponent {
    private messages: any = {};
    private label: string = '';
    private isMenuCollapsed: boolean = false;

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

    toggleSidenav() {
        this.isMenuCollapsed = !this.isMenuCollapsed;
    }
}

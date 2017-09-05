import {Component, OnDestroy, OnInit, ViewEncapsulation} from '@angular/core';

@Component({
    encapsulation: ViewEncapsulation.None,
    providers: [],
    selector: 'dot-main-component',
    styleUrls: ['./main-legacy.component.scss'],
    templateUrl: './main-legacy.component.html',
})
export class MainComponentLegacy implements OnInit, OnDestroy{
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

import {Component, Inject} from "@angular/core";

@Component({
    selector: "header-component",
    template: `
    <div class="container">
        <h1>Header</h1>
        <span *ngFor="let menu of menuItems">
            <a href="{{menu.url}}">{{menu.tabName}}</a>
            <ul>
                <li *ngFor="let menuItem of menu.menuItems">
                    <a href="{{menuItem.url}}" *ngIf="!menuItem.angular && !menuItem.ajax">
                        {{menuItem.name}}
                    </a>
                    <a linkTo="{{menuItem.url}}" *ngIf="menuItem.angular && !menuItem.ajax">
                        {{menuItem.name}}
                    </a>
                    <a linkTo="{{menuItem.url}}" *ngIf="menuItem.ajax">
                        {{menuItem.name}}
                    </a>
                </li>
            </ul>
        </span>
    </div>
    `,
    providers: []
})

export class HeaderComponent {

    constructor(@Inject('menuItems') private menuItems:any[]) {

    }

}
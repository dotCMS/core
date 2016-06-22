import {Component, Inject} from "@angular/core";

@Component({
    selector: "header-component",
    template: `

    <div class="ui vertical menu">
        <div class="item" *ngFor="let menu of menuItems.navigationItems">
            <div class="header">{{menu.tabName}}</div>
        
            <div class="menu">
                <span *ngFor="let menuItem of menu.menuItems">
                    <a class="item" linkTo="/portlet/{{menuItem.id}}" *ngIf="menuItem.url && !menuItem.angular && !menuItem.ajax">
                        {{menuItem.name}}
                    </a>
                    <a class="item" linkTo="{{menuItem.url}}" *ngIf="menuItem.angular && !menuItem.ajax">
                        {{menuItem.name}}
                    </a>
                    <a class="item" linkTo="{{menuItem.url}}"  *ngIf="menuItem.ajax">
                        {{menuItem.name}}
                    </a>
                </span>
            </div>
        </div>
    </div>
    `,
    providers: []
})

export class HeaderComponent {

    constructor(@Inject('menuItems') private menuItems:any[]) {

    }

}
import {Component, Inject} from "@angular/core";

@Component({
    selector: "header-component",
    template: `
    <div class="ui vertical menu">
        <div class="item" *ngFor="let menu of menuItems">
            <div class="header">{{menu.tabName}}</div>
        
            <div class="menu">
                <!--TODO: find a way to do this without the span-->
                <span *ngFor="let menuItem of menu.menuItems">
                    <a class="item" href="{{menuItem.url}}">
                        {{menuItem.name}}
                    </a>
                    <a class="item" linkTo="{{menuItem.url}}" *ngIf="menuItem.angular && !menuItem.ajax">
                        {{menuItem.name}}
                    </a>
                    <a class="item" linkTo="{{menuItem.url}}" *ngIf="menuItem.ajax">
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
import {Component} from "@angular/core";
import {HeaderComponent} from "./HeaderComponent";

@Component({
    selector: "app",
    template: `
    <header-component></header-component>
    <div class="container">
        <h1>Hello World</h1>
        <route-view></route-view>
    </div>
    `,
    providers: [],
    directives: [HeaderComponent]
})
export class AppComponent {
    constructor() {
    }
}
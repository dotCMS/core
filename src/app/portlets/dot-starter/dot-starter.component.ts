import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { pluck, take } from 'rxjs/operators';

@Component({
    selector: 'dot-starter',
    templateUrl: './dot-starter.component.html',
    styleUrls: ['./dot-starter.component.scss']
})
export class DotStarterComponent implements OnInit {
    username: string;

    constructor(private route: ActivatedRoute) {}

    ngOnInit() {
        this.route.data.pipe(pluck('username'), take(1)).subscribe((username: string) => {
            this.username = username;
        });
    }
}

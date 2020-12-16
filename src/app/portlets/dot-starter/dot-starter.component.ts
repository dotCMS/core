import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { pluck, take } from 'rxjs/operators';
import { DotToolGroupService } from '@services/dot-tool-group/dot-tool-group.service';

@Component({
    selector: 'dot-starter',
    templateUrl: './dot-starter.component.html',
    styleUrls: ['./dot-starter.component.scss']
})
export class DotStarterComponent implements OnInit {
    username: string;

    constructor(private route: ActivatedRoute, private dotToolGroupService: DotToolGroupService) {}

    ngOnInit() {
        this.route.data.pipe(pluck('username'), take(1)).subscribe((username: string) => {
            this.username = username;
        });
    }

    /**
     * Hit the endpoint to show/hide the tool group in the menu.
     * @param {boolean} hide
     * @memberof DotStarterComponent
     */
    handleVisibility(hide: boolean): void {
        const layoutId = 'gettingstarted';
        hide
            ? this.dotToolGroupService.hide(layoutId).pipe(take(1)).subscribe()
            : this.dotToolGroupService.show(layoutId).pipe(take(1)).subscribe();
    }
}

import { Observable } from 'rxjs';

import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { map, pluck, take } from 'rxjs/operators';

import { DotAccountService } from '@dotcms/app/api/services/dot-account-service';
import { DotCurrentUser, DotPermissionsType, PermissionsType } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-starter',
    templateUrl: './dot-starter.component.html',
    styleUrls: ['./dot-starter.component.scss']
})
export class DotStarterComponent implements OnInit {
    userData$: Observable<{
        username: string;
        showCreateContentLink: boolean;
        showCreateDataModelLink: boolean;
        showCreatePageLink: boolean;
        showCreateTemplateLink: boolean;
    }>;
    username: string;
    showCreateContentLink: boolean;
    showCreateDataModelLink: boolean;
    showCreatePageLink: boolean;
    showCreateTemplateLink: boolean;

    constructor(
        private route: ActivatedRoute,
        private dotAccountService: DotAccountService
    ) {}

    ngOnInit() {
        this.userData$ = this.route.data.pipe(
            pluck('userData'),
            take(1),
            map(
                ({
                    user,
                    permissions
                }: {
                    user: DotCurrentUser;
                    permissions: DotPermissionsType;
                }) => {
                    return {
                        username: user.givenName,
                        showCreateContentLink: permissions[PermissionsType.CONTENTLETS].canWrite,
                        showCreateDataModelLink: permissions[PermissionsType.STRUCTURES].canWrite,
                        showCreatePageLink: permissions[PermissionsType.HTMLPAGES].canWrite,
                        showCreateTemplateLink: permissions[PermissionsType.TEMPLATES].canWrite
                    };
                }
            )
        );
    }

    /**
     * Hit the endpoint to show/hide the tool group in the menu.
     * @param {boolean} hide
     * @memberof DotStarterComponent
     */
    handleVisibility(hide: boolean): void {
        hide
            ? this.dotAccountService.removeStarterPage().subscribe()
            : this.dotAccountService.addStarterPage().subscribe();
    }
}

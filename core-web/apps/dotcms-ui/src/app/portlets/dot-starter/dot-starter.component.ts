import { Observable } from 'rxjs';

import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

import { map, pluck, take } from 'rxjs/operators';

import { DotCurrentUser, DotPermissionsType, PermissionsType } from '@dotcms/dotcms-models';

import { DotAccountService } from '../../api/services/dot-account-service';

@Component({
    selector: 'dot-starter',
    templateUrl: './dot-starter.component.html',
    standalone: false
})
export class DotStarterComponent implements OnInit {
    private route = inject(ActivatedRoute);
    private dotAccountService = inject(DotAccountService);

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

    readonly #destroyRef = inject(DestroyRef);

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
        const subscription = hide
            ? this.dotAccountService.removeStarterPage()
            : this.dotAccountService.addStarterPage();

        subscription.pipe(takeUntilDestroyed(this.#destroyRef)).subscribe();
    }
}

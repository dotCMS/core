import { Observable } from 'rxjs';

import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

import { map, pluck, take } from 'rxjs/operators';

import {
    DotCurrentUser,
    DotCMSClazzes,
    DotCMSContentType,
    DotPermissionsType,
    PermissionsType
} from '@dotcms/dotcms-models';
import { Site } from '@dotcms/dotcms-js';

import { DotAccountService } from '../../api/services/dot-account-service';

@Component({
    selector: 'dot-starter',
    templateUrl: './dot-starter.component.html',
    styleUrls: ['./dot-starter.component.scss'],
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

    // Test properties for dot-content-type component
    selectedContentType: DotCMSContentType | null = {
        baseType: 'CONTENT',
        clazz: DotCMSClazzes.SIMPLE_CONTENT_TYPE,
        defaultType: false,
        fields: [],
        fixed: false,
        folder: 'SYSTEM_FOLDER',
        folderPath: '/',
        host: '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d',
        iDate: 1765477561000,
        icon: 'event_note',
        id: 'e2537ac7bc0c4b9c5e4e987aecb1aba7',
        layout: [
            {
                divider: {
                    id: 'row-field-id',
                    variable: 'rowField',
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                    contentTypeId: 'e2537ac7bc0c4b9c5e4e987aecb1aba7',
                    dataType: 'SYSTEM',
                    fieldContentTypeProperties: [],
                    fieldType: 'Row',
                    fieldTypeLabel: 'Row',
                    fieldVariables: [],
                    fixed: false,
                    forceIncludeInApi: false,
                    iDate: 1765492714000,
                    indexed: false,
                    listed: false,
                    modDate: 1765492714000,
                    name: 'Row Field',
                    readOnly: false,
                    required: false,
                    searchable: false,
                    sortOrder: -1,
                    unique: false
                },
                columns: [
                    {
                        columnDivider: {
                            id: 'column-field-id',
                            variable: 'columnField',
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                            contentTypeId: 'e2537ac7bc0c4b9c5e4e987aecb1aba7',
                            dataType: 'SYSTEM',
                            fieldContentTypeProperties: [],
                            fieldType: 'Column',
                            fieldTypeLabel: 'Column',
                            fieldVariables: [],
                            fixed: false,
                            forceIncludeInApi: false,
                            iDate: 1765492714000,
                            indexed: false,
                            listed: false,
                            modDate: 1765492714000,
                            name: 'Column Field',
                            readOnly: false,
                            required: false,
                            searchable: false,
                            sortOrder: -1,
                            unique: false
                        },
                        fields: []
                    }
                ]
            }
        ],
        metadata: {
            CONTENT_EDITOR2_ENABLED: false
        },
        modDate: 1765477561000,
        multilingualable: false,
        nEntries: 0,
        name: 'Test 124',
        siteName: 'default',
        sortOrder: 0,
        system: false,
        variable: 'Test124',
        versionable: true,
        workflows: [
            {
                archived: false,
                creationDate: new Date(1765478128761),
                defaultScheme: false,
                description: '',
                entryActionId: null,
                id: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
                mandatory: false,
                modDate: new Date(1765476157839),
                name: 'System Workflow',
                system: true,
                variableName: 'SystemWorkflow'
            }
        ]
    } as DotCMSContentType;

    // selectedContentType = null;
    isDisabled = false;
    isInitiallyDisabled = true;
    contentTypeControl = new FormControl<DotCMSContentType | null>(null);

    // Test properties for dot-site component
    selectedSite: Site | null = null;
    siteControl = new FormControl<Site | null>(null);

    readonly #destroyRef = inject(DestroyRef);

    onContentTypeChange(contentType: DotCMSContentType | null): void {
        console.log('Content type changed:', contentType);
    }

    onSiteChange(site: Site | null): void {
        console.log('Site changed:', site);
    }

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

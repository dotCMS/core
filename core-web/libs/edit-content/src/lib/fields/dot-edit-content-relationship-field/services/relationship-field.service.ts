import { faker } from '@faker-js/faker';
import { forkJoin, Observable, of } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { map, tap } from 'rxjs/operators';

import { DotFieldService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { RelationshipFieldItem } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/models/relationship.models';

import { MANDATORY_FIRST_COLUMNS, MANDATORY_LAST_COLUMNS } from '../dot-edit-content-relationship-field.constants';
import { Column } from '../models/column.model';
@Injectable({
    providedIn: 'root'
})
export class RelationshipFieldService {
    readonly #fieldService = inject(DotFieldService);

    /**
     * Gets relationship content items
     * @returns Observable of RelationshipFieldItem array
     */
    getContent(count = 100): Observable<RelationshipFieldItem[]> {
        const contentlets = this.#generateMockContentlets(count);
        const relationshipContent = this.#mapContentletsToRelationshipItems(contentlets);

        return of(relationshipContent);
    }

    getColumnsAndContent(contentTypeId: string): Observable<[Column[], RelationshipFieldItem[]]> {
        console.log('getColumnsAndContent', contentTypeId);
        
        return forkJoin([this.getColumns(contentTypeId), this.getContent()]).pipe(
            tap(([columns, content]) => console.log(columns, content))
        );
    }

    /**
     * Gets the columns for the relationship field
     * @param contentTypeId The content type ID
     * @returns Observable of Column array
     */
    getColumns(contentTypeId: string): Observable<Column[]> {
        return this.#fieldService
            .getFields(contentTypeId, 'SHOW_IN_LIST')
            .pipe(
                map((fields) =>
                    fields.map((field) => ({ field: field.variable, header: field.name }))
                ),
                map((columns) => [
                    ...MANDATORY_FIRST_COLUMNS.map((field) => ({ field, header: field })),
                    ...columns,
                    ...MANDATORY_LAST_COLUMNS.map((field) => ({ field, header: field }))
                ])
            );
    }

    /**
     * Generates mock contentlets for testing purposes
     * @returns Array of DotCMSContentlet
     */
    #generateMockContentlets(count: number): DotCMSContentlet[] {
        return faker.helpers.multiple(() => this.#createMockContentlet(), { count });
    }

    /**
     * Creates a single mock contentlet
     * @returns DotCMSContentlet
     */
    #createMockContentlet(): DotCMSContentlet {
        return {
            identifier: faker.string.uuid(),
            inode: faker.string.uuid(),
            title: faker.lorem.words({ min: 1, max: 10 }),
            contentType: faker.helpers.arrayElement(['News', 'Blog', 'Product', 'Event']),
            baseType: 'CONTENT',
            languageId: faker.number.int({ min: 1, max: 5 }),
            folder: faker.system.directoryPath(),
            hostName: faker.internet.domainName(),
            modUser: faker.internet.userName(),
            modDate: faker.date.recent().toISOString(),
            owner: faker.internet.userName(),
            sortOrder: faker.number.int({ min: 1, max: 100 }),
            live: faker.datatype.boolean(),
            working: true,
            archived: false,
            locked: faker.datatype.boolean(),
            hasLiveVersion: faker.datatype.boolean(),
            hasTitleImage: faker.datatype.boolean(),
            url: faker.internet.url(),
            titleImage: faker.image.url(),
            stInode: faker.string.uuid(),
            host: faker.internet.domainName(),
            modUserName: faker.internet.userName()
        };
    }

    /**
     * Maps contentlets to relationship field items
     * @param contentlets Array of DotCMSContentlet to be mapped
     * @returns Array of RelationshipFieldItem
     */
    #mapContentletsToRelationshipItems(contentlets: DotCMSContentlet[]): RelationshipFieldItem[] {
        return contentlets.map((content) => ({
            id: content.identifier,
            title: content.title,
            language: faker.helpers.arrayElement([
                'English (en-us)',
                'Spanish (es-es)',
                'French (fr-fr)'
            ]),
            state: this.#getRandomState(),
            description: faker.lorem.sentence(3),
            step: faker.helpers.arrayElement(['Published', 'Editing', 'Archived', 'QA', 'New']),
            lastUpdate: content.modDate
        }));
    }

    #getRandomState(): { label: string; styleClass: string } {
        const label = faker.helpers.arrayElement(['Changed', 'Published', 'Draft', 'Archived']);

        const styleClasses = {
            Changed: 'p-chip-sm p-chip-blue',
            Published: 'p-chip-sm p-chip-success',
            Draft: 'p-chip-sm p-chip-warning',
            Archived: 'p-chip-sm p-chip-error'
        };

        return {
            label,
            styleClass: styleClasses[label]
        };
    }
}

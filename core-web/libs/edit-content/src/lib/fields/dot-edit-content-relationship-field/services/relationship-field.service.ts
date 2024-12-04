import { faker } from '@faker-js/faker';
import { Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { RelationshipFieldItem } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/models/relationship.models';

@Injectable({
    providedIn: 'root'
})
export class RelationshipFieldService {
    readonly #CONTENT_TYPES = ['News', 'Blog', 'Product', 'Event'] as const;

    /**
     * Gets relationship content items
     * @returns Observable of RelationshipFieldItem array
     */
    getContent(count = 100): Observable<RelationshipFieldItem[]> {
        const contentlets = this.#generateMockContentlets(count);
        const relationshipContent = this.#mapContentletsToRelationshipItems(contentlets);

        return of(relationshipContent);
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
            title: faker.commerce.productName(),
            contentType: faker.helpers.arrayElement(this.#CONTENT_TYPES),
            baseType: 'CONTENT',
            languageId: faker.number.int({ min: 1, max: 5 }),
            folder: faker.system.directoryPath(),
            hostName: faker.internet.domainName(),
            modUser: faker.internet.userName(),
            modDate: faker.date.recent().toISOString(),
            owner: faker.internet.userName(),
            sortOrder: faker.datatype.number(100),
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
            language: content.languageId.toString(),
            state: this.#getRandomState(),
            description: faker.lorem.sentence(),
            step: faker.helpers.arrayElement(['Step 1', 'Step 2', 'Step 3']),
            lastUpdate: content.modDate
        }));
    }

    #getRandomState(): { label: string; styleClass: string } {
        const label = faker.helpers.arrayElement([
            'Changed',
            'Published',
            'Draft',
            'Archived'
        ]);

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

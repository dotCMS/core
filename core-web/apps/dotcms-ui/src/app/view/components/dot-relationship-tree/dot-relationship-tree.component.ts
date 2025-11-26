import { Component, Input, OnChanges } from '@angular/core';

import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotIconComponent } from '@dotcms/ui';

@Component({
    selector: 'dot-relationship-tree',
    templateUrl: './dot-relationship-tree.component.html',
    styleUrls: ['./dot-relationship-tree.component.scss'],
    imports: [DotIconComponent]
})
export class DotRelationshipTreeComponent implements OnChanges {
    @Input() velocityVar: string;
    @Input() contentType: DotCMSContentType;
    @Input() isParentField: boolean;

    child: string;
    parent: string;

    ngOnChanges(): void {
        this.setValues();
    }
    /**
     * Sets values of the relationship tree component
     *
     * @memberof DotRelationshipTreeComponent
     */
    setValues(): void {
        const [relatedContentType] = this.velocityVar?.split('.') || '';
        const contentTypeName = this.contentType?.name;

        this.child = this.isParentField ? relatedContentType : contentTypeName;
        this.parent = this.isParentField ? contentTypeName : relatedContentType;
    }
}

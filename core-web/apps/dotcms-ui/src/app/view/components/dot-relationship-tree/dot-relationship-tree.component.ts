import { Component, Input, OnInit } from '@angular/core';
import { DotCMSContentType } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-relationship-tree',
    templateUrl: './dot-relationship-tree.component.html',
    styleUrls: ['./dot-relationship-tree.component.scss']
})
export class DotRelationshipTreeComponent implements OnInit {
    @Input() velocityVar: string;
    @Input() contentType: DotCMSContentType;
    @Input() isParentField: boolean;

    child: string;
    parent: string;

    ngOnInit(): void {
        this.setInitialValues();
    }
    /**
     * Sets initial values of the relationship tree component
     *
     * @memberof DotRelationshipTreeComponent
     */
    setInitialValues(): void {
        const [relatedContentType] = this.velocityVar?.split('.') || '';
        const contentTypeName = this.contentType?.name;

        this.child = this.isParentField ? relatedContentType : contentTypeName;
        this.parent = this.isParentField ? contentTypeName : relatedContentType;
    }
}

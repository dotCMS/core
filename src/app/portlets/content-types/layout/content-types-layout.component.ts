import { Component, Input, OnChanges, OnInit, OnDestroy } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { DotMenuService } from '@services/dot-menu.service';
import { FieldDragDropService } from '../fields/service';
import { take } from 'rxjs/internal/operators/take';
import { Subject } from 'rxjs/internal/Subject';
import { takeUntil } from 'rxjs/internal/operators/takeUntil';

@Component({
    selector: 'dot-content-type-layout',
    styleUrls: ['./content-types-layout.component.scss'],
    templateUrl: 'content-types-layout.component.html'
})
export class ContentTypesLayoutComponent implements OnChanges, OnInit, OnDestroy {
    @Input()
    contentTypeId: string;

    permissionURL: string;
    pushHistoryURL: string;
    relationshipURL: string;

    i18nMessages: {
        [key: string]: string;
    } = {};

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotMessageService: DotMessageService,
        private dotMenuService: DotMenuService,
        private fieldDragDropService: FieldDragDropService
    ) {}

    ngOnInit(): void {
        this.fieldDragDropService.setBagOptions();
        this.dotMessageService
            .getMessages([
                'contenttypes.sidebar.components.title',
                'contenttypes.tab.fields.header',
                'contenttypes.sidebar.layouts.title',
                'contenttypes.tab.permissions.header',
                'contenttypes.tab.publisher.push.history.header',
                'contenttypes.tab.relationship.header'
            ])
            .pipe(take(1))
            .subscribe((res) => {
                this.i18nMessages = res;
            });
    }

    ngOnChanges(changes): void {
        if (changes.contentTypeId.currentValue) {
            this.dotMenuService.getDotMenuId('content-types-angular').pipe(takeUntil(this.destroy$)).subscribe((id) => {
                // tslint:disable-next-line:max-line-length
                this.relationshipURL = `c/portal/layout?p_l_id=${id}&p_p_id=content-types&_content_types_struts_action=%2Fext%2Fstructure%2Fview_relationships&_content_types_structure_id=${
                    changes.contentTypeId.currentValue
                }`;
            });

            this.permissionURL = `/html/content_types/permissions.jsp?contentTypeId=${
                changes.contentTypeId.currentValue
            }&popup=true`;
            this.pushHistoryURL = `/html/content_types/push_history.jsp?contentTypeId=${
                changes.contentTypeId.currentValue
            }&popup=true`;
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}

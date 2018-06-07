import { Component, OnInit } from '@angular/core';
import { DotContentletEditorService } from '../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotNavigationService } from '../../view/components/dot-navigation/dot-navigation.service';
import { ActivatedRoute } from '@angular/router';

@Component({
    providers: [],
    selector: 'dot-contentlets',
    template: ''
})
export class DotContentletsComponent implements OnInit {

    constructor(
        private dotContentletEditorService: DotContentletEditorService,
        private dotNavigationService: DotNavigationService,
        private route: ActivatedRoute,
    ) {}

    ngOnInit(): void {
        this.dotNavigationService.goToFirstPortlet();
        this.dotContentletEditorService.edit({
            data: {
                inode: this.route.snapshot.params.inode
            }
        });
    }
}

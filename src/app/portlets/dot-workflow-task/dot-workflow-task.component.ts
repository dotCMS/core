import { Component, OnInit } from '@angular/core';
import { DotWorkflowTaskDetailService } from '../../view/components/dot-workflow-task-detail/services/dot-workflow-task-detail.service';
import { DotNavigationService } from '../../view/components/dot-navigation/dot-navigation.service';
import { ActivatedRoute } from '@angular/router';

@Component({
    providers: [],
    selector: 'dot-workflow-task',
    template: ''
})
export class DotWorkflowTaskComponent implements OnInit {
    constructor(
        private dotWorkflowTaskDetailService: DotWorkflowTaskDetailService,
        private dotNavigationService: DotNavigationService,
        private route: ActivatedRoute
    ) {}

    ngOnInit(): void {
        this.dotNavigationService.goToFirstPortlet();
        this.dotWorkflowTaskDetailService.view({
            id: this.route.snapshot.params.id
        });
    }
}

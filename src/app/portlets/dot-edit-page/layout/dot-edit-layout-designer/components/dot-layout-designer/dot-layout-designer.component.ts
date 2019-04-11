import { Component, OnInit, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { DotMessageService } from '@services/dot-messages-service';
import { Observable } from 'rxjs';
import { take } from 'rxjs/operators';

@Component({
    selector: 'dot-layout-designer',
    templateUrl: './dot-layout-designer.component.html',
    styleUrls: ['./dot-layout-designer.component.scss']
})
export class DotLayoutDesignerComponent implements OnInit {
    @Input()
    group: FormGroup;

    messages$: Observable<{ [key: string]: string }>;

    constructor(public dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.messages$ = this.dotMessageService.getMessages([
            'editpage.layout.designer.header',
            'editpage.layout.designer.footer'
        ]).pipe(take(1));
    }
}

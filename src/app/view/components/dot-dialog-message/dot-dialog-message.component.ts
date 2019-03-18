import {
    DotDialogMessageService,
    DotDialogMessageParams
} from './services/dot-dialog-message.service';
import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';

@Component({
    selector: 'dot-dialog-message',
    templateUrl: './dot-dialog-message.component.html',
    styleUrls: ['./dot-dialog-message.component.scss']
})
export class DotDialogMessageComponent implements OnInit {
    data$: Observable<DotDialogMessageParams>;

    constructor(public dotDialogMessageService: DotDialogMessageService) {}

    ngOnInit() {
        this.data$ = this.dotDialogMessageService.sub();
    }

    close() {
        this.dotDialogMessageService.push(null);
    }

}

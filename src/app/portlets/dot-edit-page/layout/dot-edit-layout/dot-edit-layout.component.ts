import { Component } from '@angular/core';

@Component({
    selector: 'dot-edit-layout',
    templateUrl: './dot-edit-layout.component.html',
    styleUrls: ['./dot-edit-layout.component.scss']
})
export class DotEditLayoutComponent {
    dummyContainers = [
        {
            id: '0.6788371914418043',
            config: {
                fixed: true,
                sizex: 3,
                maxCols: 12,
                maxRows: 1,
                row: 1,
                col: 1,
                sizey: 1,
                dragHandle: null,
                resizeHandle: null,
                draggable: true,
                resizable: true,
                borderSize: 25
            }
        },
        {
            id: '0.04567715273481454',
            config: {
                fixed: true,
                sizex: 3,
                maxCols: 12,
                maxRows: 1,
                row: 2,
                col: 4,
                sizey: 1,
                dragHandle: null,
                resizeHandle: null,
                draggable: true,
                resizable: true,
                borderSize: 25
            }
        },
        {
            id: '0.6654743856177827',
            config: {
                fixed: true,
                sizex: 3,
                maxCols: 12,
                maxRows: 1,
                row: 3,
                col: 7,
                sizey: 1,
                dragHandle: null,
                resizeHandle: null,
                draggable: true,
                resizable: true,
                borderSize: 25
            }
        }
    ];

    constructor() {}
}

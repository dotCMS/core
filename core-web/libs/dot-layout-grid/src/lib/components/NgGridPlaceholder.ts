import { Component, ElementRef, OnInit, Renderer2, inject } from '@angular/core';

import { NgGrid } from '../directives/NgGrid';
import { NgGridItemPosition, NgGridItemSize } from '../interfaces/INgGrid';

@Component({
    selector: 'ng-grid-placeholder',
    template: ''
})
export class NgGridPlaceholder implements OnInit {
    private _ngEl = inject(ElementRef);
    private _renderer = inject(Renderer2);

    private _size: NgGridItemSize;
    private _position: NgGridItemPosition;
    private _ngGrid: NgGrid;
    private _cascadeMode: string;

    public registerGrid(ngGrid: NgGrid) {
        this._ngGrid = ngGrid;
    }

    public ngOnInit(): void {
        this._renderer.addClass(this._ngEl.nativeElement, 'grid-placeholder');
        if (this._ngGrid.autoStyle) {
            this._renderer.setStyle(this._ngEl.nativeElement, 'position', 'absolute');
        }
    }

    public setSize(newSize: NgGridItemSize): void {
        this._size = newSize;
        this._recalculateDimensions();
    }

    public setGridPosition(newPosition: NgGridItemPosition): void {
        this._position = newPosition;
        this._recalculatePosition();
    }

    public setCascadeMode(cascade: string): void {
        this._cascadeMode = cascade;
        switch (cascade) {
            case 'up':

            case 'left':

            default:
                this._renderer.setStyle(this._ngEl.nativeElement, 'left', '0px');
                this._renderer.setStyle(this._ngEl.nativeElement, 'top', '0px');
                this._renderer.removeStyle(this._ngEl.nativeElement, 'right');
                this._renderer.removeStyle(this._ngEl.nativeElement, 'bottom');
                break;

            case 'right':
                this._renderer.setStyle(this._ngEl.nativeElement, 'right', '0px');
                this._renderer.setStyle(this._ngEl.nativeElement, 'top', '0px');
                this._renderer.removeStyle(this._ngEl.nativeElement, 'left');
                this._renderer.removeStyle(this._ngEl.nativeElement, 'bottom');
                break;

            case 'down':
                this._renderer.setStyle(this._ngEl.nativeElement, 'left', '0px');
                this._renderer.setStyle(this._ngEl.nativeElement, 'bottom', '0px');
                this._renderer.removeStyle(this._ngEl.nativeElement, 'right');
                this._renderer.removeStyle(this._ngEl.nativeElement, 'top');
                break;
        }
    }

    // 	Private methods
    private _setDimensions(w: number, h: number): void {
        this._renderer.setStyle(this._ngEl.nativeElement, 'width', w + 'px');
        this._renderer.setStyle(this._ngEl.nativeElement, 'height', h + 'px');
    }

    private _setPosition(x: number, y: number): void {
        switch (this._cascadeMode) {
            case 'up':

            case 'left':

            default:
                this._renderer.setStyle(
                    this._ngEl.nativeElement,
                    'transform',
                    'translate(' + x + 'px, ' + y + 'px)'
                );
                break;

            case 'right':
                this._renderer.setStyle(
                    this._ngEl.nativeElement,
                    'transform',
                    'translate(' + -x + 'px, ' + y + 'px)'
                );
                break;

            case 'down':
                this._renderer.setStyle(
                    this._ngEl.nativeElement,
                    'transform',
                    'translate(' + x + 'px, ' + -y + 'px)'
                );
                break;
        }
    }

    private _recalculatePosition(): void {
        const x: number =
            (this._ngGrid.colWidth + this._ngGrid.marginLeft + this._ngGrid.marginRight) *
                (this._position.col - 1) +
            this._ngGrid.marginLeft +
            this._ngGrid.screenMargin;
        const y: number =
            (this._ngGrid.rowHeight + this._ngGrid.marginTop + this._ngGrid.marginBottom) *
                (this._position.row - 1) +
            this._ngGrid.marginTop;
        this._setPosition(x, y);
    }

    private _recalculateDimensions(): void {
        const w: number =
            this._ngGrid.colWidth * this._size.x +
            (this._ngGrid.marginLeft + this._ngGrid.marginRight) * (this._size.x - 1);
        const h: number =
            this._ngGrid.rowHeight * this._size.y +
            (this._ngGrid.marginTop + this._ngGrid.marginBottom) * (this._size.y - 1);
        this._setDimensions(w, h);
    }
}

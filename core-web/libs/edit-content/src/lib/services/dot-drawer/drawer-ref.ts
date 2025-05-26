import { ComponentRef } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { take } from 'rxjs/operators';

import { DotDrawerOptions, DotDrawerResult } from './drawer-options.interface';

/**
 * Reference to a drawer instance
 * Provides methods to control and interact with the drawer
 */
export class DotDrawerRef<T = any, R = any> {
    private readonly afterCloseSubject = new Subject<DotDrawerResult<R>>();
    private readonly afterOpenSubject = new Subject<void>();

    componentRef?: ComponentRef<T>;

    constructor(
        private drawerService: any, // Will be injected
        private drawerId: string,
        private options: DotDrawerOptions<T>
    ) { }

    /**
     * Observable that emits when the drawer is closed
     */
    get afterClose(): Observable<DotDrawerResult<R>> {
        return this.afterCloseSubject.asObservable();
    }

    /**
     * Observable that emits when the drawer is opened
     */
    get afterOpen(): Observable<void> {
        return this.afterOpenSubject.asObservable();
    }

    /**
     * Get the component instance inside the drawer
     */
    getContentComponent(): T | null {
        return this.componentRef?.instance || null;
    }

    /**
     * Close the drawer with an optional result
     */
    close(result?: R): void {
        this.drawerService.closeDrawer(this.drawerId, {
            type: 'close',
            data: result
        } as DotDrawerResult<R>);
    }

    /**
     * Close the drawer with cancel action
     */
    cancel(): void {
        this.drawerService.closeDrawer(this.drawerId, {
            type: 'cancel'
        } as DotDrawerResult<R>);
    }

    /**
     * Update drawer options
     */
    updateOptions(options: Partial<DotDrawerOptions<T>>): void {
        this.drawerService.updateDrawerOptions(this.drawerId, options);
    }

    /**
     * Check if the drawer is currently open
     */
    isOpen(): boolean {
        return this.drawerService.isDrawerOpen(this.drawerId);
    }

    /**
     * Trigger the after open event (internal use)
     */
    triggerAfterOpen(): void {
        this.afterOpenSubject.next();
    }

    /**
     * Trigger the after close event (internal use)
     */
    triggerAfterClose(result: DotDrawerResult<R>): void {
        this.afterCloseSubject.next(result);
        this.afterCloseSubject.complete();
        this.afterOpenSubject.complete();
    }

    /**
     * Get a promise that resolves when the drawer is closed
     */
    afterClosedPromise(): Promise<DotDrawerResult<R>> {
        return this.afterClose.pipe(take(1)).toPromise();
    }

    /**
     * Get a promise that resolves when the drawer is opened
     */
    afterOpenPromise(): Promise<void> {
        return this.afterOpen.pipe(take(1)).toPromise();
    }
}

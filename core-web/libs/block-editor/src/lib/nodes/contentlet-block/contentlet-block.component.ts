import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { pluck, take } from 'rxjs/operators';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { AngularNodeViewComponent } from '../../NodeViewRenderer';

@Component({
    selector: 'dot-contentlet-block',
    templateUrl: './contentlet-block.component.html',
    styleUrls: ['./contentlet-block.component.scss']
})
export class ContentletBlockComponent extends AngularNodeViewComponent implements OnInit {
    private readonly http = inject(HttpClient);
    private readonly detroyRef = inject(DestroyRef);

    protected readonly data = signal(null);

    ngOnInit() {
        const data = this.node.attrs.data;

        // Remove this when backend is ready and test it
        this.getContentletByInode(data.identifier)
            .pipe(takeUntilDestroyed(this.detroyRef))
            .subscribe((contentlet: DotCMSContentlet) => {
                this.data.set(contentlet);
            });
    }

    /**
     * Get the Contentlet versions by the inode.
     *
     * @param string inode
     * @returns Observable<DotCMSContentlet>
     * @memberof DotContentletService
     */

    private getContentletByInode(inode: string): Observable<DotCMSContentlet> {
        return this.http.get(`/api/v1/content/${inode}`).pipe(take(1), pluck('entity'));
    }
}

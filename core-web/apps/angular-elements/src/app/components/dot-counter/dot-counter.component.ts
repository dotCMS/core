import {
    ChangeDetectionStrategy,
    Component,
    Input,
    OnInit,
    Output,
    EventEmitter,
    DoCheck
} from '@angular/core';

@Component({
    selector: 'dotcms-dot-counter',
    standalone: true,
    imports: [],
    templateUrl: './dot-counter.component.html',
    styleUrls: ['./dot-counter.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCounterComponent implements OnInit, DoCheck {
    @Input() initialValue?: number;

    @Input() step?: number;

    @Input() flag?: number;

    @Output() flagReached: EventEmitter<number> = new EventEmitter();

    count = 0;

    ngOnInit() {
        this.count = this.initialValue ?? 0;
    }

    ngDoCheck() {
        if (this.flag !== undefined && this.count == this.flag) this.flagReached.emit(this.flag);
    }

    increment() {
        this.count += this.step ?? 1;
    }

    decrement() {
        this.count -= this.step ?? 1;
    }

    reset() {
        this.count = this.initialValue ?? 0;
    }
}

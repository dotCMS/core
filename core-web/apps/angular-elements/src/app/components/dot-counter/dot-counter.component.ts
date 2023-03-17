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
    @Input() initialValue = 0;

    @Input() step = 1;

    @Input() flag: number = Number.MAX_SAFE_INTEGER;

    @Output() flagReached: EventEmitter<number> = new EventEmitter();

    count = 0;

    ngOnInit() {
        this.count = this.initialValue;
    }

    ngDoCheck() {
        if (this.count == this.flag) this.flagReached.emit(this.flag);
    }

    increment() {
        this.count += this.step;
    }

    decrement() {
        this.count -= this.step;
    }

    reset() {
        this.count = this.initialValue;
    }
}

import {Component, Input, Output, EventEmitter, ElementRef, ViewChild, forwardRef} from '@angular/core';
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from '@angular/forms';
import {MdIcon} from '@angular2-material/icon/icon';

@Component({
    directives: [MdIcon],
    host: {
        '(document:click)': 'handleClick($event)',
    },
    moduleId: __moduleName,
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotSelect),
        }
    ],
    selector: 'dot-select',
    styleUrls: ['dot-select.css'],
    templateUrl: ['dot-select.html']
})
export class DotSelect implements ControlValueAccessor {
    @Input('value') _innerValue;
    @Input() disabled;

    @Output() change = new EventEmitter();

    @ViewChild('selectOptions') selectOptions: ElementRef;
    @ViewChild('dotSelect') dotSelect: ElementRef;

    private elementRef;
    private isHover: boolean = false;
    private isOpen: boolean = false;
    private options: Array<DotOption> = [];
    private selectedText: string;

    propagateChange = (_: any) => {};

    constructor(myElement: ElementRef) {
        this.elementRef = myElement;
    }

    set innerValue(val) {
        console.log('VAL');
        this._innerValue = val;
        let selectedOption = this.options.filter((option) => option.value === val)[0];
        this.selectedText = selectedOption ? selectedOption.text : '';
        this.propagateChange(val);
    }

    get innerValue(): string {
        return this._innerValue;
    }

    ngAfterContentInit(): void {
        this.disabled = typeof(this.disabled) === 'string' ? true : this.disabled;
    }

    ngOnChanges(): void {
        this.dotSelect.nativeElement.style.width = this.selectOptions.nativeElement.offsetWidth + 'px';
    }

    addOption(option: DotOption): void {
        this.options.push(option);

        if (this._innerValue && option.value === this._innerValue) {
           this.selectedText = option.text;
        }
    }

    openClose(): void {
        if (this.disabled) {
            return;
        }
        this.isOpen = !this.isOpen;
    }

    setValue(value: string): void {
        this.innerValue = value;
    }

    setOption(option): void {
        this.openClose();
        this.innerValue = option.value;

        this.options.forEach((o) => {
            o.selected = o.value === option.value;
        });

        this.change.emit(option);
    }

    toggleHoverState(): void {
        this.isHover = !this.isHover;
    }

    // Implementing ControlValueAccessor methods
    writeValue(value: any): void {
        if (value) {
            this.innerValue = value;
        }
    }

    registerOnChange(fn): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {}

    // TODO: need to make this global because will be use in a lot of places
    private handleClick($event): void {
        let clickedComponent = $event.target;
        let inside = false;
        do {
            if (clickedComponent === this.elementRef.nativeElement) {
                inside = true;
            }
            clickedComponent = clickedComponent.parentNode;
        } while (clickedComponent);

        if (!inside) {
            this.isOpen = false;
        }
    }
}

@Component({
    host: {
        '(click)': 'onClick($event)',
    },
    moduleId: __moduleName,
    selector: 'dot-option',
    styleUrls: ['dot-option.css'],
    templateUrl: ['dot-option.html']
})

export class DotOption {
    @Input() value: string;
    @Input() selected: boolean;
    public node: ElementRef;
    public text: string;

    constructor(private select: DotSelect, myElement: ElementRef) {
        this.node = myElement;
    }

    ngAfterViewInit(): void {
        this.text = this.node.nativeElement.firstChild.innerHTML.trim();
        this.select.addOption(this);
    }

    onClick(): void {
        this.select.setOption({
            text: this.node.nativeElement.firstChild.innerHTML.trim(),
            value: this.value
        });
    }
}
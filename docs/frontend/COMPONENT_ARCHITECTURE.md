# Angular Component Architecture

**Feature state**: Prefer NgRx Signal Store; see [STATE_MANAGEMENT.md](./STATE_MANAGEMENT.md). This doc focuses on component structure, file layout, and data flow.

All code examples in this document follow [ANGULAR_STANDARDS.md](./ANGULAR_STANDARDS.md): signals use the `$` prefix (e.g. `$loading`, `$items`), observables use the `$` suffix (e.g. `vm$`), no `standalone: true` in decorators, and `ChangeDetectionStrategy.OnPush` where applicable.

## Standalone Component Pattern (Required)

### Component Structure
```typescript
@Component({
  selector: 'dot-my-feature',
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './dot-my-feature.component.html',
  styleUrls: ['./dot-my-feature.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotMyFeatureComponent {
  private readonly myService = inject(MyService);
  private readonly destroyRef = inject(DestroyRef);

  // Input signals ($ prefix per ANGULAR_STANDARDS)
  readonly $config = input<MyFeatureConfig>();
  readonly $filter = input<string>('');

  // Output signals ($ prefix)
  readonly $itemSelected = output<MyItem>();
  readonly $itemsChanged = output<MyItem[]>();

  // State signals ($ prefix)
  readonly $loading = signal(false);
  readonly $error = signal<string | null>(null);
  readonly $items = signal<MyItem[]>([]);
  readonly $selectedId = signal<string | null>(null);

  // Computed signals ($ prefix)
  readonly $filteredItems = computed(() => {
    const filterValue = this.$filter().toLowerCase();
    return this.$items().filter(item =>
      item.name.toLowerCase().includes(filterValue)
    );
  });

  readonly $hasSelection = computed(() => this.$selectedId() !== null);

  // Effects
  readonly loadItemsEffect = effect(() => {
    const config = this.$config();
    if (config) {
      this.loadItems();
    }
  });

  selectItem(item: MyItem): void {
    this.$selectedId.set(item.id);
    this.$itemSelected.emit(item);
  }

  private loadItems(): void {
    this.$loading.set(true);
    this.$error.set(null);

    this.myService.getItems(this.$config()!)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (items) => {
          this.$items.set(items);
          this.$loading.set(false);
          this.$itemsChanged.emit(items);
        },
        error: (err) => {
          this.$error.set('Failed to load items');
          this.$loading.set(false);
          console.error('Error loading items:', err);
        }
      });
  }
}
```

Template (`dot-my-feature.component.html`) — use `$` prefix when reading signals:
```html
<div class="dot-my-feature">
  @if ($loading()) {
    <dot-loading-indicator data-testid="loading-indicator" />
  }

  @if ($error()) {
    <dot-error-message [message]="$error()!" />
  }

  @if ($items().length > 0) {
    <div class="dot-my-feature__list">
      @for (item of $items(); track item.id) {
        <div
          class="dot-my-feature__item"
          [class.dot-my-feature__item--active]="item.id === $selectedId()"
          [data-testid]="'item-' + item.id"
          (click)="selectItem(item)"
        >
          {{ item.name }}
        </div>
      }
    </div>
  } @else {
    <dot-empty-state
      message="No items found"
      [data-testid]="'empty-state'"
    />
  }
</div>
```

### Component Interface Definition
```typescript
// my-feature-config.interface.ts
export interface MyFeatureConfig {
    readonly apiEndpoint: string;
    readonly pageSize: number;
    readonly enableSelection: boolean;
    readonly sortBy: 'name' | 'date' | 'priority';
}

// my-item.interface.ts
export interface MyItem {
    readonly id: string;
    readonly name: string;
    readonly description: string;
    readonly createdDate: Date;
    readonly status: 'active' | 'inactive' | 'pending';
}
```

## Signal-Based State Management

### State Signals Pattern
```typescript
@Component({
  selector: 'dot-form-component',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './dot-form-component.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly myService = inject(MyService);
  private readonly destroyRef = inject(DestroyRef);

  readonly $data = input<MyItem | null>(null);
  readonly $saved = output<MyItem>();
  readonly $cancelled = output<void>();

  // Form state
  readonly formGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(1000)]]
  });

  // Form validation signals ($ prefix)
  readonly $nameError = computed(() => {
    const control = this.formGroup.get('name');
    if (control?.errors && control.touched) {
      if (control.errors['required']) return 'Name is required';
      if (control.errors['maxlength']) return 'Name must be 100 characters or less';
    }
    return null;
  });

  readonly $descriptionError = computed(() => {
    const control = this.formGroup.get('description');
    if (control?.errors && control.touched) {
      if (control.errors['maxlength']) return 'Description must be 1000 characters or less';
    }
    return null;
  });

  readonly $formValid = computed(() => this.formGroup.valid);

  // Loading state ($ prefix)
  readonly $saving = signal(false);

  // Initialize form with data
  readonly initFormEffect = effect(() => {
    const data = this.$data();
    if (data) {
      this.formGroup.patchValue({
        name: data.name,
        description: data.description
      });
    }
  });

  onSubmit(): void {
    if (this.formGroup.valid) {
      this.$saving.set(true);

      const formValue = this.formGroup.value;
      const item: MyItem = {
        id: this.$data()?.id || generateId(),
        name: formValue.name!,
        description: formValue.description || '',
        createdDate: new Date(),
        status: 'active'
      };

      this.myService.saveItem(item)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (savedItem) => {
            this.$saving.set(false);
            this.$saved.emit(savedItem);
          },
          error: (err) => {
            this.$saving.set(false);
            console.error('Error saving item:', err);
          }
        });
    }
  }
}
```

Template (`dot-form-component.component.html`):
```html
<form [formGroup]="formGroup" (ngSubmit)="onSubmit()">
  <dot-input-field
    label="Name"
    formControlName="name"
    [data-testid]="'name-input'"
    [error]="$nameError()"
  />

  <dot-textarea-field
    label="Description"
    formControlName="description"
    [data-testid]="'description-input'"
    [error]="$descriptionError()"
  />

  <dot-button
    type="submit"
    [disabled]="!$formValid() || $saving()"
    [data-testid]="'submit-button'"
  >
    @if ($saving()) {
      Saving...
    } @else {
      Save
    }
  </dot-button>
</form>
```

### Service Integration Pattern
```typescript
@Injectable({
  providedIn: 'root'
})
export class MyService {
  private readonly http = inject(HttpClient);

  // State signals ($ prefix per ANGULAR_STANDARDS)
  readonly $items = signal<MyItem[]>([]);
  readonly $loading = signal(false);
  readonly $error = signal<string | null>(null);

  // Computed signals ($ prefix)
  readonly $itemCount = computed(() => this.$items().length);
  readonly $hasItems = computed(() => this.$items().length > 0);

  getItems(config: MyFeatureConfig): Observable<MyItem[]> {
    return this.http.get<MyItem[]>(`${config.apiEndpoint}/items`).pipe(
      tap(items => this.$items.set(items)),
      catchError(() => {
        this.$error.set('Failed to load items');
        return throwError(() => new Error('Failed to load items'));
      })
    );
  }

  saveItem(item: MyItem): Observable<MyItem> {
    const request = item.id
      ? this.http.put<MyItem>(`/api/v1/items/${item.id}`, item)
      : this.http.post<MyItem>('/api/v1/items', item);

    return request.pipe(
      tap(savedItem => {
        this.$items.update(items => {
          const index = items.findIndex(i => i.id === savedItem.id);
          if (index >= 0) {
            return items.map(i => (i.id === savedItem.id ? savedItem : i));
          }
          return [...items, savedItem];
        });
      }),
      catchError(() => {
        this.$error.set('Failed to save item');
        return throwError(() => new Error('Failed to save item'));
      })
    );
  }

  deleteItem(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/items/${id}`).pipe(
      tap(() => {
        this.$items.update(items => items.filter(i => i.id !== id));
      }),
      catchError(() => {
        this.$error.set('Failed to delete item');
        return throwError(() => new Error('Failed to delete item'));
      })
    );
  }
}
```

## Data Flow Patterns

### Parent-Child Communication
```typescript
// Parent component
@Component({
  selector: 'dot-feature-container',
  imports: [CommonModule],
  templateUrl: './dot-feature-container.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotFeatureContainerComponent {
  private readonly featureService = inject(FeatureService);

  // State signals ($ prefix)
  readonly $items = signal<MyItem[]>([]);
  readonly $loading = signal(false);
  readonly $selectedId = signal<string | null>(null);

  // Computed signals ($ prefix)
  readonly $title = computed(() => `My Feature (${this.$items().length})`);
  readonly $showActions = computed(() => this.$items().length > 0);
  readonly $selectedItem = computed(() =>
    this.$items().find(item => item.id === this.$selectedId()) ?? null
  );
  readonly $showDetails = computed(() => this.$selectedItem() !== null);

  onItemSelected(item: MyItem): void {
    this.$selectedId.set(item.id);
  }

  onItemDeleted(id: string): void {
    this.featureService.deleteItem(id).subscribe();
    if (this.$selectedId() === id) {
      this.$selectedId.set(null);
    }
  }

  onItemUpdated(item: MyItem): void {
    this.featureService.saveItem(item).subscribe();
  }

  onDetailsClosed(): void {
    this.$selectedId.set(null);
  }

  handleAction(action: string): void {
    switch (action) {
      case 'refresh':
        this.loadItems();
        break;
      case 'add':
        this.addNewItem();
        break;
    }
  }
}
```

Template — bind to signals with `$` prefix:
```html
<div class="dot-feature-container">
  <dot-feature-header
    [title]="$title()"
    [showActions]="$showActions()"
    (actionClicked)="handleAction($event)"
  />

  <dot-feature-list
    [items]="$items()"
    [loading]="$loading()"
    [selectedId]="$selectedId()"
    (itemSelected)="onItemSelected($event)"
    (itemDeleted)="onItemDeleted($event)"
  />

  @if ($showDetails()) {
    <dot-feature-details
      [item]="$selectedItem()"
      (itemUpdated)="onItemUpdated($event)"
      (closed)="onDetailsClosed()"
    />
  }
</div>
```

### Event Handling Pattern
```typescript
@Component({
  selector: 'dot-interactive-component',
  imports: [CommonModule],
  templateUrl: './dot-interactive-component.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotInteractiveComponent {
  // Search state ($ prefix)
  readonly $searchTerm = signal('');
  readonly $selectedFilter = signal<string>('all');
  readonly $sortBy = signal<'name' | 'date'>('name');
  readonly $sortDirection = signal<'asc' | 'desc'>('asc');

  // Data ($ prefix)
  readonly $items = signal<MyItem[]>([]);
  readonly $filterOptions = signal<FilterOption[]>([
    { value: 'all', label: 'All Items' },
    { value: 'active', label: 'Active' },
    { value: 'inactive', label: 'Inactive' }
  ]);

  // Output ($ prefix)
  readonly $itemSelected = output<MyItem>();

  // Computed ($ prefix)
  readonly $filteredAndSortedItems = computed(() => {
    let filtered = this.$items();

    const search = this.$searchTerm().toLowerCase();
    if (search) {
      filtered = filtered.filter(item =>
        item.name.toLowerCase().includes(search) ||
        item.description.toLowerCase().includes(search)
      );
    }

    const filter = this.$selectedFilter();
    if (filter !== 'all') {
      filtered = filtered.filter(item => item.status === filter);
    }

    const sortBy = this.$sortBy();
    const direction = this.$sortDirection();

    return filtered.sort((a, b) => {
      let comparison = 0;
      if (sortBy === 'name') {
        comparison = a.name.localeCompare(b.name);
      } else if (sortBy === 'date') {
        comparison = a.createdDate.getTime() - b.createdDate.getTime();
      }
      return direction === 'asc' ? comparison : -comparison;
    });
  });

  onSearchChanged(term: string): void {
    this.$searchTerm.set(term);
  }

  onFilterChanged(filter: string): void {
    this.$selectedFilter.set(filter);
  }

  onSortChanged(sort: { by: 'name' | 'date'; direction: 'asc' | 'desc' }): void {
    this.$sortBy.set(sort.by);
    this.$sortDirection.set(sort.direction);
  }

  onItemClicked(item: MyItem): void {
    this.$itemSelected.emit(item);
  }

  onItemContextMenu(event: { item: MyItem; event: MouseEvent }): void {
    event.event.preventDefault();
    this.showContextMenu(event.item, event.event);
  }
}
```

Template — bind to signals with `$` prefix:
```html
<div class="dot-interactive-component">
  <dot-search-input
    [value]="$searchTerm()"
    (valueChanged)="onSearchChanged($event)"
    [data-testid]="'search-input'"
  />

  <dot-filter-dropdown
    [options]="$filterOptions()"
    [selected]="$selectedFilter()"
    (selectionChanged)="onFilterChanged($event)"
    [data-testid]="'filter-dropdown'"
  />

  <dot-sort-controls
    [sortBy]="$sortBy()"
    [sortDirection]="$sortDirection()"
    (sortChanged)="onSortChanged($event)"
    [data-testid]="'sort-controls'"
  />

  <dot-item-list
    [items]="$filteredAndSortedItems()"
    (itemClicked)="onItemClicked($event)"
    (itemContextMenu)="onItemContextMenu($event)"
  />
</div>
```

## Performance Patterns

### OnPush Change Detection
```typescript
@Component({
  selector: 'dot-optimized-component',
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dot-optimized-component.component.html'
})
export class DotOptimizedComponent {
  readonly $items = input<MyItem[]>();
  readonly $selectedId = input<string | null>();
  readonly $itemSelected = output<MyItem>();

  selectItem(item: MyItem): void {
    this.$itemSelected.emit(item);
  }
}
```

Template:
```html
<div class="dot-optimized-component">
  @for (item of $items(); track item.id) {
    <dot-item-card
      [item]="item"
      [selected]="$selectedId() === item.id"
      (clicked)="selectItem(item)"
    />
  }
</div>
```

### Lazy Loading Pattern
```typescript
@Component({
  selector: 'dot-lazy-container',
  imports: [CommonModule],
  templateUrl: './dot-lazy-container.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotLazyContainerComponent {
  readonly $activeTab = signal('overview');
  readonly tabs = [
    { id: 'overview', label: 'Overview' },
    { id: 'details', label: 'Details' },
    { id: 'settings', label: 'Settings' }
  ];

  onTabChanged(tabId: string): void {
    this.$activeTab.set(tabId);
  }
}
```

Template:
```html
<div class="dot-lazy-container">
  <dot-tab-navigation
    [tabs]="tabs"
    [activeTab]="$activeTab()"
    (tabChanged)="onTabChanged($event)"
  />

  <div class="dot-lazy-container__content">
    @switch ($activeTab()) {
      @case ('overview') {
        <dot-overview-tab />
      }
      @case ('details') {
        @defer (when $activeTab() === 'details') {
          <dot-details-tab />
        } @loading {
          <dot-loading-indicator />
        }
      }
      @case ('settings') {
        @defer (when $activeTab() === 'settings') {
          <dot-settings-tab />
        } @loading {
          <dot-loading-indicator />
        }
      }
    }
  </div>
</div>
```

## Testing Integration

### Component Testing Setup
```typescript
describe('DotMyFeatureComponent', () => {
  let spectator: Spectator<DotMyFeatureComponent>;
  let mockService: SpyObject<MyService>;

  const createComponent = createComponentFactory({
    component: DotMyFeatureComponent,
    imports: [CommonModule, DotTestingModule],
    providers: [
      mockProvider(MyService, {
        getItems: jest.fn().mockReturnValue(of(mockItems))
      })
    ]
  });

  beforeEach(() => {
    spectator = createComponent();
    mockService = spectator.inject(MyService) as SpyObject<MyService>;
  });

  it('should load items on config change', () => {
    const config: MyFeatureConfig = {
      apiEndpoint: '/api/v1/test',
      pageSize: 10,
      enableSelection: true,
      sortBy: 'name'
    };

    spectator.setInput('config', config); // input name is 'config' (Angular strips $ for binding)

    expect(mockService.getItems).toHaveBeenCalledWith(config);
    expect(spectator.component.$items()).toEqual(mockItems);
  });

  it('should handle item selection', () => {
    const item = mockItems[0];
    const itemSelectedSpy = jest.spyOn(spectator.component.$itemSelected, 'emit');

    spectator.click(byTestId(`item-${item.id}`));

    expect(spectator.component.$selectedId()).toBe(item.id);
    expect(itemSelectedSpy).toHaveBeenCalledWith(item);
  });

  it('should display loading state', () => {
    spectator.component.$loading.set(true);
    spectator.detectChanges();

    expect(spectator.query(byTestId('loading-indicator'))).toBeVisible();
  });

  it('should display empty state when no items', () => {
    spectator.component.$items.set([]);
    spectator.detectChanges();

    expect(spectator.query(byTestId('empty-state'))).toBeVisible();
  });
});
```

## See also
- [ANGULAR_STANDARDS.md](./ANGULAR_STANDARDS.md) — Syntax, signals, OnPush
- [STATE_MANAGEMENT.md](./STATE_MANAGEMENT.md) — NgRx Signal Store for feature state
- [TESTING_FRONTEND.md](./TESTING_FRONTEND.md) — Spectator, byTestId, setInput
- [docs/frontend/README.md](./README.md) — Index of all frontend docs

## Location Information
- **Components**: Located in `libs/ui/src/lib/` and `apps/dotcms-ui/src/app/`
- **Services**: Found in `libs/data-access/src/lib/`
- **Interfaces**: Located in `libs/dotcms-models/src/lib/`
- **Testing utilities**: Found in `libs/utils/src/lib/testing/`
- **Shared components**: Located in `libs/ui/src/lib/shared/`
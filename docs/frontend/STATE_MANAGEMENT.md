# State Management with Signals

## Signal-Based State Pattern (Required)

### Component State Management
```typescript
@Component({
    selector: 'dot-state-example',
    standalone: true,
    template: `
        <div class="dot-state-example">
            @if (loading()) {
                <dot-loading />
            }
            
            @if (error()) {
                <dot-error [message]="error()!" />
            }
            
            @if (data().length > 0) {
                <div class="items">
                    @for (item of filteredItems(); track item.id) {
                        <div [class.selected]="selectedId() === item.id">
                            {{ item.name }}
                        </div>
                    }
                </div>
            }
            
            <div class="summary">
                Total: {{ itemCount() }} | Selected: {{ selectedItem()?.name || 'None' }}
            </div>
        </div>
    `
})
export class DotStateExampleComponent {
    // Input signals
    readonly filter = input<string>('');
    readonly sortBy = input<'name' | 'date'>('name');
    
    // Output signals
    readonly itemSelected = output<Item>();
    readonly stateChanged = output<ComponentState>();
    
    // State signals
    readonly loading = signal(false);
    readonly error = signal<string | null>(null);
    readonly data = signal<Item[]>([]);
    readonly selectedId = signal<string | null>(null);
    
    // Computed signals
    readonly filteredItems = computed(() => {
        const filterValue = this.filter().toLowerCase();
        const sortBy = this.sortBy();
        
        return this.data()
            .filter(item => item.name.toLowerCase().includes(filterValue))
            .sort((a, b) => {
                if (sortBy === 'name') {
                    return a.name.localeCompare(b.name);
                } else {
                    return a.date.getTime() - b.date.getTime();
                }
            });
    });
    
    readonly itemCount = computed(() => this.filteredItems().length);
    
    readonly selectedItem = computed(() => {
        const id = this.selectedId();
        return id ? this.data().find(item => item.id === id) || null : null;
    });
    
    readonly componentState = computed(() => ({
        loading: this.loading(),
        error: this.error(),
        itemCount: this.itemCount(),
        hasSelection: this.selectedId() !== null
    }));
    
    // Effects
    readonly stateChangeEffect = effect(() => {
        // Emit state changes
        this.stateChanged.emit(this.componentState());
    });
    
    readonly dataLoadEffect = effect(() => {
        // React to filter changes
        const filter = this.filter();
        if (filter !== this.previousFilter) {
            this.loadData();
            this.previousFilter = filter;
        }
    });
    
    private previousFilter = '';
    
    selectItem(id: string): void {
        this.selectedId.set(id);
        const item = this.selectedItem();
        if (item) {
            this.itemSelected.emit(item);
        }
    }
    
    private loadData(): void {
        this.loading.set(true);
        this.error.set(null);
        
        // Simulate API call
        setTimeout(() => {
            this.data.set(mockData);
            this.loading.set(false);
        }, 1000);
    }
}
```

### Service State Management
```typescript
@Injectable({
    providedIn: 'root'
})
export class ItemStateService {
    // Private state signals
    private readonly _items = signal<Item[]>([]);
    private readonly _loading = signal(false);
    private readonly _error = signal<string | null>(null);
    private readonly _selectedId = signal<string | null>(null);
    private readonly _filters = signal<ItemFilters>({
        search: '',
        status: 'all',
        category: null
    });
    
    // Public readonly signals
    readonly items = this._items.asReadonly();
    readonly loading = this._loading.asReadonly();
    readonly error = this._error.asReadonly();
    readonly selectedId = this._selectedId.asReadonly();
    readonly filters = this._filters.asReadonly();
    
    // Computed signals
    readonly filteredItems = computed(() => {
        const items = this._items();
        const filters = this._filters();
        
        return items.filter(item => {
            // Search filter
            if (filters.search && !item.name.toLowerCase().includes(filters.search.toLowerCase())) {
                return false;
            }
            
            // Status filter
            if (filters.status !== 'all' && item.status !== filters.status) {
                return false;
            }
            
            // Category filter
            if (filters.category && item.category !== filters.category) {
                return false;
            }
            
            return true;
        });
    });
    
    readonly selectedItem = computed(() => {
        const id = this._selectedId();
        return id ? this._items().find(item => item.id === id) || null : null;
    });
    
    readonly itemCount = computed(() => this.filteredItems().length);
    
    readonly hasItems = computed(() => this._items().length > 0);
    
    // Actions
    loadItems(): void {
        this._loading.set(true);
        this._error.set(null);
        
        this.http.get<Item[]>('/api/v1/items')
            .pipe(
                catchError(error => {
                    this._error.set('Failed to load items');
                    this._loading.set(false);
                    return throwError(() => error);
                })
            )
            .subscribe(items => {
                this._items.set(items);
                this._loading.set(false);
            });
    }
    
    addItem(item: CreateItemRequest): void {
        this.http.post<Item>('/api/v1/items', item)
            .subscribe(newItem => {
                this._items.update(items => [...items, newItem]);
            });
    }
    
    updateItem(id: string, updates: Partial<Item>): void {
        this.http.put<Item>(`/api/v1/items/${id}`, updates)
            .subscribe(updatedItem => {
                this._items.update(items => 
                    items.map(item => item.id === id ? updatedItem : item)
                );
            });
    }
    
    deleteItem(id: string): void {
        this.http.delete(`/api/v1/items/${id}`)
            .subscribe(() => {
                this._items.update(items => items.filter(item => item.id !== id));
                
                // Clear selection if deleted item was selected
                if (this._selectedId() === id) {
                    this._selectedId.set(null);
                }
            });
    }
    
    selectItem(id: string): void {
        this._selectedId.set(id);
    }
    
    clearSelection(): void {
        this._selectedId.set(null);
    }
    
    updateFilters(filters: Partial<ItemFilters>): void {
        this._filters.update(current => ({ ...current, ...filters }));
    }
    
    resetFilters(): void {
        this._filters.set({
            search: '',
            status: 'all',
            category: null
        });
    }
}
```

## Advanced State Patterns

### State Normalization
```typescript
@Injectable({
    providedIn: 'root'
})
export class NormalizedStateService {
    // Normalized state structure
    private readonly _entities = signal<Record<string, Item>>({});
    private readonly _entityIds = signal<string[]>([]);
    private readonly _loading = signal(false);
    private readonly _error = signal<string | null>(null);
    
    // Computed selectors
    readonly entities = computed(() => this._entities());
    readonly entityIds = computed(() => this._entityIds());
    readonly items = computed(() => 
        this._entityIds().map(id => this._entities()[id]).filter(Boolean)
    );
    
    // Entity selectors
    getEntity = (id: string) => computed(() => this._entities()[id] || null);
    
    // Actions
    loadItems(): void {
        this._loading.set(true);
        
        this.http.get<Item[]>('/api/v1/items')
            .subscribe(items => {
                // Normalize data
                const entities: Record<string, Item> = {};
                const ids: string[] = [];
                
                items.forEach(item => {
                    entities[item.id] = item;
                    ids.push(item.id);
                });
                
                this._entities.set(entities);
                this._entityIds.set(ids);
                this._loading.set(false);
            });
    }
    
    addItem(item: Item): void {
        this._entities.update(entities => ({
            ...entities,
            [item.id]: item
        }));
        
        this._entityIds.update(ids => [...ids, item.id]);
    }
    
    updateItem(id: string, updates: Partial<Item>): void {
        this._entities.update(entities => ({
            ...entities,
            [id]: { ...entities[id], ...updates }
        }));
    }
    
    removeItem(id: string): void {
        this._entities.update(entities => {
            const { [id]: removed, ...rest } = entities;
            return rest;
        });
        
        this._entityIds.update(ids => ids.filter(entityId => entityId !== id));
    }
}
```

### State Composition
```typescript
@Injectable({
    providedIn: 'root'
})
export class ComposedStateService {
    private readonly itemService = inject(ItemStateService);
    private readonly userService = inject(UserStateService);
    private readonly settingsService = inject(SettingsStateService);
    
    // Composed state
    readonly appState = computed(() => ({
        items: {
            items: this.itemService.items(),
            loading: this.itemService.loading(),
            error: this.itemService.error(),
            selectedId: this.itemService.selectedId()
        },
        user: {
            currentUser: this.userService.currentUser(),
            permissions: this.userService.permissions(),
            authenticated: this.userService.isAuthenticated()
        },
        settings: {
            theme: this.settingsService.theme(),
            language: this.settingsService.language(),
            notifications: this.settingsService.notifications()
        }
    }));
    
    // Derived state
    readonly canCreateItem = computed(() => {
        const user = this.userService.currentUser();
        const permissions = this.userService.permissions();
        return user && permissions.includes('create_items');
    });
    
    readonly isReady = computed(() => {
        return !this.itemService.loading() && 
               this.userService.isAuthenticated() &&
               this.settingsService.loaded();
    });
}
```

## State Persistence

### Local Storage State
```typescript
@Injectable({
    providedIn: 'root'
})
export class PersistentStateService {
    private readonly storageKey = 'app-state';
    
    // State signals
    private readonly _preferences = signal<UserPreferences>(this.loadFromStorage());
    readonly preferences = this._preferences.asReadonly();
    
    // Auto-save effect
    private readonly saveEffect = effect(() => {
        const preferences = this._preferences();
        this.saveToStorage(preferences);
    });
    
    updatePreferences(updates: Partial<UserPreferences>): void {
        this._preferences.update(current => ({ ...current, ...updates }));
    }
    
    private loadFromStorage(): UserPreferences {
        try {
            const stored = localStorage.getItem(this.storageKey);
            return stored ? JSON.parse(stored) : this.getDefaultPreferences();
        } catch {
            return this.getDefaultPreferences();
        }
    }
    
    private saveToStorage(preferences: UserPreferences): void {
        try {
            localStorage.setItem(this.storageKey, JSON.stringify(preferences));
        } catch (error) {
            console.error('Failed to save preferences:', error);
        }
    }
    
    private getDefaultPreferences(): UserPreferences {
        return {
            theme: 'light',
            language: 'en',
            pageSize: 20,
            autoSave: true
        };
    }
}
```

### Session State
```typescript
@Injectable({
    providedIn: 'root'
})
export class SessionStateService {
    // Session-specific state
    private readonly _sessionData = signal<SessionData>({
        startTime: new Date(),
        activeRoute: '',
        lastAction: null,
        breadcrumbs: []
    });
    
    readonly sessionData = this._sessionData.asReadonly();
    
    // Session duration
    readonly sessionDuration = computed(() => {
        const start = this._sessionData().startTime;
        return Date.now() - start.getTime();
    });
    
    // Navigation tracking
    updateRoute(route: string): void {
        this._sessionData.update(session => ({
            ...session,
            activeRoute: route,
            lastAction: {
                type: 'navigation',
                timestamp: new Date(),
                data: { route }
            }
        }));
    }
    
    // Breadcrumb management
    addBreadcrumb(breadcrumb: Breadcrumb): void {
        this._sessionData.update(session => ({
            ...session,
            breadcrumbs: [...session.breadcrumbs, breadcrumb].slice(-5) // Keep last 5
        }));
    }
    
    // Action tracking
    recordAction(action: UserAction): void {
        this._sessionData.update(session => ({
            ...session,
            lastAction: action
        }));
    }
}
```

## Testing State Management

### Signal Testing
```typescript
describe('ItemStateService', () => {
    let service: ItemStateService;
    let httpMock: jasmine.SpyObj<HttpClient>;
    
    beforeEach(() => {
        const spy = jasmine.createSpyObj('HttpClient', ['get', 'post', 'put', 'delete']);
        
        TestBed.configureTestingModule({
            providers: [
                ItemStateService,
                { provide: HttpClient, useValue: spy }
            ]
        });
        
        service = TestBed.inject(ItemStateService);
        httpMock = TestBed.inject(HttpClient) as jasmine.SpyObj<HttpClient>;
    });
    
    it('should initialize with empty state', () => {
        expect(service.items()).toEqual([]);
        expect(service.loading()).toBe(false);
        expect(service.error()).toBe(null);
    });
    
    it('should load items successfully', () => {
        const mockItems = [{ id: '1', name: 'Item 1' }];
        httpMock.get.and.returnValue(of(mockItems));
        
        service.loadItems();
        
        expect(service.loading()).toBe(false);
        expect(service.items()).toEqual(mockItems);
        expect(service.error()).toBe(null);
    });
    
    it('should handle load errors', () => {
        httpMock.get.and.returnValue(throwError(() => new Error('API Error')));
        
        service.loadItems();
        
        expect(service.loading()).toBe(false);
        expect(service.error()).toBe('Failed to load items');
    });
    
    it('should filter items correctly', () => {
        const items = [
            { id: '1', name: 'Apple', status: 'active' },
            { id: '2', name: 'Banana', status: 'inactive' }
        ];
        
        service._items.set(items);
        service.updateFilters({ search: 'app' });
        
        expect(service.filteredItems()).toEqual([items[0]]);
    });
});
```

### Component State Testing
```typescript
describe('DotStateExampleComponent', () => {
    let component: DotStateExampleComponent;
    let fixture: ComponentFixture<DotStateExampleComponent>;
    
    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [DotStateExampleComponent]
        });
        
        fixture = TestBed.createComponent(DotStateExampleComponent);
        component = fixture.componentInstance;
    });
    
    it('should compute filtered items correctly', () => {
        const items = [
            { id: '1', name: 'Apple', date: new Date('2023-01-01') },
            { id: '2', name: 'Banana', date: new Date('2023-01-02') }
        ];
        
        component.data.set(items);
        fixture.componentRef.setInput('filter', 'app');
        
        expect(component.filteredItems()).toEqual([items[0]]);
    });
    
    it('should handle item selection', () => {
        const items = [{ id: '1', name: 'Apple', date: new Date() }];
        component.data.set(items);
        
        spyOn(component.itemSelected, 'emit');
        
        component.selectItem('1');
        
        expect(component.selectedId()).toBe('1');
        expect(component.itemSelected.emit).toHaveBeenCalledWith(items[0]);
    });
});
```

## State Management Best Practices

### Signal Patterns
- **Use computed signals** for derived state
- **Use effects** for side effects and reactions
- **Keep state normalized** for complex data structures
- **Use readonly signals** for public API
- **Batch updates** when possible

### Performance Optimization
- **Minimize signal dependencies** in computed signals
- **Use trackBy** functions in templates
- **Implement OnPush** change detection strategy
- **Debounce frequent updates** where appropriate

### Testing Strategy
- **Test state transitions** not implementation details
- **Use signal utilities** for testing
- **Mock external dependencies** (HTTP, services)
- **Test computed signal results** directly

## Location Information
- **State services**: Located in `libs/data-access/src/lib/state/`
- **Component state**: Found in component files with `.component.ts` suffix
- **State models**: Located in `libs/dotcms-models/src/lib/state/`
- **State utilities**: Found in `libs/utils/src/lib/state/`
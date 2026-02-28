# State Management

State management in the dotCMS frontend uses **NgRx Signal Store**. Do not manage state manually with raw `signal()` / `computed()` / `effect()` in components or services for feature-level state. Follow [ANGULAR_STANDARDS.md](./ANGULAR_STANDARDS.md) (e.g. `$` prefix for local signals in components) and [TYPESCRIPT_STANDARDS.md](./TYPESCRIPT_STANDARDS.md) (strict types, `#` for private, `as const`).

## Required: NgRx Signal Store

Use **@ngrx/signals** `signalStore` with `withState`, `withComputed`, and `withMethods`. Use `rxMethod` for async flows (HTTP, debounce) and `tapResponse` for success/error/finalize.

### Example: Feature store

```typescript
import { computed, inject } from '@angular/core';
import { debounceTime, distinctUntilChanged, pipe, switchMap, tap } from 'rxjs';
import {
  patchState,
  signalStore,
  withComputed,
  withMethods,
  withState,
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { tapResponse } from '@ngrx/operators';
import { BooksService } from './books-service';
import { Book } from './book';

type BookSearchState = {
  books: Book[];
  isLoading: boolean;
  filter: { query: string; order: 'asc' | 'desc' };
};

const initialState: BookSearchState = {
  books: [],
  isLoading: false,
  filter: { query: '', order: 'asc' },
};

export const BookSearchStore = signalStore(
  withState(initialState),
  withComputed(({ books, filter }) => ({
    booksCount: computed(() => books().length),
    sortedBooks: computed(() => {
      const direction = filter().order === 'asc' ? 1 : -1;
      return books().toSorted((a, b) =>
        direction * a.title.localeCompare(b.title)
      );
    }),
  })),
  withMethods((store, booksService = inject(BooksService)) => ({
    updateQuery(query: string): void {
      patchState(store, (state) => ({ filter: { ...state.filter, query } }));
    },
    updateOrder(order: 'asc' | 'desc'): void {
      patchState(store, (state) => ({ filter: { ...state.filter, order } }));
    },
    loadByQuery: rxMethod<string>(
      pipe(
        debounceTime(300),
        distinctUntilChanged(),
        tap(() => patchState(store, { isLoading: true })),
        switchMap((query) =>
          booksService.getByQuery(query).pipe(
            tapResponse({
              next: (books) => patchState(store, { books }),
              error: console.error,
              finalize: () => patchState(store, { isLoading: false }),
            })
          )
        )
      )
    ),
  }))
);
```

### Component using the store

Components inject the store and use its signals and methods. Keep component-local signals (if any) with the `$` prefix per [ANGULAR_STANDARDS.md](./ANGULAR_STANDARDS.md).

```typescript
import { Component, inject } from '@angular/core';
import { BookSearchStore } from './book-search.store';

@Component({
  selector: 'dot-book-search',
  imports: [CommonModule],
  templateUrl: './book-search.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [BookSearchStore],
})
export class BookSearchComponent {
  readonly store = inject(BookSearchStore);

  onQueryChange(query: string): void {
    this.store.updateQuery(query);
    this.store.loadByQuery(query);
  }

  onOrderChange(order: 'asc' | 'desc'): void {
    this.store.updateOrder(order);
  }
}
```

Template: call store signals (they are already signals).

```html
<div class="dot-book-search">
  @if (store.isLoading()) {
    <dot-loading data-testid="loading" />
  }
  @if (store.books().length > 0) {
    <ul>
      @for (book of store.sortedBooks(); track book.id) {
        <li [data-testid]="'book-' + book.id">{{ book.title }}</li>
      }
    </ul>
    <p>Total: {{ store.booksCount() }}</p>
  }
</div>
```

## Avoid: Manual state in components or services

Do not build feature state with multiple `signal()` / `computed()` / `effect()` and manual `set()` / `update()` in components or injectable services.

**Avoid (manual component state):**
```typescript
// ❌ Too much manual state in one place
export class DotStateExampleComponent {
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly data = signal<Item[]>([]);
  readonly selectedId = signal<string | null>(null);
  readonly filteredItems = computed(() => /* ... */);
  // ... effects that call set()/update(), subscribe() in loadData(), etc.
}
```

**Prefer:** a Signal Store (as above) and a thin component that injects the store and calls its methods.

**Avoid (manual service state with asReadonly):**
```typescript
// ❌ Manual service state
@Injectable({ providedIn: 'root' })
export class ItemStateService {
  private readonly _items = signal<Item[]>([]);
  readonly items = this._items.asReadonly();
  loadItems(): void {
    this._items.set(/* ... */);
  }
}
```

**Prefer:** `signalStore` + `withState` + `withMethods` (and `rxMethod` for HTTP).

## Store patterns

### Immutable updates with `patchState`

Always update state immutably. Use `patchState(store, ...)`; do not mutate the state object.

```typescript
patchState(store, (state) => ({ filter: { ...state.filter, query } }));
patchState(store, { books: newBooks, isLoading: false });
```

### Async and HTTP with `rxMethod` and `tapResponse`

Use `rxMethod` for reactive flows (e.g. query → debounce → HTTP). Use `tapResponse` for `next` / `error` / `finalize` so loading and error state stay in sync.

```typescript
loadByQuery: rxMethod<string>(
  pipe(
    debounceTime(300),
    distinctUntilChanged(),
    tap(() => patchState(store, { isLoading: true })),
    switchMap((query) =>
      booksService.getByQuery(query).pipe(
        tapResponse({
          next: (books) => patchState(store, { books }),
          error: (err) => patchState(store, { error: err.message }),
          finalize: () => patchState(store, { isLoading: false }),
        })
      )
    )
  )
),
```

### Typed state with `as const` (TypeScript)

Use [TYPESCRIPT_STANDARDS.md](./TYPESCRIPT_STANDARDS.md): strict types, `as const` for constant shapes (e.g. filter order).

```typescript
const FilterOrder = { asc: 'asc', desc: 'desc' } as const;
type FilterOrder = (typeof FilterOrder)[keyof typeof FilterOrder];
```

### Private store references

Stores are usually provided at component or route level. If a service holds a store reference, use `#` for private fields per TypeScript standards.

```typescript
export class SomeFacade {
  #store = inject(BookSearchStore);
}
```

## Persistence (e.g. localStorage)

Use `withHooks` and an `effect` to sync store state to storage; on init, read from storage and `patchState`. Keep persistence logic inside the store or a small wrapper.

```typescript
export const PersistentBookSearchStore = signalStore(
  withState(initialState),
  withComputed(/* ... */),
  withMethods(/* ... */),
  withHooks({
    onInit(store) {
      const saved = loadFromStorage();
      if (saved) patchState(store, saved);
      effect(() => {
        saveToStorage(store.filter());
      });
    },
  })
);
```

## Testing

### Testing the store

Use Spectator or TestBed; provide the store and any dependencies (e.g. `BooksService`). Assert state after calling store methods and advancing async work.

```typescript
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { BookSearchStore } from './book-search.store';
import { BookSearchComponent } from './book-search.component';

describe('BookSearchStore', () => {
  it('updates filter and sortedBooks when updateOrder is called', () => {
    const fixture = TestBed.createComponent(BookSearchComponent);
    const store = fixture.debugElement.injector.get(BookSearchStore);
    patchState(store, { books: [/* ... */] });
    store.updateOrder('desc');
    fixture.detectChanges();
    expect(store.filter().order).toBe('desc');
    expect(store.sortedBooks()[0].title).toBe(/* expected first */);
  });
});
```

### Testing the component (Spectator)

Follow [ANGULAR_STANDARDS.md](./ANGULAR_STANDARDS.md) and [TESTING_FRONTEND.md](./TESTING_FRONTEND.md): use `byTestId`, `spectator.setInput()`, and test user-visible behavior.

```typescript
const createComponent = createComponentFactory({
  component: BookSearchComponent,
  imports: [CommonModule],
  providers: [BookSearchStore],
  mocks: [BooksService],
});

it('shows loading then books when query is entered', () => {
  spectator = createComponent();
  spectator.typeInElement('angular', byTestId('search-input'));
  // ... assert loading then list with store.books()
});
```

## Best practices

- **Use NgRx Signal Store** for feature/domain state; avoid large manual signal/computed/effect blocks in components or services.
- **Use `patchState`** for all store updates; keep updates immutable.
- **Use `rxMethod` + `tapResponse`** for async/HTTP so loading and error state stay consistent.
- **Keep components thin**: inject the store, call methods, bind to store signals in the template.
- **Apply ANGULAR_STANDARDS**: `$` prefix for signals declared in the component; OnPush; `inject()`; separate template/style files when not trivial.
- **Apply TYPESCRIPT_STANDARDS**: strict types, no `any`, `#` for private fields, `as const` instead of enums.

## Resources

- [NgRx Signal Store](https://ngrx.io/guide/signals)
- [ANGULAR_STANDARDS.md](./ANGULAR_STANDARDS.md)
- [TYPESCRIPT_STANDARDS.md](./TYPESCRIPT_STANDARDS.md)
- [TESTING_FRONTEND.md](./TESTING_FRONTEND.md)
- [README.md](./README.md) — Index of all frontend docs

## Location information

- **Stores**: `libs/data-access/src/lib/` (e.g. `stores/`, `feature-name.store.ts`)
- **State models/types**: `libs/dotcms-models/src/lib/`

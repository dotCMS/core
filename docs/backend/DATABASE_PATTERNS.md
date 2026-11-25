# Database Access Patterns

## DotConnect Pattern (Required)

### Basic Query Pattern
```java
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;

public class MyDataAccess {
    
    public List<Map<String, Object>> findByStatus(String status) throws DotDataException {
        DotConnect dotConnect = new DotConnect();
        
        return dotConnect
            .setSQL("SELECT id, name, status FROM my_table WHERE status = ?")
            .addParam(status)
            .loadResults();
    }
    
    public Optional<Map<String, Object>> findById(String id) throws DotDataException {
        DotConnect dotConnect = new DotConnect();
        
        List<Map<String, Object>> results = dotConnect
            .setSQL("SELECT * FROM my_table WHERE id = ?")
            .addParam(id)
            .loadResults();
            
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
```

### Transaction Pattern (Required)
```java
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.LocalTransaction;

public class MyTransactionalService {
    
    public MyEntity createEntity(MyEntityForm form) throws DotDataException {
        return LocalTransaction.wrapReturn(() -> {
            // Insert main entity
            DotConnect dotConnect = new DotConnect();
            String id = UUIDGenerator.generateUuid();
            
            dotConnect
                .setSQL("INSERT INTO my_table (id, name, description, created_date) VALUES (?, ?, ?, ?)")
                .addParam(id)
                .addParam(form.getName())
                .addParam(form.getDescription())
                .addParam(new Date())
                .executeUpdate();
            
            // Insert related data
            for (String tag : form.getTags()) {
                dotConnect
                    .setSQL("INSERT INTO my_table_tags (entity_id, tag) VALUES (?, ?)")
                    .addParam(id)
                    .addParam(tag)
                    .executeUpdate();
            }
            
            return findById(id);
        });
    }
    
    public void deleteEntity(String id) throws DotDataException {
        LocalTransaction.wrapReturnWithListeners(() -> {
            DotConnect dotConnect = new DotConnect();
            
            // Delete related data first
            dotConnect
                .setSQL("DELETE FROM my_table_tags WHERE entity_id = ?")
                .addParam(id)
                .executeUpdate();
            
            // Delete main entity
            int deletedRows = dotConnect
                .setSQL("DELETE FROM my_table WHERE id = ?")
                .addParam(id)
                .executeUpdate();
                
            if (deletedRows == 0) {
                throw new DotDataException("Entity not found: " + id);
            }
            
            return null;
        });
    }
}
```

## Parameter Binding (Critical)

### Safe Parameter Binding
```java
// ✅ ALWAYS use parameterized queries
DotConnect dotConnect = new DotConnect();
List<Map<String, Object>> results = dotConnect
    .setSQL("SELECT * FROM my_table WHERE name = ? AND status = ?")
    .addParam(name)
    .addParam(status)
    .loadResults();

// ✅ Handle null parameters
dotConnect
    .setSQL("SELECT * FROM my_table WHERE (? IS NULL OR category = ?)")
    .addParam(category)
    .addParam(category)
    .loadResults();

// ❌ NEVER use string concatenation (SQL injection risk)
String sql = "SELECT * FROM my_table WHERE name = '" + name + "'";
```

### Parameter Type Handling
```java
// String parameters
dotConnect.addParam("string_value");

// Numeric parameters
dotConnect.addParam(123);
dotConnect.addParam(123.45);

// Boolean parameters
dotConnect.addParam(true);

// Date parameters
dotConnect.addParam(new Date());
dotConnect.addParam(new java.sql.Timestamp(System.currentTimeMillis()));

// Null parameters
dotConnect.addParam(null);
```

## Query Patterns

### Pagination Pattern
```java
public PaginationResult<MyEntity> findPaginated(MyEntityQuery query, User user) throws DotDataException {
    // Count total records
    DotConnect countConnect = new DotConnect();
    int totalCount = countConnect
        .setSQL("SELECT COUNT(*) as total FROM my_table WHERE status = ?")
        .addParam(query.getStatus())
        .getInt("total");
    
    // Get paginated results
    DotConnect dataConnect = new DotConnect();
    List<Map<String, Object>> results = dataConnect
        .setSQL("SELECT * FROM my_table WHERE status = ? ORDER BY created_date DESC LIMIT ? OFFSET ?")
        .addParam(query.getStatus())
        .addParam(query.getPerPage())
        .addParam((query.getPage() - 1) * query.getPerPage())
        .loadResults();
    
    // Convert to entities
    List<MyEntity> entities = results.stream()
        .map(this::mapToEntity)
        .collect(Collectors.toList());
    
    return new PaginationResult<>(entities, totalCount, query.getPage(), query.getPerPage());
}
```

### Dynamic Query Building
```java
public List<MyEntity> findByFilters(MyEntityFilters filters) throws DotDataException {
    StringBuilder sql = new StringBuilder("SELECT * FROM my_table WHERE 1=1");
    DotConnect dotConnect = new DotConnect();
    
    // Dynamic filter conditions
    if (UtilMethods.isSet(filters.getName())) {
        sql.append(" AND name ILIKE ?");
        dotConnect.addParam("%" + filters.getName() + "%");
    }
    
    if (UtilMethods.isSet(filters.getStatus())) {
        sql.append(" AND status = ?");
        dotConnect.addParam(filters.getStatus());
    }
    
    if (filters.getCreatedAfter() != null) {
        sql.append(" AND created_date >= ?");
        dotConnect.addParam(filters.getCreatedAfter());
    }
    
    // Add ordering
    sql.append(" ORDER BY created_date DESC");
    
    List<Map<String, Object>> results = dotConnect
        .setSQL(sql.toString())
        .loadResults();
    
    return results.stream()
        .map(this::mapToEntity)
        .collect(Collectors.toList());
}
```

### Batch Operations
```java
public void batchInsert(List<MyEntity> entities) throws DotDataException {
    if (entities.isEmpty()) return;
    
    LocalTransaction.wrapReturnWithListeners(() -> {
        DotConnect dotConnect = new DotConnect();
        
        // Prepare batch insert
        StringBuilder sql = new StringBuilder("INSERT INTO my_table (id, name, status) VALUES ");
        
        for (int i = 0; i < entities.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("(?, ?, ?)");
        }
        
        dotConnect.setSQL(sql.toString());
        
        // Add parameters for each entity
        for (MyEntity entity : entities) {
            dotConnect
                .addParam(entity.getId())
                .addParam(entity.getName())
                .addParam(entity.getStatus());
        }
        
        dotConnect.executeUpdate();
        return null;
    });
}
```

## Result Mapping Patterns

### Map to Entity Conversion
```java
private MyEntity mapToEntity(Map<String, Object> row) {
    return MyEntity.builder()
        .id((String) row.get("id"))
        .name((String) row.get("name"))
        .description((String) row.get("description"))
        .status((String) row.get("status"))
        .enabled((Boolean) row.get("enabled"))
        .createdDate((Date) row.get("created_date"))
        .modifiedDate((Date) row.get("modified_date"))
        .build();
}

// Handle null values safely
private String getString(Map<String, Object> row, String key) {
    Object value = row.get(key);
    return value != null ? value.toString() : null;
}

private Integer getInteger(Map<String, Object> row, String key) {
    Object value = row.get(key);
    return value != null ? ((Number) value).intValue() : null;
}
```

### Single Value Extraction
```java
// Get single string value
String name = dotConnect
    .setSQL("SELECT name FROM my_table WHERE id = ?")
    .addParam(id)
    .getString("name");

// Get single integer value
int count = dotConnect
    .setSQL("SELECT COUNT(*) as total FROM my_table")
    .getInt("total");

// Get single boolean value
boolean exists = dotConnect
    .setSQL("SELECT COUNT(*) > 0 as exists FROM my_table WHERE id = ?")
    .addParam(id)
    .getBoolean("exists");
```

## Connection Management

### Database Configuration
```java
// Database properties (in dotmarketing-config.properties)
db.driver=org.postgresql.Driver
db.url=jdbc:postgresql://localhost:5432/dotcms
db.username=dotcms
db.password=dotcms
db.max.connections=60
db.min.connections=5
```

### Connection Pool Usage
```java
// DotConnect automatically manages connections
// No need to manually open/close connections
DotConnect dotConnect = new DotConnect();
// Connection is automatically returned to pool after operation
```

## Performance Patterns

### Query Optimization
```java
// Use indexes effectively
dotConnect
    .setSQL("SELECT * FROM my_table WHERE indexed_column = ? AND status = ?")
    .addParam(value)
    .addParam(status)
    .loadResults();

// Limit result sets
dotConnect
    .setSQL("SELECT * FROM my_table WHERE status = ? LIMIT 100")
    .addParam(status)
    .loadResults();

// Use EXISTS instead of COUNT when checking existence
boolean exists = dotConnect
    .setSQL("SELECT EXISTS(SELECT 1 FROM my_table WHERE id = ?) as exists")
    .addParam(id)
    .getBoolean("exists");
```

### Caching Integration
```java
import com.dotmarketing.cache.CacheLocator;

public class MyEntityCache {
    
    private static final String CACHE_KEY_PREFIX = "MyEntity:";
    
    public MyEntity get(String id) {
        String cacheKey = CACHE_KEY_PREFIX + id;
        MyEntity cached = (MyEntity) CacheLocator.getCacheAdministrator()
            .get(cacheKey, "MyEntityCache");
            
        if (cached != null) {
            return cached;
        }
        
        // Load from database
        MyEntity entity = loadFromDatabase(id);
        
        // Cache result
        CacheLocator.getCacheAdministrator()
            .put(cacheKey, entity, "MyEntityCache");
            
        return entity;
    }
    
    public void remove(String id) {
        String cacheKey = CACHE_KEY_PREFIX + id;
        CacheLocator.getCacheAdministrator()
            .remove(cacheKey, "MyEntityCache");
    }
}
```

## Error Handling Patterns

### Database Exception Handling
```java
public MyEntity findById(String id) throws DotDataException {
    try {
        DotConnect dotConnect = new DotConnect();
        List<Map<String, Object>> results = dotConnect
            .setSQL("SELECT * FROM my_table WHERE id = ?")
            .addParam(id)
            .loadResults();
            
        if (results.isEmpty()) {
            throw new DotDataException("Entity not found: " + id);
        }
        
        return mapToEntity(results.get(0));
        
    } catch (SQLException e) {
        Logger.error(this, "Database error finding entity: " + e.getMessage(), e);
        throw new DotDataException("Database error: " + e.getMessage(), e);
    }
}
```

### Transaction Rollback
```java
public void complexOperation() throws DotDataException {
    try {
        LocalTransaction.wrapReturnWithListeners(() -> {
            // Multiple database operations
            insertMainEntity();
            insertRelatedData();
            updateCounters();
            
            // If any operation fails, entire transaction rolls back
            return null;
        });
    } catch (Exception e) {
        Logger.error(this, "Transaction failed: " + e.getMessage(), e);
        throw new DotDataException("Operation failed: " + e.getMessage(), e);
    }
}
```

## Commit Listeners

Work that must only happen if the transaction actually committed — updating a search index,
invalidating a cache on other nodes, firing an event, calling an external service — belongs in a
commit listener, not inline in the transaction. Inline, a rollback leaves the side effect behind:
an index carrying the mapping of a field that never existed, a cache primed with a row that was
never written.

### Sync or async — the choice that bites

```java
import com.dotmarketing.db.HibernateUtil;

// Async (default). Queued and run on another thread after the commit.
HibernateUtil.addCommitListener(() -> localSystemEventsAPI.notify(new FieldSavedEvent(field)));

// Sync. Runs at commit, on the committing thread, before the caller gets control back.
HibernateUtil.addSyncCommitListener(() -> indexAPI.addCustomMappingForNewField(field));
```

`addCommitListener(Runnable)` is **asynchronous whenever `ASYNC_COMMIT_LISTENERS` is on, which is
the default** (`HibernateUtil:1448`). The calling method therefore returns before the listener has
run. That is fine for a notification nobody waits on, and wrong whenever a later request could
observe the system in the half-finished state.

Use `addSyncCommitListener` when something else could act on the committed row before the side
effect lands. The field-mapping push is the worked example: with an async listener, `FieldAPI.save`
returns as soon as the field is committed, and content of that type indexed from another request in
that window gets **dynamically mapped first** — and a field that dynamic mapping already created
cannot be re-typed, so the explicit mapping is lost for good. See
`FieldAPIImpl.addSearchMappingForField` (issue #36577).

### What sync does not buy you

Three things to know before leaning on it:

- **The window narrows, it does not close.** The row is committed and visible to other connections
  before any listener runs, sync or not. Sync reduces the exposure to the duration of the side
  effect itself, on the same thread. Work that must be atomic with the row cannot be a commit
  listener at all.
- **A sync listener runs inside a fresh `LocalTransaction`** (`DotRunnableThread.run`), so it holds
  a pooled connection for as long as it runs. Do not put a slow or remote call in one without
  thinking about the pool — batch the work into a single listener rather than registering one per
  item.
- **An exception escaping a sync listener fails the caller's save**, as `DotHibernateException`,
  after the data is already committed: the caller sees a failure for a row that exists. A listener
  whose failure must not surface that way has to catch its own exceptions, the way
  `FieldAPIImpl.addSearchMappingForField` does.

### Testing them

`dotcms-integration/src/test/resources/dotmarketing-config.properties` sets
`ASYNC_COMMIT_LISTENERS=false`, so **integration tests never exercise the async path**. A listener
that only works because the test ran it synchronously will still pass CI and fail in production.
Decide sync or async by the ordering guarantee you need, never by what the tests show.

## Testing Patterns

### Database Integration Tests
```java
@Test
public void testFindById() throws DotDataException {
    // Arrange
    MyEntity entity = new MyEntityDataGen().nextPersisted();
    
    // Act
    MyEntity result = myDataAccess.findById(entity.getId());
    
    // Assert
    assertNotNull(result);
    assertEquals(entity.getId(), result.getId());
    assertEquals(entity.getName(), result.getName());
}

@Test
public void testTransactionRollback() throws DotDataException {
    // Arrange
    String id = UUIDGenerator.generateUuid();
    
    // Act & Assert
    assertThrows(DotDataException.class, () -> {
        LocalTransaction.wrapReturnWithListeners(() -> {
            // Insert entity
            insertEntity(id);
            
            // Simulate error that should cause rollback
            throw new RuntimeException("Simulated error");
        });
    });
    
    // Verify entity was not persisted due to rollback
    Optional<MyEntity> entity = myDataAccess.findById(id);
    assertFalse(entity.isPresent());
}
```

## Location Information
- **DotConnect**: Located in `com.dotmarketing.common.db.DotConnect`
- **LocalTransaction**: Located in `com.dotmarketing.db.LocalTransaction`
- **Database configuration**: Found in `dotmarketing-config.properties`
- **Data access classes**: Typically in `com.dotcms.*.business` packages
- **Cache integration**: Available via `com.dotmarketing.cache.CacheLocator`
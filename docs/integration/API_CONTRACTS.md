# API Contracts and Integration Patterns

## Frontend-Backend Integration

### REST API Consumption Pattern
```typescript
// Angular service consuming Java REST endpoint
@Injectable({
    providedIn: 'root'
})
export class MyEntityService {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = '/api/v1/my-entities';
    
    // GET /api/v1/my-entities
    getEntities(params?: MyEntityQuery): Observable<PaginationResult<MyEntity>> {
        const httpParams = new HttpParams()
            .set('page', params?.page?.toString() || '1')
            .set('per_page', params?.perPage?.toString() || '20')
            .set('filter', params?.filter || '')
            .set('orderBy', params?.orderBy || 'name');
            
        return this.http.get<PaginationResult<MyEntity>>(this.baseUrl, { params: httpParams });
    }
    
    // GET /api/v1/my-entities/{id}
    getEntity(id: string): Observable<MyEntity> {
        return this.http.get<MyEntity>(`${this.baseUrl}/${id}`);
    }
    
    // POST /api/v1/my-entities
    createEntity(entity: CreateMyEntityRequest): Observable<MyEntity> {
        return this.http.post<MyEntity>(this.baseUrl, entity);
    }
    
    // PUT /api/v1/my-entities/{id}
    updateEntity(id: string, entity: UpdateMyEntityRequest): Observable<MyEntity> {
        return this.http.put<MyEntity>(`${this.baseUrl}/${id}`, entity);
    }
    
    // DELETE /api/v1/my-entities/{id}
    deleteEntity(id: string): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/${id}`);
    }
}
```

### Java REST Endpoint Implementation
```java
@Path("/v1/my-entities")
@ApplicationScoped
public class MyEntityResource {
    private final WebResource webResource = new WebResource();
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response getEntities(
        @QueryParam("page") @DefaultValue("1") int page,
        @QueryParam("per_page") @DefaultValue("20") int perPage,
        @QueryParam("filter") String filter,
        @QueryParam("orderBy") @DefaultValue("name") String orderBy,
        @Context HttpServletRequest request
    ) {
        InitDataObject initData = webResource.init(request, response, true);
        User user = initData.getUser();
        
        try {
            MyEntityQuery query = MyEntityQuery.builder()
                .page(page)
                .perPage(perPage)
                .filter(filter)
                .orderBy(orderBy)
                .build();
                
            PaginationResult<MyEntity> result = myEntityService.findPaginated(query, user);
            return Response.ok(new ResponseEntityView<>(result)).build();
            
        } catch (Exception e) {
            return ResponseUtil.mapExceptionResponse(e);
        }
    }
    
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response getEntity(
        @PathParam("id") String id,
        @Context HttpServletRequest request
    ) {
        InitDataObject initData = webResource.init(request, response, true);
        User user = initData.getUser();
        
        try {
            MyEntity entity = myEntityService.findById(id, user);
            return Response.ok(new ResponseEntityView<>(entity)).build();
        } catch (Exception e) {
            return ResponseUtil.mapExceptionResponse(e);
        }
    }
}
```

## Data Model Consistency

### Shared Data Structures
```typescript
// TypeScript interface (frontend)
export interface MyEntity {
    readonly id: string;
    readonly name: string;
    readonly description: string;
    readonly status: 'active' | 'inactive' | 'pending';
    readonly createdDate: Date;
    readonly modifiedDate: Date;
    readonly createdBy: string;
    readonly modifiedBy: string;
}

export interface PaginationResult<T> {
    readonly items: T[];
    readonly totalCount: number;
    readonly page: number;
    readonly perPage: number;
    readonly totalPages: number;
}

export interface MyEntityQuery {
    readonly page?: number;
    readonly perPage?: number;
    readonly filter?: string;
    readonly orderBy?: 'name' | 'createdDate' | 'modifiedDate';
    readonly sortDirection?: 'asc' | 'desc';
}
```

```java
// Java entity (backend)
@Value.Immutable
@JsonSerialize(as = ImmutableMyEntity.class)
@JsonDeserialize(as = ImmutableMyEntity.class)
public abstract class MyEntity {
    public abstract String id();
    public abstract String name();
    public abstract Optional<String> description();
    public abstract MyEntityStatus status();
    public abstract Date createdDate();
    public abstract Date modifiedDate();
    public abstract String createdBy();
    public abstract String modifiedBy();
    
    public static Builder builder() {
        return ImmutableMyEntity.builder();
    }
}

public enum MyEntityStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    PENDING("pending");
    
    private final String value;
    
    MyEntityStatus(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
}
```

### Request/Response DTOs
```typescript
// Frontend request DTOs
export interface CreateMyEntityRequest {
    readonly name: string;
    readonly description?: string;
    readonly status?: 'active' | 'inactive';
}

export interface UpdateMyEntityRequest {
    readonly name?: string;
    readonly description?: string;
    readonly status?: 'active' | 'inactive';
}
```

```java
// Backend form objects
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateMyEntityForm {
    private String name;
    private String description;
    private MyEntityStatus status;
    
    // Getters and setters
    
    public boolean isValid() {
        return UtilMethods.isSet(name) && 
               name.length() <= 255 &&
               (description == null || description.length() <= 1000);
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateMyEntityForm {
    private String name;
    private String description;
    private MyEntityStatus status;
    
    // Getters and setters
    
    public boolean isValid() {
        return (name == null || (UtilMethods.isSet(name) && name.length() <= 255)) &&
               (description == null || description.length() <= 1000);
    }
}
```

## Authentication Integration

### Frontend Authentication Service
```typescript
@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private readonly http = inject(HttpClient);
    
    // Authentication signals
    readonly isAuthenticated = signal(false);
    readonly currentUser = signal<User | null>(null);
    readonly permissions = signal<string[]>([]);
    
    login(credentials: LoginCredentials): Observable<AuthResponse> {
        return this.http.post<AuthResponse>('/api/v1/auth/login', credentials)
            .pipe(
                tap(response => {
                    this.isAuthenticated.set(true);
                    this.currentUser.set(response.user);
                    this.permissions.set(response.permissions);
                    
                    // Store token for subsequent requests
                    localStorage.setItem('auth_token', response.token);
                })
            );
    }
    
    logout(): Observable<void> {
        return this.http.post<void>('/api/v1/auth/logout', {})
            .pipe(
                tap(() => {
                    this.isAuthenticated.set(false);
                    this.currentUser.set(null);
                    this.permissions.set([]);
                    localStorage.removeItem('auth_token');
                })
            );
    }
    
    hasPermission(permission: string): boolean {
        return this.permissions().includes(permission);
    }
}
```

### Backend Authentication Endpoint
```java
@Path("/v1/auth")
@ApplicationScoped
public class AuthResource {
    private final WebResource webResource = new WebResource();
    
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(
        @Context HttpServletRequest request,
        @Context HttpServletResponse response,
        LoginForm loginForm
    ) {
        try {
            // Validate credentials
            if (!loginForm.isValid()) {
                return ResponseUtil.mapExceptionResponse(
                    new DotDataException("Invalid credentials")
                );
            }
            
            // Authenticate user
            User user = userAPI.authenticate(loginForm.getUsername(), loginForm.getPassword());
            
            if (user == null) {
                return ResponseUtil.mapExceptionResponse(
                    new DotSecurityException("Authentication failed")
                );
            }
            
            // Generate token
            String token = jwtService.generateToken(user);
            
            // Get user permissions
            List<String> permissions = permissionAPI.getUserPermissions(user);
            
            // Build response
            AuthResponse authResponse = AuthResponse.builder()
                .user(user)
                .token(token)
                .permissions(permissions)
                .build();
                
            return Response.ok(new ResponseEntityView<>(authResponse)).build();
            
        } catch (Exception e) {
            return ResponseUtil.mapExceptionResponse(e);
        }
    }
}
```

## Error Handling Integration

### Frontend Error Handling
```typescript
@Injectable({
    providedIn: 'root'
})
export class ErrorHandlerService {
    private readonly notificationService = inject(NotificationService);
    
    handleHttpError(error: HttpErrorResponse): Observable<never> {
        let message: string;
        
        switch (error.status) {
            case 401:
                message = 'Authentication required';
                this.redirectToLogin();
                break;
            case 403:
                message = 'Access denied';
                break;
            case 404:
                message = 'Resource not found';
                break;
            case 500:
                message = 'Server error occurred';
                break;
            default:
                message = 'An unexpected error occurred';
        }
        
        this.notificationService.showError(message);
        return throwError(() => error);
    }
    
    private redirectToLogin(): void {
        // Redirect to login page
        window.location.href = '/login';
    }
}
```

### Backend Error Response Format
```java
@ApplicationScoped
public class ErrorResponseBuilder {
    
    public Response buildErrorResponse(Exception e) {
        ErrorResponse errorResponse;
        Response.Status status;
        
        if (e instanceof DotSecurityException) {
            status = Response.Status.FORBIDDEN;
            errorResponse = ErrorResponse.builder()
                .error("Access denied")
                .message("You don't have permission to access this resource")
                .timestamp(new Date())
                .build();
        } else if (e instanceof DotDataException) {
            status = Response.Status.BAD_REQUEST;
            errorResponse = ErrorResponse.builder()
                .error("Invalid request")
                .message(e.getMessage())
                .timestamp(new Date())
                .build();
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR;
            errorResponse = ErrorResponse.builder()
                .error("Internal server error")
                .message("An unexpected error occurred")
                .timestamp(new Date())
                .build();
        }
        
        return Response.status(status)
            .entity(new ResponseEntityView<>(errorResponse))
            .build();
    }
}
```

## WebSocket Integration

### Frontend WebSocket Service
```typescript
@Injectable({
    providedIn: 'root'
})
export class WebSocketService {
    private readonly socket$ = new Subject<any>();
    private websocket?: WebSocket;
    
    connect(url: string): void {
        this.websocket = new WebSocket(url);
        
        this.websocket.onmessage = (event) => {
            const data = JSON.parse(event.data);
            this.socket$.next(data);
        };
        
        this.websocket.onclose = () => {
            console.log('WebSocket connection closed');
        };
        
        this.websocket.onerror = (error) => {
            console.error('WebSocket error:', error);
        };
    }
    
    subscribe(eventType: string): Observable<any> {
        return this.socket$.pipe(
            filter(event => event.type === eventType),
            map(event => event.data)
        );
    }
    
    send(message: any): void {
        if (this.websocket?.readyState === WebSocket.OPEN) {
            this.websocket.send(JSON.stringify(message));
        }
    }
    
    disconnect(): void {
        if (this.websocket) {
            this.websocket.close();
        }
    }
}
```

### Backend WebSocket Handler
```java
@ServerEndpoint("/websocket/events")
@ApplicationScoped
public class EventWebSocketHandler {
    
    @OnOpen
    public void onOpen(Session session) {
        Logger.info(this, "WebSocket connection opened: " + session.getId());
    }
    
    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            // Parse message
            JsonObject messageObj = JsonParser.parseString(message).getAsJsonObject();
            String eventType = messageObj.get("type").getAsString();
            
            // Handle different event types
            switch (eventType) {
                case "subscribe":
                    handleSubscription(messageObj, session);
                    break;
                case "unsubscribe":
                    handleUnsubscription(messageObj, session);
                    break;
                default:
                    Logger.warn(this, "Unknown event type: " + eventType);
            }
            
        } catch (Exception e) {
            Logger.error(this, "Error handling WebSocket message: " + e.getMessage(), e);
        }
    }
    
    @OnClose
    public void onClose(Session session) {
        Logger.info(this, "WebSocket connection closed: " + session.getId());
    }
    
    @OnError
    public void onError(Session session, Throwable error) {
        Logger.error(this, "WebSocket error: " + error.getMessage(), error);
    }
    
    private void handleSubscription(JsonObject message, Session session) {
        // Handle subscription logic
    }
    
    private void handleUnsubscription(JsonObject message, Session session) {
        // Handle unsubscription logic
    }
}
```

## File Upload Integration

### Frontend File Upload Component
```typescript
@Component({
    selector: 'dot-file-upload',
    standalone: true,
    template: `
        <div class="dot-file-upload">
            <input 
                type="file"
                #fileInput
                (change)="onFileSelected($event)"
                [accept]="acceptedTypes"
                [multiple]="allowMultiple"
                [data-testid]="'file-input'"
            />
            
            <button 
                (click)="fileInput.click()"
                [disabled]="uploading()"
                [data-testid]="'select-file-button'"
            >
                Select File
            </button>
            
            @if (uploading()) {
                <div class="dot-file-upload__progress">
                    <progress [value]="uploadProgress()" max="100"></progress>
                    <span>{{ uploadProgress() }}%</span>
                </div>
            }
            
            @if (uploadedFiles().length > 0) {
                <div class="dot-file-upload__files">
                    @for (file of uploadedFiles(); track file.id) {
                        <div class="dot-file-upload__file" [data-testid]="'uploaded-file-' + file.id">
                            <span>{{ file.name }}</span>
                            <button (click)="removeFile(file.id)">Remove</button>
                        </div>
                    }
                </div>
            }
        </div>
    `
})
export class DotFileUploadComponent {
    private readonly fileService = inject(FileService);
    
    readonly acceptedTypes = input<string>('*/*');
    readonly allowMultiple = input<boolean>(false);
    readonly maxFileSize = input<number>(10 * 1024 * 1024); // 10MB
    
    readonly fileUploaded = output<UploadedFile>();
    readonly uploadError = output<string>();
    
    readonly uploading = signal(false);
    readonly uploadProgress = signal(0);
    readonly uploadedFiles = signal<UploadedFile[]>([]);
    
    onFileSelected(event: Event): void {
        const input = event.target as HTMLInputElement;
        const files = input.files;
        
        if (files && files.length > 0) {
            this.uploadFiles(Array.from(files));
        }
    }
    
    private uploadFiles(files: File[]): void {
        this.uploading.set(true);
        this.uploadProgress.set(0);
        
        const formData = new FormData();
        files.forEach(file => {
            formData.append('files', file);
        });
        
        this.fileService.uploadFiles(formData)
            .subscribe({
                next: (event) => {
                    if (event.type === HttpEventType.UploadProgress && event.total) {
                        const progress = Math.round((event.loaded / event.total) * 100);
                        this.uploadProgress.set(progress);
                    } else if (event.type === HttpEventType.Response) {
                        this.uploading.set(false);
                        const uploadedFiles = event.body as UploadedFile[];
                        this.uploadedFiles.set(uploadedFiles);
                        uploadedFiles.forEach(file => this.fileUploaded.emit(file));
                    }
                },
                error: (error) => {
                    this.uploading.set(false);
                    this.uploadError.emit('Upload failed');
                }
            });
    }
    
    removeFile(fileId: string): void {
        this.uploadedFiles.update(files => files.filter(f => f.id !== fileId));
    }
}
```

### Backend File Upload Endpoint
```java
@Path("/v1/files")
@ApplicationScoped
public class FileResource {
    private final WebResource webResource = new WebResource();
    
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadFiles(
        @Context HttpServletRequest request,
        @Context HttpServletResponse response,
        @FormDataParam("files") List<FormDataBodyPart> files
    ) {
        InitDataObject initData = webResource.init(request, response, true);
        User user = initData.getUser();
        
        try {
            List<UploadedFile> uploadedFiles = new ArrayList<>();
            
            for (FormDataBodyPart filePart : files) {
                // Validate file
                if (filePart.getContentDisposition().getSize() > MAX_FILE_SIZE) {
                    return ResponseUtil.mapExceptionResponse(
                        new DotDataException("File size exceeds limit")
                    );
                }
                
                // Save file
                InputStream inputStream = filePart.getValueAs(InputStream.class);
                String fileName = filePart.getContentDisposition().getFileName();
                
                UploadedFile uploadedFile = fileService.saveFile(inputStream, fileName, user);
                uploadedFiles.add(uploadedFile);
            }
            
            return Response.ok(new ResponseEntityView<>(uploadedFiles)).build();
            
        } catch (Exception e) {
            Logger.error(this, "File upload error: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }
}
```

## Testing Integration

### End-to-End Testing
```typescript
describe('MyFeature E2E', () => {
    let app: INestApplication;
    let httpServer: any;
    
    beforeAll(async () => {
        // Setup test application
        const moduleFixture = await Test.createTestingModule({
            imports: [AppModule],
        }).compile();
        
        app = moduleFixture.createNestApplication();
        await app.init();
        httpServer = app.getHttpServer();
    });
    
    it('should handle full workflow', async () => {
        // Create entity via API
        const createResponse = await request(httpServer)
            .post('/api/v1/my-entities')
            .send({
                name: 'Test Entity',
                description: 'Test Description'
            })
            .expect(201);
            
        const entityId = createResponse.body.entity.id;
        
        // Update entity
        await request(httpServer)
            .put(`/api/v1/my-entities/${entityId}`)
            .send({
                name: 'Updated Entity'
            })
            .expect(200);
            
        // Verify update
        const getResponse = await request(httpServer)
            .get(`/api/v1/my-entities/${entityId}`)
            .expect(200);
            
        expect(getResponse.body.entity.name).toBe('Updated Entity');
        
        // Delete entity
        await request(httpServer)
            .delete(`/api/v1/my-entities/${entityId}`)
            .expect(204);
    });
    
    afterAll(async () => {
        await app.close();
    });
});
```

## Location Information
- **Frontend services**: Located in `libs/data-access/src/lib/`
- **Backend resources**: Found in `com.dotcms.rest.*` packages
- **Data models**: TypeScript in `libs/dotcms-models/src/lib/`, Java in `*.model` packages
- **WebSocket handlers**: Located in `com.dotcms.websocket.*` packages
- **File handling**: Backend in `com.dotcms.storage.*`, Frontend in file services
- **Integration tests**: Located in `dotcms-integration` module and `e2e/` directory
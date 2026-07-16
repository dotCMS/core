# Legacy Custom Field Architecture

This module provides the bridge between legacy VTL-based custom fields and the modern Angular application in DotCMS. It handles iframe communication, field value synchronization, and automatic height management.

## 📁 **File Structure**

```
legacy_custom_field/
├── README.md                    # This documentation
├── shared-logger.js             # Centralized logging system
├── iframe-height-manager.js     # Automatic iframe height adjustment
├── field-interceptors.js        # Field value synchronization engine
└── legacy-custom-field.jsp      # Main entry point and configuration
```

## 🔧 **Architecture Overview**

### **1. Entry Point: `legacy-custom-field.jsp`**

**Purpose**: Main entry point that renders legacy custom fields in an iframe

- **Configures**: Dojo framework, VTL rendering environment
- **Initializes**: All JavaScript modules and bridge API
- **Provides**: Server-side data to client-side JavaScript

**Key Responsibilities**:

- Renders VTL-based field HTML from field definition
- Injects field metadata and contentlet data as JSON
- Sets up the `DotCustomFieldApi` bridge for Angular communication
- Initializes height manager and field interceptors

### **2. Communication Bridge: `DotCustomFieldApi`**

**Purpose**: JavaScript API that enables bidirectional communication between iframe and parent Angular app

```javascript
// Available in iframe window
window.DotCustomFieldApi = {
    ready(callback),           // Wait for Angular to be ready
    set(variable, value),      // Send value to Angular
    get(variable),             // Get value from Angular
    onChangeField(variable, callback) // Listen to Angular changes
}
```

**Communication Flow**:

```
Angular Parent ←→ postMessage ←→ DotCustomFieldApi ←→ Field Interceptors ←→ VTL Fields
```

### **3. Field Synchronization: `field-interceptors.js`**

**Purpose**: Core engine that handles bidirectional field value synchronization

#### **🔄 Initialization Flow**:

1. **Create Hidden Inputs** - Generate tracking inputs for each field
2. **Add Smart Interceptors** - Override `input.value` setters to detect changes
3. **Install Global Interceptors** - Monitor DOM for new inputs and setAttribute calls
4. **Setup Event Handlers** - Listen for blur/change events
5. **Wait for Angular** - Schedule sync when Angular API is ready

#### **🔄 Synchronization Process**:

Checks multiple value sources in priority order:

1. **VTL Target Elements** - Direct DOM elements set by VTL
2. **DOM Inputs** - Standard HTML inputs by name/id
3. **Dojo Widgets** - Dijit framework widgets (`dijit.byId()`)
4. **Dojo "Box" Widgets** - Pattern like `cachettlbox` → `cachettl`

#### **🔄 Event-Driven Updates**:

- **User Interactions** → `blur/change` events → `syncFieldValue()`
- **Programmatic Changes** → Smart interceptor → Direct Angular sync
- **Dynamic Inputs** → MutationObserver → Add interceptors

#### **🔄 Integration Points**:

- **Angular API**: `DotCustomFieldApi.set()` for value updates
- **Dojo Framework**: Widget value synchronization
- **VTL Elements**: Direct DOM element monitoring
- **DWR**: Direct Web Remoting error handling

### **4. Height Management: `iframe-height-manager.js`**

**Purpose**: Automatic iframe height adjustment to prevent scrollbars

#### **🔄 Detection Methods**:

- **ResizeObserver** - Monitors element size changes
- **MutationObserver** - Watches DOM modifications
- **Event Listeners** - User interactions (click, focus, input)
- **Window Resize** - Viewport changes

#### **🔄 Height Calculation**:

```javascript
Math.max(
  document.body.scrollHeight,
  document.body.offsetHeight,
  document.documentElement.scrollHeight,
  document.documentElement.offsetHeight
);
```

#### **🔄 Communication to Parent**:

```javascript
window.parent.postMessage(
  {
    type: "dotcms:iframe:resize",
    height: calculatedHeight,
    iframeId: uniqueId,
    fieldVariable: fieldVariable,
  },
  "*"
);
```

### **5. Logging System: `shared-logger.js`**

**Purpose**: Centralized, configurable logging for all modules

#### **🔧 Log Levels**:

- `ERROR (0)` - Only critical errors
- `WARN (1)` - Errors and warnings
- `INFO (2)` - Important information (default)
- `DEBUG (3)` - Detailed debugging information
- `TRACE (4)` - Everything including trace logs

#### **🔧 Usage**:

```javascript
// Set log level
DotLegacyLogger.setGlobalLogLevel("DEBUG");

// Enable/disable logging
DotLegacyLogger.setLoggingEnabled(false);

// Create module logger
const logger = DotLegacyLogger.createLogger("ModuleName");
```

## 🔀 **Communication Patterns**

### **1. Angular → Iframe (Value Updates)**

```
Angular Form Change
    ↓
postMessage to iframe
    ↓
DotCustomFieldApi.onChangeField()
    ↓
Update Dojo widget values
    ↓
Trigger DOM updates
```

### **2. Iframe → Angular (Value Sync)**

```
User edits field in iframe
    ↓
blur/change event detected
    ↓
Field Interceptor captures value
    ↓
DotCustomFieldApi.set(variable, value)
    ↓
postMessage to Angular parent
    ↓
Angular form control updated
```

### **3. Height Communication**

```
DOM changes in iframe
    ↓
ResizeObserver/MutationObserver
    ↓
Calculate new height
    ↓
postMessage('dotcms:iframe:resize')
    ↓
Angular adjusts iframe height
```

## 🔧 **Configuration & Debugging**

### **Performance Tuning**

#### **Field Interceptors**:

```javascript
INTERCEPTOR_CONFIG = {
  retryDelay: 100, // Retry delay for failed operations
  maxRetries: 3, // Max retry attempts
  interceptorTimeout: {
    immediate: 100, // Immediate intercept delay
    short: 500, // Short delay for detection
    long: 1000, // Long delay for fallback
  },
  syncTimeouts: [100, 500, 1000], // Staggered sync timeouts
};
```

#### **Height Manager**:

```javascript
IFRAME_CONFIG = {
  heightCheckThrottle: 100, // Height calculation throttle
  resizeTimeout: 200, // Resize event timeout
  mutationTimeout: 100, // Mutation observer timeout
  eventTimeout: 100, // User interaction timeout
  loadTimeout: 50, // Load detection timeout
  safetyTimeout: 3000, // Safety timeout for load detection
};
```

### **Debugging Commands**

#### **Available in Browser Console**:

```javascript
// Change log levels globally
DotLegacyLogger.setGlobalLogLevel("DEBUG"); // Enable verbose logging
DotLegacyLogger.setGlobalLogLevel("ERROR"); // Only show errors
DotLegacyLogger.setLoggingEnabled(false); // Disable all logging

// Force sync all fields
DotFieldInterceptors.syncNow(); // Force immediate sync

// Manual height adjustment
DotIframeHeightManager.setLogLevel("DEBUG"); // Enable height debug logs
```

#### **Common Debugging Scenarios**:

**Field Values Not Syncing**:

```javascript
// 1. Check if Angular API is available
console.log("API available:", !!window.DotCustomFieldApi);

// 2. Enable debug logging
DotLegacyLogger.setGlobalLogLevel("DEBUG");

// 3. Force sync and check console
DotFieldInterceptors.syncNow();

// 4. Check field interceptors
console.log("Instance:", window.DotFieldInterceptorManager_Instance);
```

**Height Not Adjusting**:

```javascript
// 1. Check if height manager is loaded
console.log("Height manager:", !!window.DotIframeHeightManager);

// 2. Enable debug logging
DotIframeHeightManager.setLogLevel("DEBUG");

// 3. Check current height
console.log("Current height:", document.body.scrollHeight);
```

## 🔧 **Development Guidelines**

### **Adding New Field Types**

1. **Update JSP**: Add VTL rendering logic in `legacy-custom-field.jsp`
2. **Field Variables**: Ensure field variable is included in `fieldJson`
3. **Interceptors**: Field interceptors will automatically detect new fields
4. **Testing**: Use debug logging to verify synchronization

### **Modifying Sync Logic**

1. **Edit `syncFieldValues()`**: Main synchronization method
2. **Add Value Sources**: Add new detection methods if needed
3. **Update Event Handlers**: Modify `setupEventHandlers()` for new events
4. **Test Integration**: Verify with different field types

### **Performance Optimization**

1. **Throttling**: Adjust timeout configurations
2. **Observer Efficiency**: Optimize MutationObserver selectors
3. **Logging**: Disable verbose logging in production
4. **Caching**: Use height caching for better performance

## 📋 **Integration Checklist**

### **For New Field Types**:

- [ ] Field variable included in `fieldJson`
- [ ] VTL generates appropriate DOM elements
- [ ] Field responds to blur/change events
- [ ] Value synchronization works bidirectionally
- [ ] Height adjusts properly with content changes
- [ ] Debug logging shows proper detection

### **For Angular Integration**:

- [ ] `DotCustomFieldApi` bridge is properly initialized
- [ ] postMessage communication works both directions
- [ ] Form control updates reflect in iframe
- [ ] Iframe value changes update Angular form
- [ ] Height messages are handled by parent
- [ ] Error handling works properly

## 🚨 **Troubleshooting**

### **Common Issues**:

1. **"DotCustomFieldApi not available"**

   - Check if bridge is initialized before interceptors
   - Verify Angular parent is properly loaded

2. **"Field values not syncing"**

   - Enable debug logging to see sync attempts
   - Check if field variable names match
   - Verify DOM elements have correct name/id attributes

3. **"Height not adjusting"**

   - Check if running in modal mode (height disabled)
   - Verify postMessage communication
   - Enable height debug logging

4. **"Performance issues"**
   - Adjust throttling configurations
   - Disable verbose logging
   - Optimize MutationObserver scope

---

## 📚 **Additional Resources**

- **Dojo Documentation**: Framework used in legacy fields
- **VTL Guide**: Velocity Template Language reference
- **Angular Integration**: Parent form communication patterns
- **PostMessage API**: Browser communication specifications

For development questions, enable debug logging and check browser console for detailed information.

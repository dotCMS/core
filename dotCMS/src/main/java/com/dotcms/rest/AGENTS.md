# dotCMS REST API Guide for Claude AI

## 📋 Essential Reference

**👉 REQUIRED: Read [README.md](./README.md) for comprehensive REST API development guidelines**

This file contains **AI-specific instructions only**. You MUST follow all patterns, standards, and checklists in README.md.

---

## 🤖 AI-Specific @Schema Rules

### Quick Decision Matrix

| Method Returns | Use @Schema |
|---------------|-------------|
| `ResponseEntity<UserView>` | `implementation = ResponseEntityUserView.class` |
| `ResponseEntity<List<T>>` | `implementation = ResponseEntityListView.class` |
| `List<MyEntity>` | `implementation = MyEntityListView.class` ¹ |
| `Map<String, MyType>` | `implementation = MapStringMyTypeView.class` ¹ |
| `Map<String, Object>` | `type = "object", description = "..."` |
| `JSONObject` / AI APIs | `type = "object", description = "...", example = "..."` |

¹ *Create if doesn't exist: `class MyEntityListView extends ArrayList<MyEntity>`*

### 🚨 FORBIDDEN for AI

```java
❌ @Schema(implementation = Object.class)     // Use type = "object" instead
❌ @Schema(implementation = Map.class)        // Use specific view class
❌ @Schema(implementation = HashMap.class)    // Use specific view class
❌ Missing descriptions on type = "object"    // Always add description
```

### ✅ AI Description Requirements

For `@Schema(type = "object")`:
- **Analyze the method's business logic** to understand what data is returned
- **Be specific**: "AI search results with contentlets and fragments" not "JSON object"  
- **Add examples** for complex AI/external API responses
- **Pattern**: "[Service] [operation] response containing [specific data] and [metadata]"

---

## 🎯 AI Workflow

1. **READ** [README.md](./README.md) patterns and requirements
2. **ANALYZE** method return statement - wrapped vs unwrapped?
3. **CHECK** if specific view class exists for the return type
4. **APPLY** correct @Schema from decision matrix above
5. **VERIFY** against [README.md checklist](./README.md#summary-checklist)

### Critical AI Verification
- ✅ Schema matches actual return type (not generic Object.class)
- ✅ All `type = "object"` have meaningful descriptions
- ✅ Descriptions explain actual data structure
- ✅ No deprecated Object.class/Map.class patterns remain

**Goal**: Create precise API documentation that helps developers understand exactly what each endpoint returns.
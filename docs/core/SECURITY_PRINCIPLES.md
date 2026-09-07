# Security Principles

## Universal Security Rules

### 🚨 NEVER Do These (Critical Violations)
```java
// ❌ NEVER: Direct input injection without validation
System.out.println("User input: " + userInput);  // INJECTION RISK

// ❌ NEVER: Hardcoded secrets or keys
String apiKey = "sk-1234567890abcdef";  // SECURITY VIOLATION

// ❌ NEVER: Exposing sensitive information in logs
Logger.info(this, "Password: " + password);  // SECURITY VIOLATION
```

### ✅ ALWAYS Do These (Required Practices)
```java
// ✅ Validate and sanitize all user input
if (UtilMethods.isSet(userInput) && userInput.matches("^[a-zA-Z0-9\\s\\-_]+$")) {
    Logger.info(this, "Valid input received");
    processInput(userInput);
} else {
    Logger.warn(this, "Invalid input rejected");
    throw new DotSecurityException("Invalid input format");
}

// ✅ Use Config for sensitive properties
String apiKey = Config.getStringProperty("external.api.key", "");
if (!UtilMethods.isSet(apiKey)) {
    throw new DotDataException("API key not configured");
}

// ✅ Never log sensitive information
Logger.info(this, "Authentication successful for user: " + user.getUserId());
```

## Input Validation Pattern
```java
public void processUserInput(String input) {
    // Null and empty validation
    if (!UtilMethods.isSet(input)) {
        throw new DotDataException("Input cannot be empty");
    }
    
    // Format validation
    if (!input.matches("^[a-zA-Z0-9\\s\\-_\\.]+$")) {
        Logger.warn(this, "Invalid input format attempted");
        throw new DotSecurityException("Invalid input format");
    }
    
    // Length validation
    if (input.length() > 255) {
        throw new DotDataException("Input exceeds maximum length");
    }
    
    // Business logic validation
    if (isBlacklisted(input)) {
        Logger.warn(this, "Blacklisted input attempted");
        throw new DotSecurityException("Input not allowed");
    }
    
    // Process validated input
    processValidatedInput(input);
}
```

## Security Checklist
Before committing any code:
- [ ] No hardcoded secrets, passwords, or API keys
- [ ] All user input is validated and sanitized
- [ ] Sensitive information is never logged
- [ ] Proper error handling without information leakage
- [ ] Security boundaries are maintained
- [ ] Authentication and authorization checks are in place

## Domain-Specific Security
- **Backend**: See `/docs/backend/SECURITY_BACKEND.md`
- **Frontend**: See [`docs/frontend/`](../frontend/README.md) — start with [ANGULAR_STANDARDS.md](../frontend/ANGULAR_STANDARDS.md)
- **Integration**: See `/docs/integration/API_CONTRACTS.md`
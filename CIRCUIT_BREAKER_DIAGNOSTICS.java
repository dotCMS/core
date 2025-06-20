// Circuit Breaker Diagnostic Commands
// You can run these in your dotCMS console or via REST API to diagnose the issue

// 1. Check current circuit breaker state
DbConnectionFactory.logCircuitBreakerState("Manual diagnostic check");

// 2. Force immediate recovery test (bypasses all delays)
DbConnectionFactory.forceRecoveryTest("Manual diagnostic recovery test");

// 3. Check again after recovery test
DbConnectionFactory.logCircuitBreakerState("After manual recovery test");

// 4. If still not working, manually close the circuit
DbConnectionFactory.closeDatabaseCircuitBreaker("Manual override - database confirmed healthy");

// 5. Final state check
DbConnectionFactory.logCircuitBreakerState("After manual circuit close");
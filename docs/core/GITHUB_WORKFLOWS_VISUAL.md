# GitHub Workflows and Claude Code Security Gates

## Overview

This document provides visual flow diagrams and decision trees for dotCMS GitHub workflows, with a focus on the Claude Code security implementation that requires organization membership verification.

## GitHub Workflow Architecture

### High-Level Workflow Flow

```mermaid
graph TB
    A[Developer creates PR] --> B{Files changed?}
    B -->|Backend files| C[PR Workflow - Backend Tests]
    B -->|Frontend files| D[PR Workflow - Frontend Tests]
    B -->|Both| E[PR Workflow - Full Tests]
    
    C --> F[Code Review]
    D --> F
    E --> F
    
    F --> G{Approved?}
    G -->|Yes| H[Merge Queue Workflow]
    G -->|No| I[Request Changes]
    I --> J[Developer fixes]
    J --> A
    
    H --> K[Trunk Workflow]
    K --> L[Nightly Workflow]
    L --> M[LTS Workflow]
    
    style A fill:#e1f5fe
    style F fill:#fff3e0
    style H fill:#e8f5e8
    style K fill:#f3e5f5
    style L fill:#fef7e0
    style M fill:#ffebee
```

### Core Workflow Components

```mermaid
graph TD
    subgraph "PR Workflow (cicd_1-pr.yml)"
        A1[Initialize Phase] --> A2[Build Phase]
        A2 --> A3[Test Phase]
        A3 --> A4[Semgrep Security Analysis]
        A4 --> A5[Finalize Phase]
    end
    
    subgraph "Merge Queue (cicd_2-merge-queue.yml)"
        B1[Comprehensive Tests] --> B2[Artifact Generation]
        B2 --> B3[Flaky Test Detection]
    end
    
    subgraph "Trunk Workflow (cicd_3-trunk.yml)"
        C1[Artifact Reuse] --> C2[CLI Native Build]
        C2 --> C3[Deployment to Trunk]
        C3 --> C4[SDK Publishing]
    end
    
    subgraph "Nightly Workflow (cicd_4-nightly.yml)"
        D1[Extended Test Suites] --> D2[Performance Benchmarking]
        D2 --> D3[Nightly Environment Deploy]
    end
    
    subgraph "LTS Workflow (cicd_5-lts.yml)"
        E1[Manual Trigger] --> E2[Release Preparation]
        E2 --> E3[Comprehensive Validation]
        E3 --> E4[Special Release Artifacts]
    end
    
    A5 --> B1
    B3 --> C1
    C4 --> D1
    D3 --> E1
    
    style A1 fill:#e3f2fd
    style B1 fill:#e8f5e8
    style C1 fill:#f3e5f5
    style D1 fill:#fff8e1
    style E1 fill:#ffebee
```

## Claude Code Security Implementation

### Organization Membership Security Gate

The Claude Code integration uses a sophisticated security system to ensure only authorized dotCMS organization members can trigger AI-powered code reviews and assistance.

#### Security Gate Decision Tree

```mermaid
graph TD
    A[@claude mention detected] --> B[Extract username from GitHub event]
    B --> C[Organization Membership Check Action]
    
    C --> D{GitHub API Call<br/>GET /orgs/dotCMS/members/{username}}
    
    D -->|HTTP 204 No Content| E[✅ User is authorized]
    D -->|HTTP 404 Not Found| F[❌ User is blocked]
    
    E --> G[Claude Code Workflow Continues]
    E --> H[Log: ✅ AUTHORIZED]
    
    F --> I[Display troubleshooting steps]
    F --> J[Log: ❌ BLOCKED]
    F --> K[Workflow terminated]
    
    I --> L[Show membership verification guide]
    
    style A fill:#e1f5fe
    style E fill:#e8f5e8
    style F fill:#ffebee
    style G fill:#e8f5e8
    style K fill:#ffcdd2
```

### User Access Scenarios

Based on the acceptance criteria from the testing issue:

```mermaid
graph TD
    A[User mentions @claude] --> B{User Type Check}
    
    B -->|Non-member| C[❌ FAIL<br/>Not in dotCMS org]
    B -->|Member with private visibility| D[❌ FAIL<br/>Membership not public]
    B -->|Member with public visibility| E[✅ SUCCESS<br/>Can use Claude]
    
    C --> F[Error: User not authorized<br/>Contact org admin to be added]
    D --> G[Error: Make membership public<br/>Visit github.com/orgs/dotCMS/people]
    E --> H[Claude Code executes successfully]
    
    style C fill:#ffcdd2
    style D fill:#fff3e0
    style E fill:#e8f5e8
    style F fill:#ffcdd2
    style G fill:#fff3e0
    style H fill:#e8f5e8
```

## Detailed Security Implementation

### Organization Membership Check Action

The security gate is implemented as a reusable composite action located at:
`.github/actions/security/org-membership-check/action.yml`

#### Implementation Flow

```mermaid
sequenceDiagram
    participant U as User
    participant GH as GitHub Event
    participant API as GitHub API
    participant Action as Security Action
    participant Log as Workflow Log
    
    U->>GH: Mentions @claude in issue/PR
    GH->>Action: Triggers workflow with username
    Action->>API: GET /orgs/dotCMS/members/{username}
    
    alt User is authorized member
        API-->>Action: HTTP 204 No Content
        Action->>Log: ✅ AUTHORIZED: {username} is dotCMS member
        Action-->>GH: is_member=true, Continue workflow
    else User is not authorized
        API-->>Action: HTTP 404 Not Found
        Action->>Log: ❌ BLOCKED: {username} failed membership check
        Action->>U: Display troubleshooting steps
        Action-->>GH: is_member=false, Terminate workflow
    end
```

### Security Features

1. **Hardcoded Organization**: Only checks `dotCMS` organization membership
2. **No Additional Secrets**: Uses default `GITHUB_TOKEN`
3. **Both Public and Private Members**: API detects all organization members
4. **Clear Error Messages**: Provides actionable troubleshooting steps
5. **Graceful Failure**: Fails securely when membership cannot be verified

### Membership Visibility Requirements

```mermaid
graph LR
    A[dotCMS Organization Member] --> B{Membership Visibility}
    
    B -->|Public| C[✅ Can use @claude<br/>API returns HTTP 204]
    B -->|Private| D[❌ Cannot use @claude<br/>API returns HTTP 404]
    
    D --> E[User Action Required:<br/>Make membership public]
    E --> F[Visit github.com/orgs/dotCMS/people]
    F --> G[Click 'Make public' button]
    G --> C
    
    style C fill:#e8f5e8
    style D fill:#ffcdd2
    style E fill:#fff3e0
```

## Change Detection System

The workflow system uses intelligent change detection to optimize build and test execution:

```mermaid
graph TD
    A[Files Changed in PR] --> B[Filter Analysis<br/>.github/filters.yaml]
    
    B --> C{Change Categories}
    
    C -->|Backend Changes| D[Java files, Maven, Tests<br/>Trigger: Backend workflow]
    C -->|Frontend Changes| E[Angular, CSS, TypeScript<br/>Trigger: Frontend workflow]
    C -->|CLI Changes| F[CLI tools, related backend<br/>Trigger: CLI workflow]
    C -->|Full Build Changes| G[Infrastructure, Config<br/>Trigger: Complete rebuild]
    
    D --> H[Backend Test Suite]
    E --> I[Frontend Test Suite]
    F --> J[CLI Test Suite]
    G --> K[All Test Suites]
    
    style A fill:#e1f5fe
    style B fill:#fff3e0
    style H fill:#e8f5e8
    style I fill:#e8f5e8
    style J fill:#e8f5e8
    style K fill:#ffebee
```

## Troubleshooting Guide

### Common Issues and Solutions

#### Issue: User is dotCMS member but @claude fails

```mermaid
graph TD
    A[User is dotCMS member<br/>but @claude fails] --> B[Check membership visibility]
    
    B --> C[Visit github.com/orgs/dotCMS/people]
    C --> D{Can you see your name?}
    
    D -->|Yes| E[Look for 'Make public' button]
    D -->|No| F[Contact organization owner<br/>to be added to dotCMS org]
    
    E --> G[Click 'Make public']
    G --> H[✅ @claude should work now]
    
    F --> I[Wait for org invitation]
    I --> J[Accept invitation]
    J --> G
    
    style H fill:#e8f5e8
    style F fill:#fff3e0
    style I fill:#fff3e0
```

#### Issue: Workflow doesn't trigger

```mermaid
graph TD
    A[Workflow not triggering] --> B{Check trigger conditions}
    
    B -->|File location| C[Verify .github/workflows/ location]
    B -->|YAML syntax| D[Run yamllint validation]
    B -->|Change filters| E[Check .github/filters.yaml]
    
    C --> F[Move file to correct location]
    D --> G[Fix YAML syntax errors]
    E --> H[Update filter patterns]
    
    F --> I[✅ Workflow triggers]
    G --> I
    H --> I
    
    style I fill:#e8f5e8
```

## Current Authorized Users

Based on the testing issue, only 3 users currently have public organization membership:

- @fmontes
- @oidacra  
- @sfreudenthaler

**All other dotCMS organization members must set their membership visibility to public to use @claude mentions.**

## Security Best Practices

### ✅ Security Patterns Used

1. **Zero-Trust PR Context**: No secrets exposed in pull request workflows
2. **Minimal Token Usage**: Uses default GITHUB_TOKEN only
3. **Hardcoded Organization**: Cannot be overridden or manipulated
4. **Graceful Error Handling**: Fails securely with helpful messages
5. **Public Membership Requirement**: Ensures transparency

### ❌ Security Anti-Patterns Avoided

1. **No Secret Exposure**: Secrets never exposed in PR context
2. **No Bypasses**: No way to bypass organization membership check  
3. **No Token Elevation**: No additional permissions requested
4. **No User Input Injection**: All inputs sanitized and validated

## Performance Optimizations

### Caching Strategy

```mermaid
graph LR
    A[Build Cache] --> B[Maven Dependencies]
    A --> C[Node Modules]
    A --> D[Build Artifacts]
    
    E[Artifact Reuse] --> F[Merge Queue → Trunk]
    E --> G[Cross-Stage Sharing]
    
    H[Parallel Execution] --> I[Test Matrix]
    H --> J[Independent Jobs]
    H --> K[Change-Based Filtering]
    
    style A fill:#e3f2fd
    style E fill:#e8f5e8
    style H fill:#fff3e0
```

## Integration Points

This security implementation integrates with:

- **Issue Management**: Links to GitHub issue workflows
- **PR Processing**: Integrates with PR review workflows  
- **Release Pipeline**: Connects to trunk and release workflows
- **Monitoring**: Sends notifications to #guild-dev-pipeline Slack channel

## Future Enhancements

Potential improvements to the security system:

1. **Team-Based Access**: Allow specific team membership instead of org-wide
2. **Rate Limiting**: Implement usage limits per user
3. **Audit Logging**: Enhanced logging for security monitoring
4. **Integration Testing**: Automated tests for security gate functionality

---

**Location**: `docs/core/GITHUB_WORKFLOWS_VISUAL.md`  
**Related Files**:
- `.github/actions/security/org-membership-check/action.yml`
- `.github/filters.yaml` 
- `docs/core/GIT_WORKFLOWS.md`
- `docs/core/GITHUB_ISSUE_MANAGEMENT.md`
# GitHub Actions CI/CD Documentation

## Overview

This repository implements a modern, modular CI/CD pipeline designed to provide fast feedback to developers while maintaining comprehensive quality gates and balanced risk management. The system is built around the principle of **DRY (Don't Repeat Yourself)** workflows and modular, reusable components.

### Key Features

- **Modular Architecture**: Reusable workflow components reduce duplication
- **Security-First Design**: Zero-trust PR model with comprehensive security layers
- **Intelligent Change Detection**: Path-based filtering minimizes unnecessary work
- **Sophisticated Caching**: Multi-level caching strategy for optimal performance
- **Comprehensive Testing**: Parallel test execution with conditional logic
- **Advanced Monitoring**: Real-time status aggregation and failure analysis
- **Developer-Friendly**: Clear documentation and support channels

## Documentation Structure

This documentation is organized into focused sections for easy navigation:

### 📚 Core Documentation

- **[Getting Started](docs/getting-started.md)** - New developer guide to GitHub Actions and our modular architecture
- **[Architecture](docs/architecture.md)** - Pipeline architecture, workflow interdependencies, and component structure
- **[Testing Strategy](docs/testing.md)** - Test categories, execution strategies, and testing workflows
- **[Security](docs/security.md)** - Comprehensive security guidelines, threat model, and best practices
- **[Troubleshooting](docs/troubleshooting.md)** - Common issues, debugging procedures, and performance optimization

### 🔧 Quick References

- **[Workflow Structure](#workflow-structure)** - File organization and naming conventions
- **[Main Workflows](#main-workflows)** - Core pipeline entry points
- **[Support and Maintenance](#support-and-maintenance)** - Getting help and maintenance information

## Workflow Structure

```
.github/
├── workflows/
│   ├── cicd_1-pr.yml           # Main entry point for PRs
│   ├── cicd_2-merge-queue.yml  # Merge queue validation
│   ├── cicd_3-trunk.yml        # Post-merge to main
│   ├── cicd_4-nightly.yml      # Nightly builds
│   ├── cicd_5-lts.yml          # LTS releases (manual)
│   ├── cicd_comp_*.yml         # ✅ Reusable components (USE THESE)
│   └── legacy-*.yml            # ⚠️ Legacy files (see Architecture docs)
├── actions/
│   └── core-cicd/
│       ├── prepare-runner/     # Sets up runner environment
│       ├── setup-java/         # Java installation
│       └── maven-job/          # Maven build orchestration
├── docs/                       # 📚 Detailed documentation
└── filters.yaml                # Defines what changes trigger what tests
```

## Main Workflows

### Pipeline Progression

The main workflows follow a numbered naming convention showing the natural progression of code through the CI/CD pipeline:

1. **[`cicd_1-pr.yml`](workflows/cicd_1-pr.yml)** - Pull Request validation and testing
2. **[`cicd_2-merge-queue.yml`](workflows/cicd_2-merge-queue.yml)** - Final validation before merge
3. **[`cicd_3-trunk.yml`](workflows/cicd_3-trunk.yml)** - Post-merge processing and deployment
4. **[`cicd_4-nightly.yml`](workflows/cicd_4-nightly.yml)** - Scheduled nightly builds
5. **[`cicd_5-lts.yml`](workflows/cicd_5-lts.yml)** - Manual LTS releases

### Reusable Components

All main workflows use these reusable components:

- **[`cicd_comp_initialize-phase.yml`](workflows/cicd_comp_initialize-phase.yml)** - Change detection and build planning
- **[`cicd_comp_build-phase.yml`](workflows/cicd_comp_build-phase.yml)** - Maven builds and artifact generation
- **[`cicd_comp_test-phase.yml`](workflows/cicd_comp_test-phase.yml)** - Test orchestration
- **[`cicd_comp_semgrep-phase.yml`](workflows/cicd_comp_semgrep-phase.yml)** - Security and code quality analysis
- **[`cicd_comp_deployment-phase.yml`](workflows/cicd_comp_deployment-phase.yml)** - Environment deployments
- **[`cicd_comp_finalize-phase.yml`](workflows/cicd_comp_finalize-phase.yml)** - Status aggregation

## Quick Start Guide

### For New Developers

1. **Start with**: [Getting Started Guide](docs/getting-started.md)
2. **Understand**: [Architecture Overview](docs/architecture.md)
3. **Learn**: [Testing Strategy](docs/testing.md)
4. **Reference**: [Troubleshooting Guide](docs/troubleshooting.md)

### For Experienced Developers

1. **Security**: Review [Security Guidelines](docs/security.md) before making changes
2. **Architecture**: Understand [Pipeline Architecture](docs/architecture.md)
3. **Troubleshooting**: Bookmark [Troubleshooting Guide](docs/troubleshooting.md)

### Most Common Tasks

| Task | Primary Documentation | Key Files |
|------|---------------------|-----------|
| Add new tests | [Testing Strategy](docs/testing.md) | `cicd_comp_test-phase.yml` |
| Modify build process | [Architecture](docs/architecture.md) | `cicd_comp_build-phase.yml` |
| Debug failing workflows | [Troubleshooting](docs/troubleshooting.md) | Logs, filters.yaml |
| Update security settings | [Security Guidelines](docs/security.md) | Workflow permissions |
| Add change detection | [Architecture](docs/architecture.md) | `filters.yaml` |

## Development Principles

### ✅ Always Do
1. **Use reusable components** rather than duplicating logic
2. **Follow security patterns** (no secrets in PR context)
3. **Implement change detection** for optimal performance
4. **Document workflow purpose** and key features
5. **Test changes thoroughly** before deployment

### ❌ Never Do
1. **Create multiple workflows** for the same trigger
2. **Add secrets to PR workflows** (security violation)
3. **Modify legacy workflows** without dedicated task and testing
4. **Use hardcoded values** (use variables instead)
5. **Implement build logic directly** in main workflows

## Support and Maintenance

### Getting Help

**Primary Support Channel**: **#guild-dev-pipeline** Slack channel
- **Best For**: Questions, troubleshooting, implementation guidance
- **Response Time**: Real-time during business hours
- **Expertise**: Direct access to CI/CD team and community knowledge

**Additional Resources**:
- **GitHub Issues**: Bug reports and technical issues
- **Documentation**: Comprehensive guides in [docs/](docs/) directory
- **Troubleshooting**: [Troubleshooting Guide](docs/troubleshooting.md)

### Maintenance Information

**Regular Maintenance**:
- **Weekly**: Security scan review and action updates
- **Monthly**: Performance optimization and cache cleanup
- **Quarterly**: Architecture review and documentation updates

**Emergency Procedures**:
- **Workflow Blocking**: Disable in GitHub UI immediately
- **Security Incident**: Follow [Security Guidelines](docs/security.md)
- **Critical Build Failure**: Check #guild-dev-pipeline Slack

## Key Configuration Files

- **[`.sdkmanrc`](../.sdkmanrc)**: Java version control (affects all workflows)
- **[`filters.yaml`](filters.yaml)**: Change detection configuration
- **[`pom.xml`](../pom.xml)**: Maven configuration
- **[`core-web/package.json`](../core-web/package.json)**: Node.js dependencies

## Additional Resources

### External Documentation
- **[GitHub Actions Documentation](https://docs.github.com/en/actions)**
- **[Workflow Syntax Reference](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions)**
- **[Security Best Practices](https://docs.github.com/en/actions/security-guides/security-hardening-for-github-actions)**

### Internal Resources
- **[dotCMS Development Guide](../CLAUDE.md)** - Main codebase development standards
- **[Maven Release Process](workflows/maven-release-process.md)** - Release procedures
- **[Actions Documentation](actions/core-cicd/)** - Individual action documentation

---

## Quick Navigation

- 🚀 **[Getting Started](docs/getting-started.md)** - New to GitHub Actions or our system?
- 🏗️ **[Architecture](docs/architecture.md)** - Understand the pipeline structure
- 🧪 **[Testing](docs/testing.md)** - Learn about our testing strategy
- 🔒 **[Security](docs/security.md)** - Security guidelines and best practices
- 🔧 **[Troubleshooting](docs/troubleshooting.md)** - Fix common issues quickly

**Need immediate help?** Join **#guild-dev-pipeline** on Slack for real-time support.
# dotCMS Feature Labels

This reference provides the complete list of `dotCMS : [Feature]` labels available in the repository and logic for selecting the most appropriate label based on issue content.

## Available Feature Labels

- `dotCMS : Admin Tools` - Admin interface, user management, system administration
- `dotCMS : Analytics` - Data and Analytics umbrella, reporting, metrics, dashboard
- `dotCMS : Architecture` - System architecture, design patterns, structural changes
- `dotCMS : Authentication` - Login, authentication, user credentials, SSO
- `dotCMS : Build` - Build system, Maven, compilation, CI/CD
- `dotCMS : Calendar` - Calendar functionality, scheduling, date/time features
- `dotCMS : CLI` - Command line interface, terminal tools, CLI utilities
- `dotCMS : Clustering` - Multi-server setup, clustering, scalability
- `dotCMS : Containerization` - Docker, containers, deployment
- `dotCMS : Content Management` - Content creation, editing, content types, content operations
- `dotCMS : New Edit Contentlet` - Content editing interface, contentlet editor, edit content functionality
- `dotCMS : Experiments` - A/B testing, experiments feature, analytics experiments
- `dotCMS : Image Processing` - Image handling, thumbnails, image optimization
- `dotCMS : Localization` - i18n, multi-lingual support, regional settings
- `dotCMS : OSGi` - OSGi bundles, plugin system, modular architecture
- `dotCMS : Permissions` - User permissions, roles, access control, security
- `dotCMS : Privacy` - Privacy compliance, GDPR, data protection
- `dotCMS : Push Publishing` - Content publishing, push/pull functionality, content distribution
- `dotCMS : Rest API` - REST API endpoints, API improvements, web services
- `dotCMS : Rules-Engine` - Business rules, automation, conditional logic
- `dotCMS : SDK` - Software Development Kit, developer tools, APIs for external development
- `dotCMS : Security` - Security features, vulnerability fixes, security enhancements
- `dotCMS : Technical Debt` - Code refactoring, cleanup, technical improvements
- `dotCMS : Time Machine` - Version control, content history, rollback functionality
- `dotCMS : Translation` - Translation services, content translation workflows
- `dotCMS : Upgrade` - System upgrades, migration, update processes
- `dotCMS : Viewtools` - Frontend rendering, view layer, template tools
- `dotCMS : Workflow` - Workflow engine, content approval processes, workflow steps
- `dotCMS Cloud : Usage` - View and understand your dotCMS Cloud usage
 
## Feature Label Selection Logic

When analyzing the task description, match keywords and concepts to select the most appropriate feature label:

### Keyword Matching Guide

- **Admin/User Interface** → `dotCMS : Admin Tools`
- **Analytics/Reporting/Metrics/Dashboard** → `dotCMS : Analytics`
- **API/REST/Endpoints/Web Services** → `dotCMS : Rest API`
- **Architecture/Design/Structure/Patterns** → `dotCMS : Architecture`
- **Authentication/Login/SSO/Credentials** → `dotCMS : Authentication`
- **Build/CI/CD/Maven/Compilation** → `dotCMS : Build`
- **Calendar/Scheduling/Date/Time** → `dotCMS : Calendar`
- **CLI/Command Line/Terminal** → `dotCMS : CLI`
- **Clustering/Scalability/Multi-server** → `dotCMS : Clustering`
- **Content/CMS/Editor/Contentlet** → `dotCMS : Content Management`
- **Edit Content/Contentlet Editor/Edit Contentlet** → `dotCMS : New Edit Contentlet`
- **Docker/Deployment/Containers** → `dotCMS : Containerization`
- **Experiments/A-B Testing/Testing** → `dotCMS : Experiments`
- **Frontend/Templates/Rendering** → `dotCMS : Viewtools`
- **Images/Thumbnails/Image Processing** → `dotCMS : Image Processing`
- **i18n/Multi-lingual/Localization/Language** → `dotCMS : Localization`
- **OSGi/Plugin/Bundles** → `dotCMS : OSGi`
- **Performance/Refactor/Cleanup** → `dotCMS : Technical Debt`
- **Permissions/Security/Access Control/Roles** → `dotCMS : Permissions`
- **Privacy/GDPR/Data Protection** → `dotCMS : Privacy`
- **Publishing/Distribution/Push/Pull** → `dotCMS : Push Publishing`
- **Rules/Automation/Conditional Logic** → `dotCMS : Rules-Engine`
- **SDK/Developer Tools/External APIs** → `dotCMS : SDK`
- **Security/Vulnerabilities/Security Fixes** → `dotCMS : Security`
- **Translation/Content Translation** → `dotCMS : Translation`
- **Upgrade/Migration/Update** → `dotCMS : Upgrade`
- **Version/History/Rollback** → `dotCMS : Time Machine`
- **Workflow/Approval/Steps** → `dotCMS : Workflow`

### Selection Strategy

1. **Primary keywords**: Look for explicit mentions of features (e.g., "workflow", "API", "authentication")
2. **Context clues**: Analyze the problem domain (e.g., UI issues → Admin Tools, backend services → appropriate backend feature)
3. **Multiple matches**: If multiple labels could apply, choose the most specific one
4. **Default fallback**: If no clear match is found, use `dotCMS : Content Management` as it's the most general category

### Examples

**Input**: "Fix the save button in the content editor"
- Keywords: content, editor
- Selection: `dotCMS : Content Management`

**Input**: "Improve content editing interface performance"
- Keywords: editing, interface, content
- Selection: `dotCMS : New Edit Contentlet`

**Input**: "Add new field type to contentlet editor"
- Keywords: contentlet editor, field
- Selection: `dotCMS : New Edit Contentlet`

**Input**: "Add OAuth2 support for login"
- Keywords: OAuth2, login
- Selection: `dotCMS : Authentication`

**Input**: "Optimize database query performance in content API"
- Keywords: performance, API
- Selection: `dotCMS : Rest API` (more specific than Technical Debt)

**Input**: "Refactor legacy JSP code in admin interface"
- Keywords: refactor, admin interface
- Selection: `dotCMS : Admin Tools` (feature context over technical debt)

**Input**: "Update Angular to version 19"
- Keywords: update, Angular (frontend)
- Selection: `dotCMS : Build` (library/framework updates)

## Retrieving Current Labels

To verify or update this list, retrieve current labels from the repository:

```bash
gh label list --repo dotCMS/core --limit 100 | grep "dotCMS :"
```

This ensures the skill always references accurate, up-to-date feature labels.

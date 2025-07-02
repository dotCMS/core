#!/bin/bash

# Script to find missing schemas in @ApiResponse annotations

echo "Comprehensive search for missing schemas in REST endpoints"
echo "=========================================================="

files=(
    "/Users/stevebolton/git/core-baseline/dotCMS/src/main/java/com/dotcms/rest/RoleResource.java"
    "/Users/stevebolton/git/core-baseline/dotCMS/src/main/java/com/dotcms/rest/UtilResource.java"
    "/Users/stevebolton/git/core-baseline/dotCMS/src/main/java/com/dotcms/rest/elasticsearch/ESContentResourcePortlet.java"
    "/Users/stevebolton/git/core-baseline/dotCMS/src/main/java/com/dotcms/rest/CMSConfigResource.java"
    "/Users/stevebolton/git/core-baseline/dotCMS/src/main/java/com/dotcms/rest/personas/PersonasResourcePortlet.java"
    "/Users/stevebolton/git/core-baseline/dotCMS/src/main/java/com/dotcms/rest/RestExamplePortlet.java"
    "/Users/stevebolton/git/core-baseline/dotCMS/src/main/java/com/dotcms/rest/OSGIResource.java"
    "/Users/stevebolton/git/core-baseline/dotCMS/src/main/java/com/dotcms/rest/UserResource.java"
    "/Users/stevebolton/git/core-baseline/dotCMS/src/main/java/com/dotcms/rest/LicenseResource.java"
    "/Users/stevebolton/git/core-baseline/dotCMS/src/main/java/com/dotcms/rest/ContentResource.java"
    "/Users/stevebolton/git/core-baseline/dotCMS/src/main/java/com/dotcms/rest/IntegrityResource.java"
    "/Users/stevebolton/git/core-baseline/dotCMS/src/main/java/com/dotcms/rest/PublishQueueResource.java"
    "/Users/stevebolton/git/core-baseline/dotCMS/src/main/java/com/dotcms/rest/AuditPublishingResource.java"
    "/Users/stevebolton/git/core-baseline/dotCMS/src/main/java/com/dotcms/rest/ClusterResource.java"
    "/Users/stevebolton/git/core-baseline/dotCMS/src/main/java/com/dotcms/rest/EnvironmentResource.java"
)

for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo ""
        echo "FILE: $file"
        echo "----------------------------------------"
        rg -n '@ApiResponse\(responseCode = "200"' "$file" -A 3 | rg -B 2 -A 1 'mediaType = "application/json"' | rg -v 'schema = @Schema' || echo "No missing schemas found"
    fi
done
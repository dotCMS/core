This Elasticsearch image includes a plugin required for dotCMS < 5.2.8 that stores relationship data in its own field.


docker buildx build --platform linux/amd64,linux/arm64 -o type=docker --pull --push -t dotcms/elasticsearch:6.1.3_for_5.2.8 .


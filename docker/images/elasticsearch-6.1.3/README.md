We encourage you to use the image provided at https://hub.docker.com/r/dotcms/elasticsearch/ rather than building your own image.  

docker buildx build --platform linux/amd64,linux/arm64 -o type=docker --pull --push -t dotcms/elasticsearch:6.1.3_for_5.2.8 .


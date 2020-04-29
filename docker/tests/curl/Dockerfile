ARG FROM_IMAGE
FROM ${FROM_IMAGE} as prepare-curl

ENV FROM_IMAGE=${FROM_IMAGE}
RUN echo "from image: $FROM_IMAGE"

WORKDIR /srv

# Installs node, npm and newman
RUN apk add --update nodejs
RUN apk add --update npm
RUN node --version
RUN npm --version
RUN npm install -g newman && npm install -g newman-reporter-htmlextra

COPY setup/build-src/curlTests.sh /build/curlTests.sh
RUN chmod 500 /build/curlTests.sh

COPY setup/build-src/newmanTestResultsHeader.html /build/newmanTestResultsHeader.html
RUN chmod 500 /build/newmanTestResultsHeader.html

COPY setup/build-src/newmanTestResultsFooter.html /build/newmanTestResultsFooter.html
RUN chmod 500 /build/newmanTestResultsFooter.html

ENTRYPOINT ["/build/entrypoint.sh"]
CMD ["curl"]

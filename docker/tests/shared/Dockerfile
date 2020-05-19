FROM gcr.io/cicd-246518/build-cms-image

LABEL com.dotcms.contact "support@dotcms.com"
LABEL com.dotcms.vendor "dotCMS LLC"
LABEL com.dotcms.description "dotCMS Content Management System"

#  One of ["TARBALL_URL", "RELEASE", "NIGHTLY", "COMMIT", "TAG" ]
ARG BUILD_FROM=COMMIT
#  Value resolved in the context of $BUILD_FROM
ARG BUILD_ID=HEAD

WORKDIR /srv

ENTRYPOINT ["/sbin/tini", "--", "/srv/entrypoint.sh"]
CMD ["dotcms"]

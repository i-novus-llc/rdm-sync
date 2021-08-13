ARG DOCKER_REGISTRY
FROM ${DOCKER_REGISTRY}/postgres:11.5

COPY docker-entrypoint-initdb.d /docker-entrypoint-initdb.d

EXPOSE 5432

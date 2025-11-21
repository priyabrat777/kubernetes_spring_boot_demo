# PostgreSQL Dockerfile
# Using official PostgreSQL 16 Alpine image

FROM postgres:16-alpine

# Set environment variables (these will be overridden by Kubernetes)
ENV POSTGRES_DB=demodb
ENV POSTGRES_USER=demouser
ENV POSTGRES_PASSWORD=demopass

# Copy initialization scripts
COPY init.sql /docker-entrypoint-initdb.d/

# Expose PostgreSQL port
EXPOSE 5432

# The base image already has the correct ENTRYPOINT and CMD

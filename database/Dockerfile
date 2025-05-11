FROM pgvector/pgvector:pg17

ARG POSTGRES_USER
ARG POSTGRES_PASSWORD
ARG POSTGRES_DB

ENV POSTGRES_USER=$POSTGRES_USER
ENV POSTGRES_PASSWORD=$POSTGRES_PASSWORD
ENV POSTGRES_DB=$POSTGRES_DB

COPY pgdata.tar.gz /pgdata.tar.gz

RUN mkdir -p /var/lib/postgresql/data && \
    tar -xzf /pgdata.tar.gz -C /var/lib/postgresql/data && \
    chown -R postgres:postgres /var/lib/postgresql/data && \
    rm /pgdata.tar.gz

VOLUME ["/var/lib/postgresql/data"]

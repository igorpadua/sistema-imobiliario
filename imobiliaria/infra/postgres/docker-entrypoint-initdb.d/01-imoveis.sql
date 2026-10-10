\getenv senha_dono DB_PASSWORD_IMOVEIS_DONO
\getenv senha_app DB_PASSWORD_IMOVEIS_APP

CREATE ROLE imoveis_dono WITH LOGIN PASSWORD :'senha_dono';

CREATE ROLE imoveis_app WITH LOGIN PASSWORD :'senha_app';

CREATE DATABASE imoveis OWNER imoveis_dono;

REVOKE CONNECT ON DATABASE imoveis FROM PUBLIC;

GRANT CONNECT ON DATABASE imoveis to imoveis_app;

\connect imoveis

CREATE EXTENSION postgis;

CREATE EXTENSION pg_trgm;

ALTER DEFAULT PRIVILEGES FOR ROLE imoveis_dono IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO imoveis_app;
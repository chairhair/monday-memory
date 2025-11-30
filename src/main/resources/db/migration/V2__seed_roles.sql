-- V2__seed_roles.sql

INSERT INTO role_entity (access_level)
VALUES ('GUEST')
ON CONFLICT (access_level) DO NOTHING;

INSERT INTO role_entity (access_level)
VALUES ('USER')
ON CONFLICT (access_level) DO NOTHING;

INSERT INTO role_entity (access_level)
VALUES ('ADMIN')
ON CONFLICT (access_level) DO NOTHING;

INSERT INTO role_entity (access_level)
VALUES ('PRO')
ON CONFLICT (access_level) DO NOTHING;

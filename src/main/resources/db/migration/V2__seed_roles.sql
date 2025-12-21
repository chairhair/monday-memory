-- V2__seed_roles.sql
DO $$
BEGIN
  IF (SELECT COUNT(*) FROM role WHERE name IN ('USER','ADMIN')) <> 1 THEN
    RAISE EXCEPTION 'Seed roles failed: would be inserting into roles again';
  END IF;
END $$;

INSERT INTO role_entity (access_level)
VALUES ('GUEST');

INSERT INTO role_entity (access_level)
VALUES ('USER');

INSERT INTO role_entity (access_level)
VALUES ('ADMIN');

INSERT INTO role_entity (access_level)
VALUES ('PRO');

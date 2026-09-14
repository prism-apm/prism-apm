INSERT INTO "permissions" ("id", "name", "url", "created_at", "updated_at") VALUES (1, 'fetch-data', '/api/apm/**', '2025-07-11 10:19:00.277939+08', '2025-07-11 10:19:00.277939+08');
INSERT INTO "permissions" ("id", "name", "url", "created_at", "updated_at") VALUES (2, 'admin', '/api/admin/**', '2025-07-11 10:19:00.303189+08', '2025-07-11 10:19:00.303189+08');
INSERT INTO "role_permission" ("id", "role_id", "permission_id", "created_at", "updated_at") VALUES (1, 1, 1, '2025-07-11 10:27:51.919464+08', '2025-07-11 10:27:51.919464+08');
INSERT INTO "role_permission" ("id", "role_id", "permission_id", "created_at", "updated_at") VALUES (2, 1, 2, '2025-07-11 10:28:25.605298+08', '2025-07-11 10:28:25.605298+08');
INSERT INTO "role_permission" ("id", "role_id", "permission_id", "created_at", "updated_at") VALUES (3, 2, 1, '2025-07-11 10:38:45.394928+08', '2025-07-11 10:38:45.394928+08');
INSERT INTO "role_user" ("id", "role_id", "user_id", "created_at", "updated_at") VALUES (1, 1, 1, '2025-07-11 10:27:30.464716+08', '2025-07-11 10:27:30.464716+08');
INSERT INTO "roles" ("id", "name", "created_at", "updated_at") VALUES (1, 'ADMIN', '2025-07-11 10:19:10.068803+08', '2025-07-11 10:19:10.068803+08');
INSERT INTO "roles" ("id", "name", "created_at", "updated_at") VALUES (2, 'USER', '2025-07-11 10:19:10.094897+08', '2025-07-11 10:19:10.094897+08');
INSERT INTO "users" ("id", "username", "password", "enabled", "created_at", "updated_at") VALUES (1, 'admin', '$2a$10$ZipLVeZelkEtd86V/rMhEOy9V2bwMP9.5sV1pTTB.BD30W5GIT00K', 't', '2025-07-11 10:19:21.958391+08', '2025-07-11 10:19:21.958391+08');

-- Reset auto-increment sequences to their maximum values
SELECT setval(pg_get_serial_sequence('"public"."permissions"', 'id'), (SELECT MAX(id) FROM "public"."permissions"));
SELECT setval(pg_get_serial_sequence('"public"."project_groups"', 'id'), (SELECT MAX(id) FROM "public"."project_groups"));
SELECT setval(pg_get_serial_sequence('"public"."projects"', 'id'), (SELECT MAX(id) FROM "public"."projects"));
SELECT setval(pg_get_serial_sequence('"public"."project_project_group"', 'id'), (SELECT MAX(id) FROM "public"."project_project_group"));
SELECT setval(pg_get_serial_sequence('"public"."role_permission"', 'id'), (SELECT MAX(id) FROM "public"."role_permission"));
SELECT setval(pg_get_serial_sequence('"public"."role_user"', 'id'), (SELECT MAX(id) FROM "public"."role_user"));
SELECT setval(pg_get_serial_sequence('"public"."roles"', 'id'), (SELECT MAX(id) FROM "public"."roles"));
SELECT setval(pg_get_serial_sequence('"public"."topology_warning_rules"', 'id'), (SELECT MAX(id) FROM "public"."topology_warning_rules"));
SELECT setval(pg_get_serial_sequence('"public"."user_project_group"', 'id'), (SELECT MAX(id) FROM "public"."user_project_group"));
SELECT setval(pg_get_serial_sequence('"public"."users"', 'id'), (SELECT MAX(id) FROM "public"."users"));

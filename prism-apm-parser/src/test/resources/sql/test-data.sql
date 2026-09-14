-- This file is intentionally left blank for tests that do not require initial data.
/* No initial data required for this test setup. */

-- INSERT INTO "field_content_replacer_rule"(id, service_name, component, span_kind, field_name, match_type, search_content, replacement, enabled) VALUES (9, '__ANY__', 'nginx', 0, 'actionName', 'regex', '^/api/staffs/diff/\d+$', '/api/staffs/{timestamp}', true);
-- INSERT INTO "field_content_replacer_rule"(id, service_name, component, span_kind, field_name, match_type, search_content, replacement, enabled) VALUES (7, '__ANY__', 'nginx', 0, 'actionName', 'regex', '^/api/staffs/MDEP\d{6}$', '/api/staffs/{mdmcode}', true);
--

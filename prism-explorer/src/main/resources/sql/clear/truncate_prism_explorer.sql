-- Clear all tables in the prism_explorer database (keep table structures)
-- Execution order: delete foreign-key dependent tables first, then base tables

TRUNCATE TABLE mock_data_usage CASCADE;
-- TRUNCATE TABLE prism_explorer_config CASCADE;
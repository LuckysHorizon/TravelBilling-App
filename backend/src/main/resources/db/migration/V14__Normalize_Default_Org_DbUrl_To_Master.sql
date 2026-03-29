-- Ensure existing default organization uses the configured master DB URL.
-- This remediates older installs where V12 seeded localhost.
UPDATE organizations
SET db_url = '${master_db_url}',
    updated_at = CURRENT_TIMESTAMP
WHERE slug = 'default'
  AND (
      db_url IS NULL
      OR db_url = ''
      OR db_url LIKE 'jdbc:postgresql://localhost:%'
      OR db_url LIKE 'postgresql://localhost:%'
      OR db_url LIKE 'postgres://localhost:%'
      OR db_url LIKE 'jdbc:postgresql://127.0.0.1:%'
      OR db_url LIKE 'postgresql://127.0.0.1:%'
      OR db_url LIKE 'postgres://127.0.0.1:%'
  );

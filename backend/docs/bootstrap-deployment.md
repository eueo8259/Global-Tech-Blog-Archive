# Archive Bootstrap Deployment

## Purpose

Flyway owns database structure and reference data. The bootstrap archive moves
locally collected `articles` and `article_ai_decisions` into an empty production
content database once, before the production crawler is enabled.

Collection run logs and database primary keys are not transferred. Company
relationships are restored through `company_key` values seeded by Flyway.

## Production Database Preparation

Flyway creates tables inside an existing database. An operator or deployment
platform must create the MySQL 8.4 database and application account first.

Example commands, executed by a MySQL administrator after replacing the host
and password placeholders:

```sql
CREATE DATABASE global_tech_blog_archive
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'archive_app'@'<application-host>' IDENTIFIED BY '<strong-password>';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES
    ON global_tech_blog_archive.*
    TO 'archive_app'@'<application-host>';
```

Configure MySQL and the JDBC connection to use the same timezone. Production
uses UTC by default. Supply `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` through
the deployment secret store. Do not grant server-wide administrative access.

Start the application once and confirm:

- Flyway applied every migration in `flyway_schema_history`.
- JPA schema validation completed successfully.
- `companies` and `blog_sources` contain the expected reference rows.
- `articles` and `article_ai_decisions` are empty before import.

## Local Flyway Transition

The previous local profile allowed Hibernate and `data-local.sql` to recreate
or delete application data. Before the first run of this Flyway version:

1. Export or back up any local data that must be kept.
2. Confirm that deleting the Docker MySQL volume is acceptable.
3. Recreate the local database volume.
4. Start the backend and confirm Flyway applied V1 and V2.
5. Run the crawler again to build the bootstrap source data.

Example destructive reset, run only after backup confirmation:

```powershell
docker compose down -v
docker compose up -d mysql
```

Do not reuse a database whose `flyway_schema_history` was created from a
different migration set. Flyway checksum validation must pass before crawling.

## Export

Run from `backend/` against the populated local database:

```powershell
.\gradlew.bat archiveBootstrapExport `
  -Pfile=C:\absolute\path\artifacts\bootstrap\archive-bootstrap-YYYYMMDD-HHmmss.ndjson
```

The command refuses relative paths and existing output files. It creates the
NDJSON archive and an adjacent `.sha256` file. On POSIX filesystems, both files
are restricted to owner read/write permissions.

Keep bootstrap files under `artifacts/bootstrap/`, which is ignored by Git.
Never include them in the application JAR, container image, or source control.

## Transfer And Import

For an SSH-accessible Linux host:

```bash
scp archive-bootstrap-*.ndjson* \
  user@prod-server:/var/lib/global-tech-blog-archive/bootstrap/

cd /var/lib/global-tech-blog-archive/bootstrap
sha256sum -c archive-bootstrap-YYYYMMDD-HHmmss.ndjson.sha256
```

If SSH is unavailable, use private artifact storage with equivalent access
control and checksum verification.

Run from the deployed backend directory:

```bash
./gradlew archiveBootstrapImport \
  -Pfile=/var/lib/global-tech-blog-archive/bootstrap/archive-bootstrap-YYYYMMDD-HHmmss.ndjson
```

The importer validates the checksum and full archive before writing. It then
requires both content tables to be empty and saves all records in one
transaction. Any error rolls back the entire import and returns a non-zero
process exit code.

Accept the import only when output contains:

```text
BOOTSTRAP IMPORT SUCCESS
failed=0
```

Compare reported counts with database row counts. Keep the local archive for
30 days after production verification, then delete it. Delete the server copy
immediately after verification.

## Reference Data Changes

Applied Flyway migrations are immutable. Add a new migration for every company
or source change:

- Add company/source: `V3__add_company_sources.sql`
- Change URL or display name: `V4__update_source_feed_url.sql`
- Stop collection: set `enabled=0` in a new migration
- Delete rows only after checking article foreign-key impact

Update the domain model and source strategy documents in the same change.
Environment-specific differences must not be introduced by editing an applied
migration.

## Performance Boundary

The MVP importer intentionally uses one transaction. Record the MySQL version,
machine profile, record counts, elapsed time, and peak memory for deployment
verification. Revisit batching when any condition is met:

- More than 10,000 total records
- Import takes longer than 60 seconds
- Transaction logs or memory usage affect production stability

The initial verification target is at least 1,000 articles and 2,000 AI
decisions.

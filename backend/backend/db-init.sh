#!/usr/bin/env bash
set -euo pipefail

SQLCMD="/opt/mssql-tools18/bin/sqlcmd"

echo "Waiting for SQL Server to accept connections..."
for i in {1..60}; do
  if $SQLCMD -S db -U sa -P "${MSSQL_SA_PASSWORD}" -Q "SELECT 1" -C >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

$SQLCMD -S db -U sa -P "${MSSQL_SA_PASSWORD}" -Q "SELECT 1" -C >/dev/null 2>&1 || {
  echo "ERROR: SQL Server did not become ready in time."
  exit 1
}

echo "Creating database if needed..."
$SQLCMD -b -S db -U sa -P "${MSSQL_SA_PASSWORD}" -C -Q "IF DB_ID(N'${DB_NAME}') IS NULL CREATE DATABASE [${DB_NAME}];"

echo "Creating login/user if needed + granting roles..."
$SQLCMD -b -S db -U sa -P "${MSSQL_SA_PASSWORD}" -C -d "${DB_NAME}" -Q "
DECLARE @login nvarchar(128) = N'${DB_USER}';
DECLARE @pwd   nvarchar(128) = N'${DB_PASSWORD}';

DECLARE @loginQuoted nvarchar(260) = N'[' + REPLACE(@login, N']', N']]') + N']';
DECLARE @pwdEsc      nvarchar(260) = REPLACE(@pwd, N'''', N'''''');

-- 1) Create server login if missing, otherwise ensure password matches
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = @login)
BEGIN
  DECLARE @sql nvarchar(max) =
    N'CREATE LOGIN ' + @loginQuoted + N' WITH PASSWORD = ''' + @pwdEsc + N''';';
  EXEC(@sql);
END
ELSE
BEGIN
  DECLARE @sqlAlt nvarchar(max) =
    N'ALTER LOGIN ' + @loginQuoted + N' WITH PASSWORD = ''' + @pwdEsc + N''';';
  EXEC(@sqlAlt);
END;

-- 2) Create database user if missing
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = @login)
BEGIN
  DECLARE @sql2 nvarchar(max) =
    N'CREATE USER ' + @loginQuoted + N' FOR LOGIN ' + @loginQuoted + N';';
  EXEC(@sql2);
END;

-- 3) Add roles only if not already member
IF NOT EXISTS (
  SELECT 1
  FROM sys.database_role_members rm
  JOIN sys.database_principals r ON rm.role_principal_id = r.principal_id
  JOIN sys.database_principals m ON rm.member_principal_id = m.principal_id
  WHERE r.name = N'db_datareader' AND m.name = @login
)
BEGIN
  EXEC(N'ALTER ROLE db_datareader ADD MEMBER ' + @loginQuoted + N';');
END;

IF NOT EXISTS (
  SELECT 1
  FROM sys.database_role_members rm
  JOIN sys.database_principals r ON rm.role_principal_id = r.principal_id
  JOIN sys.database_principals m ON rm.member_principal_id = m.principal_id
  WHERE r.name = N'db_datawriter' AND m.name = @login
)
BEGIN
  EXEC(N'ALTER ROLE db_datawriter ADD MEMBER ' + @loginQuoted + N';');
END;

IF NOT EXISTS (
  SELECT 1
  FROM sys.database_role_members rm
  JOIN sys.database_principals r ON rm.role_principal_id = r.principal_id
  JOIN sys.database_principals m ON rm.member_principal_id = m.principal_id
  WHERE r.name = N'db_ddladmin' AND m.name = @login
)
BEGIN
  EXEC(N'ALTER ROLE db_ddladmin ADD MEMBER ' + @loginQuoted + N';');
END;
"

echo "DB init OK"

param(
  [string]$Host = "127.0.0.1",
  [int]$Port = 3306,
  [string]$User = "root"
)

Set-Location $PSScriptRoot

# MEMORY: execute from sql directory so init_mvp.sql can resolve relative SOURCE paths (schema_mvp.sql and seed_mvp.sql).
mysql --host=$Host --port=$Port --user=$User --default-character-set=utf8mb4 < .\init_mvp.sql

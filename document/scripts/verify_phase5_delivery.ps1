$ErrorActionPreference = "Stop"

$requiredFiles = @(
    "document/delivery/phase5-delivery.md",
    "document/delivery/local-runbook.md",
    "document/delivery/demo-script.md",
    "document/delivery/review-notes.md",
    "document/api/optimization-api.md",
    "document/architecture/optimization-architecture.md",
    "document/performance/seckill-phase1c-report.md",
    "document/performance/phase2-performance-report.md",
    "document/jmeter/seckill-baseline.jmx",
    "document/jmeter/seckill-redis-lua-mq.jmx",
    "document/sql/seckill_baseline_schema.sql",
    "document/sql/seckill_baseline_reset.sql",
    "document/sql/seckill_baseline_verify.sql",
    "document/sql/seckill_redis_reset.sql",
    "document/sql/phase2_performance_indexes.sql",
    "document/scripts/seckill_redis_reset.ps1",
    "document/scripts/seckill_redis_verify.ps1",
    "document/scripts/run_seckill_phase1c.ps1",
    "document/scripts/run_phase2_performance.ps1"
)

$missing = @()
foreach ($file in $requiredFiles) {
    if (-not (Test-Path -LiteralPath $file)) {
        $missing += $file
    }
}

if ($missing.Count -gt 0) {
    Write-Host "[FAIL] Missing delivery files:" -ForegroundColor Red
    $missing | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
    exit 1
}

Write-Host "[PASS] Phase 5 delivery files are present." -ForegroundColor Green

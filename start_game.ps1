$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$moduleRoot = Join-Path $repoRoot "org.eclipse.papyrus.javagen.catan"
$global:LASTEXITCODE = 0
$mavenCmd = (Get-Command mvn.cmd -ErrorAction SilentlyContinue).Source

if (-not $mavenCmd) {
    throw "mvn.cmd was not found on PATH."
}

if (-not (Test-Path $moduleRoot)) {
    throw "Maven module not found: $moduleRoot"
}

Push-Location $moduleRoot
try {
    Write-Host "[Launcher] Compiling Java sources..."
    & $mavenCmd "-q" "-DskipTests" "compile"
    if ($LASTEXITCODE -ne 0) {
        throw "Maven compile failed."
    }

    Write-Host "[Launcher] Starting game..."
    & java "-cp" "target/classes" "Catan.HumanGameLauncher" "game.config" "visualize/state.json"
    if ($LASTEXITCODE -ne 0) {
        throw "Java launch failed."
    }
} finally {
    Pop-Location
}

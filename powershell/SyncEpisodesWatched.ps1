$ErrorActionPreference = [System.Management.Automation.ActionPreference]::Stop

if (-Not (Get-Module -ListAvailable -Name ImmersionTracker)) {
    Write-Host "Installing module..."
    Install-Module -Name 'ImmersionTracker'
}
else {
    Write-Host "Checking for updates..."
    Update-Module -Name 'ImmersionTracker'
}

try {
    Sync-Episodes-Watched -Username 'username' -Password 'password'
}
catch {
    Write-Host -ForegroundColor Red "An error occurred"
    Write-Host -ForegroundColor Red $_
}
finally {
    Write-Host
    Pause
}
$ErrorActionPreference = [System.Management.Automation.ActionPreference]::Stop

if (-Not (Get-Module -ListAvailable -Name ImmersionTracker)) {
    Write-Host "installing module..."
    Install-Module -Name 'ImmersionTracker'
}
else {
    Write-Host "checking for updates..."
    Update-Module -Name 'ImmersionTracker'
}

try {
    Sync-Episodes-Watched -Username 'username' -Password 'password'
}
finally {
    Pause
}
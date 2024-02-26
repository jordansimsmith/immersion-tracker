if (-Not (Get-Module -ListAvailable -Name ImmersionTracker)) {
    Write-Host "installing module..."
    Install-Module -Name 'ImmersionTracker'
}
else {
    Write-Host "checking for updates..."
    Update-Module -Name 'ImmersionTracker'
}

Sync-Episodes-Watched
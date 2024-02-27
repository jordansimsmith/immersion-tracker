function Sync-Episodes-Watched {
    [CmdletBinding(PositionalBinding = $false)]
    param (
        [Parameter(mandatory = $true)][string] $Username,
        [Parameter(mandatory = $true)][string] $Password
    )
    process {
        Write-Host "Hello 2 $Username $Password"
    }
}
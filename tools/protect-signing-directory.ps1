param([Parameter(Mandatory=$true)][string]$Directory)
$ErrorActionPreference = 'Stop'
# Restrict module discovery to this executable's own modules (PowerShell 5/7 may coexist).
$env:PSModulePath = Join-Path $PSHOME 'Modules'
$path = [System.IO.Path]::GetFullPath($Directory)
if (-not (Test-Path -LiteralPath $path)) { New-Item -ItemType Directory -Path $path -Force | Out-Null }
$item = Get-Item -LiteralPath $path -Force
if (-not $item.PSIsContainer -or ($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint)) { throw 'Signing backup must be a normal directory' }
$sid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User
& icacls.exe $path /inheritance:r /grant:r "*$($sid.Value):(OI)(CI)F" '*S-1-5-18:(OI)(CI)F' | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Cannot restrict signing backup directory permissions' }
$allowed = @($sid.Value, 'S-1-5-18')
$actual = Get-Acl -LiteralPath $path
if (-not $actual.AreAccessRulesProtected) { throw 'Signing backup still inherits permissions' }
foreach ($entryPath in @($item) + @(Get-ChildItem -LiteralPath $path -Force)) {
    foreach ($entry in (Get-Acl -LiteralPath $entryPath.FullName).Access) {
        $entrySid = $entry.IdentityReference.Translate([System.Security.Principal.SecurityIdentifier]).Value
        if ($entrySid -notin $allowed -or $entry.AccessControlType -ne 'Allow') { throw 'Unexpected signing-backup access rule' }
    }
}
Write-Output 'Signing backup directory and file ACLs verified'

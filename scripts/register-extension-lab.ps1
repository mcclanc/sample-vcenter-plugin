# Lab helper: register this repo's remote plug-in with vCenter (Windows / Windows Server).
# Same behavior as register-extension-lab.sh — uses extension-registration.bat + Java on PATH.
#
# Interactive: run in a normal console; press Enter to accept [bracketed] defaults.
# Non-interactive: $env:REGISTER_NON_INTERACTIVE = "1" and set $env:VC_PASSWORD, $env:PLUGIN_SERVER_TP (etc.).
# Update existing extension: $env:REGISTER_ACTION = "updatePlugin" (same PLUGIN_KEY).
#
# Requires: JDK/JRE on PATH (java), Broadcom html-client-sdk under repo html-client-sdk/

param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-RepoRoot {
    $scriptDir = $PSScriptRoot
    if (-not $scriptDir) { $scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path }
    return (Resolve-Path (Join-Path $scriptDir "..")).Path
}

function Test-IsInteractive {
    if ($env:REGISTER_NON_INTERACTIVE -eq "1") { return $false }
    try { return -not [Console]::IsInputRedirected } catch { return $true }
}

function Read-WithDefault {
    param([string]$Prompt, [string]$Default)
    $line = Read-Host "$Prompt [$Default]"
    if ([string]::IsNullOrWhiteSpace($line)) { return $Default }
    return $line
}

function Read-LabPassword {
    param([string]$Prompt)
    if ($PSVersionTable.PSVersion.Major -ge 7) {
        return Read-Host $Prompt -MaskInput
    }
    $sec = Read-Host $Prompt -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
}

function Test-PluginKey {
    param([string]$k)
    if ([string]::IsNullOrWhiteSpace($k)) {
        Write-Error "PLUGIN_KEY is empty after trimming." -ErrorAction Stop
    }
    if ($k.Length -gt 127) {
        Write-Error "PLUGIN_KEY is longer than 127 characters ($($k.Length)). Use a shorter reverse-DNS key." -ErrorAction Stop
    }
    if ($k -like "*..*") {
        Write-Error "PLUGIN_KEY must not contain consecutive dots. Got: $k" -ErrorAction Stop
    }
    if ($k -notmatch '^[a-z][a-z0-9]*(\.[a-z0-9]+)+$') {
        Write-Error @"
PLUGIN_KEY must look like reverse-DNS: only a-z, 0-9, dots; start with a letter;
each segment non-empty; no leading/trailing/double dots. Got: $k
Example: com.yourcompany.remote.paasforvcf
"@ -ErrorAction Stop
    }
    if ($k -like "com.example*") {
        Write-Error "com.example.* is rejected by many vCenter builds. Pick a key you own, e.g. com.yourorg.remote.paasforvcf" -ErrorAction Stop
    }
}

function Get-VcHostFromSdkUrl {
    param([string]$Url)
    try {
        $u = [Uri]$Url
        return $u.Host
    } catch {
        Write-Error "Invalid VC_SDK_URL: $Url" -ErrorAction Stop
    }
}

$Root = Get-RepoRoot
$ToolBat = Join-Path $Root "html-client-sdk\tools\vCenter plugin registration\prebuilt\extension-registration.bat"
if (-not (Test-Path -LiteralPath $ToolBat)) {
    Write-Error @"
Registration launcher not found:
  $ToolBat
Unpack Broadcom vSphere HTML Client SDK so html-client-sdk\ exists under the repo root.
"@ -ErrorAction Stop
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        $env:Path = "$(Join-Path $env:JAVA_HOME 'bin');$env:Path"
    }
}
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    $adoptiumRoot = "C:\Program Files\Eclipse Adoptium"
    if (Test-Path -LiteralPath $adoptiumRoot) {
        $jdkHome = Get-ChildItem -LiteralPath $adoptiumRoot -Directory -ErrorAction SilentlyContinue |
            Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName "bin\java.exe") } |
            Sort-Object Name -Descending |
            Select-Object -First 1
        if ($jdkHome) {
            $env:Path = "$(Join-Path $jdkHome.FullName 'bin');$env:Path"
        }
    }
}
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Error @"
java not found on PATH. Install a JRE/JDK (JDK 17+ recommended), then either:
  Close and reopen this terminal (PATH updates), or run Chocolatey:  refreshenv
  Or set JAVA_HOME to the JDK folder and add %JAVA_HOME%\bin to PATH.
Example:  choco install Temurin17 -y
"@ -ErrorAction Stop
}

# --- Defaults (align with register-extension-lab.sh) ---
$VC_SDK_URL = if ($env:VC_SDK_URL) { $env:VC_SDK_URL } else { "https://172.16.30.14/sdk" }
if ($env:VCENTER_IP) {
    $VC_SDK_URL = "https://$($env:VCENTER_IP)/sdk"
}

$DEFAULT_VC_USER = if ($env:VC_USER) { $env:VC_USER } else { "administrator@vsphere.local" }
$DEFAULT_PLUGIN_KEY = if ($env:PLUGIN_KEY) { $env:PLUGIN_KEY } else { "com.cmaclabs.remote.paasforvcf" }
$DEFAULT_PLUGIN_VERSION = if ($env:PLUGIN_VERSION) { $env:PLUGIN_VERSION } else { "1.0.0.0" }
$DEFAULT_PLUGIN_URL = if ($env:PLUGIN_URL) { $env:PLUGIN_URL } else { "https://192.168.68.5:8443/tanzu-hub-poc-ui/plugin.json" }
$DEFAULT_PLUGIN_COMPANY = if ($env:PLUGIN_COMPANY) { $env:PLUGIN_COMPANY } else { "Lab" }
$DEFAULT_PLUGIN_NAME = if ($env:PLUGIN_NAME) { $env:PLUGIN_NAME } else { "App Platform as a Service" }
$DEFAULT_PLUGIN_SUMMARY = if ($env:PLUGIN_SUMMARY) { $env:PLUGIN_SUMMARY } else { "Tanzu Hub install from vSphere Client" }

$VC_USER = $null
$VC_PASSWORD = $env:VC_PASSWORD
$PLUGIN_URL = $null
$PLUGIN_SERVER_TP = $env:PLUGIN_SERVER_TP
$PLUGIN_KEY = $null
$PLUGIN_VERSION = $null
$PLUGIN_NAME = $null
$PLUGIN_COMPANY = $null
$PLUGIN_SUMMARY = $null

if (Test-IsInteractive) {
    Write-Host "=== Register remote plug-in with vCenter ===" -ForegroundColor Cyan
    Write-Host "Press Enter at each prompt to accept the default in [brackets].`n"

    $VC_SDK_URL = Read-WithDefault "1) vCenter SDK URL (https://vcenter/sdk or https://IP/sdk)" $VC_SDK_URL

    if (-not $env:VCENTER_IP) {
        $rawIp = Read-WithDefault "   Optional: vCenter IP if DNS fails (leave empty to skip)" ""
        if (-not [string]::IsNullOrWhiteSpace($rawIp)) {
            $VC_SDK_URL = "https://$rawIp/sdk"
            Write-Host "   Using $VC_SDK_URL"
        }
    } else {
        $VC_SDK_URL = "https://$($env:VCENTER_IP)/sdk"
        Write-Host "   (VCENTER_IP is set → using $VC_SDK_URL)"
    }

    $VC_USER = Read-WithDefault "2) vCenter SSO username" $DEFAULT_VC_USER

    if ([string]::IsNullOrWhiteSpace($VC_PASSWORD)) {
        $VC_PASSWORD = Read-LabPassword "3) vCenter SSO password"
        if ([string]::IsNullOrWhiteSpace($VC_PASSWORD)) {
            Write-Error "Password cannot be empty." -ErrorAction Stop
        }
    } else {
        Write-Host "3) vCenter SSO password: (using existing `$env:VC_PASSWORD)"
    }

    $PLUGIN_URL = Read-WithDefault "4) Plug-in manifest URL (must end with .../plugin.json)" $DEFAULT_PLUGIN_URL

    if ([string]::IsNullOrWhiteSpace($PLUGIN_SERVER_TP)) {
        Write-Host ""
        Write-Host "5) Plug-in server HTTPS cert SHA-1 thumbprint (with colons), e.g. AA:BB:…"
        Write-Host "   Tip (PowerShell): see docs/REGISTRATION.md or openssl s_client."
        $PLUGIN_SERVER_TP = Read-Host "   Thumbprint (-st)"
        if ([string]::IsNullOrWhiteSpace($PLUGIN_SERVER_TP)) {
            Write-Error "Thumbprint is required (or set `$env:PLUGIN_SERVER_TP)." -ErrorAction Stop
        }
    } else {
        Write-Host "5) Plug-in server thumbprint: (using existing `$env:PLUGIN_SERVER_TP)"
    }

    $PLUGIN_KEY = Read-WithDefault "6) Extension key (-k); unique reverse-DNS; avoid com.example.*" $DEFAULT_PLUGIN_KEY
    $PLUGIN_VERSION = Read-WithDefault "7) Plug-in version (-v); bump for re-register / refresh" $DEFAULT_PLUGIN_VERSION
    $PLUGIN_NAME = Read-WithDefault "8) Display name in vSphere Client" $DEFAULT_PLUGIN_NAME
    $PLUGIN_COMPANY = Read-WithDefault "9) Company / publisher (-c)" $DEFAULT_PLUGIN_COMPANY
    $PLUGIN_SUMMARY = Read-WithDefault "10) Short summary (-s)" $DEFAULT_PLUGIN_SUMMARY
    Write-Host ""
} else {
    $VC_USER = if ($env:VC_USER) { $env:VC_USER } else { "administrator@vsphere.local" }
    if ([string]::IsNullOrWhiteSpace($env:VC_PASSWORD)) {
        Write-Error "Set `$env:VC_PASSWORD or run interactively (without REGISTER_NON_INTERACTIVE=1)." -ErrorAction Stop
    }
    $VC_PASSWORD = $env:VC_PASSWORD
    $PLUGIN_KEY = if ($env:PLUGIN_KEY) { $env:PLUGIN_KEY } else { "com.cmaclabs.remote.paasforvcf" }
    $PLUGIN_VERSION = if ($env:PLUGIN_VERSION) { $env:PLUGIN_VERSION } else { "1.0.0.0" }
    $PLUGIN_URL = if ($env:PLUGIN_URL) { $env:PLUGIN_URL } else { "https://192.168.70.27:8443/tanzu-hub-poc-ui/plugin.json" }
    if ([string]::IsNullOrWhiteSpace($env:PLUGIN_SERVER_TP)) {
        Write-Error "Set `$env:PLUGIN_SERVER_TP or run interactively." -ErrorAction Stop
    }
    $PLUGIN_SERVER_TP = $env:PLUGIN_SERVER_TP
    $PLUGIN_COMPANY = if ($env:PLUGIN_COMPANY) { $env:PLUGIN_COMPANY } else { "Lab" }
    $PLUGIN_NAME = if ($env:PLUGIN_NAME) { $env:PLUGIN_NAME } else { "App Platform as a Service" }
    $PLUGIN_SUMMARY = if ($env:PLUGIN_SUMMARY) { $env:PLUGIN_SUMMARY } else { "Tanzu Hub install from vSphere Client" }
}

# Normalize (same as bash)
$PLUGIN_KEY = ($PLUGIN_KEY -replace '\s', '').ToLowerInvariant()
$PLUGIN_VERSION = $PLUGIN_VERSION -replace '\s', ''
$PLUGIN_SERVER_TP = $PLUGIN_SERVER_TP -replace '\s', ''

Test-PluginKey $PLUGIN_KEY

$VC_HOST = Get-VcHostFromSdkUrl $VC_SDK_URL

# Clear proxy env for child JVM (mirrors bash)
@(
    "http_proxy", "https_proxy", "HTTP_PROXY", "HTTPS_PROXY", "ALL_PROXY", "all_proxy",
    "socks_proxy", "SOCKS_PROXY", "socksProxyHost", "socksProxyPort", "JAVA_TOOL_OPTIONS"
) | ForEach-Object {
    Remove-Item "env:$_" -ErrorAction SilentlyContinue
}

if (-not $env:EXTENSION_REGISTRATION_INSECURE) {
    $env:EXTENSION_REGISTRATION_INSECURE = "true"
}

$HTTP_NON_PROXY = if ($env:VC_NO_PROXY_HOSTS) { $env:VC_NO_PROXY_HOSTS } else { "${VC_HOST}|*.cmaclabs.com|localhost|127.*|[::1]" }
# CMD.EXE treats `|` as a pipe unless the -D values are quoted for java.exe.
$javaOptsExtra = @(
    "-Dextension.registration.insecure=true",
    "-Djava.net.preferIPv4Stack=true",
    "-Djava.net.useSystemProxies=false",
    "-Dhttp.nonProxyHosts=`"$HTTP_NON_PROXY`"",
    "-Dhttps.nonProxyHosts=`"$HTTP_NON_PROXY`"",
    "-Dhttp.proxyHost=",
    "-Dhttp.proxyPort=0",
    "-Dhttps.proxyHost=",
    "-Dhttps.proxyPort=0",
    "-DsocksProxyHost=",
    "-DsocksProxyPort=0"
)
if ($env:JAVA_OPTS) {
    $env:JAVA_OPTS = "$($env:JAVA_OPTS) $($javaOptsExtra -join ' ')"
} else {
    $env:JAVA_OPTS = $javaOptsExtra -join ' '
}

# Optional DNS check
if ($env:SKIP_DNS_CHECK -ne "1") {
    $isIPv4 = $VC_HOST -match '^\d{1,3}(\.\d{1,3}){3}$'
    if (-not $isIPv4) {
        try {
            [void][System.Net.Dns]::GetHostEntry($VC_HOST)
        } catch {
            Write-Error @"
DNS did not resolve vCenter host "$VC_HOST" (from VC_SDK_URL).
Registration will fail with UnknownHostException until this host resolves (VPN, hosts file, or correct FQDN).
Or set `$env:VCENTER_IP / VC_SDK_URL with https://<IP>/sdk.
To skip: `$env:SKIP_DNS_CHECK = "1"
"@ -ErrorAction Stop
        }
    }
}

$REGISTER_ACTION = if ($env:REGISTER_ACTION) { $env:REGISTER_ACTION } else { "registerPlugin" }
if ($REGISTER_ACTION -notin @("registerPlugin", "updatePlugin")) {
    Write-Error "REGISTER_ACTION must be registerPlugin or updatePlugin (got: $REGISTER_ACTION)." -ErrorAction Stop
}

if (Test-IsInteractive) {
    Write-Host "Registering with:"
    Write-Host "  VC_SDK_URL=$VC_SDK_URL"
    Write-Host "  VC_USER=$VC_USER"
    Write-Host "  PLUGIN_URL=$PLUGIN_URL"
    Write-Host "  PLUGIN_KEY=$PLUGIN_KEY PLUGIN_VERSION=$PLUGIN_VERSION"
    Write-Host "  PLUGIN_NAME=$PLUGIN_NAME"
    Write-Host "  -action $REGISTER_ACTION"
    Write-Host ""
}

if ($env:REGISTER_DEBUG -eq "1") {
    Write-Host "DEBUG argv to Java tool:"
    Write-Host "  -action $REGISTER_ACTION"
    Write-Host "  -url $VC_SDK_URL"
    Write-Host "  -username $VC_USER"
    Write-Host "  -password (hidden $($VC_PASSWORD.Length) chars)"
    Write-Host "  -key $PLUGIN_KEY"
    Write-Host "  -version $PLUGIN_VERSION"
    Write-Host "  -pluginUrl $PLUGIN_URL"
    Write-Host "  -serverThumbprint $PLUGIN_SERVER_TP"
    Write-Host "  -company $PLUGIN_COMPANY"
    Write-Host "  -name $PLUGIN_NAME"
    Write-Host "  -summary $PLUGIN_SUMMARY"
    Write-Host ""
}

$javaArgs = @(
    "-action", $REGISTER_ACTION,
    "-url", $VC_SDK_URL,
    "-username", $VC_USER,
    "-password", $VC_PASSWORD,
    "-key", $PLUGIN_KEY,
    "-version", $PLUGIN_VERSION,
    "-pluginUrl", $PLUGIN_URL,
    "-serverThumbprint", $PLUGIN_SERVER_TP,
    "-company", $PLUGIN_COMPANY,
    "-name", $PLUGIN_NAME,
    "-summary", $PLUGIN_SUMMARY
)

& $ToolBat @javaArgs
exit $LASTEXITCODE

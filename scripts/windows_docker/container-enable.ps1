$ErrorActionPreference = "Stop"

# Install containerization feature
if ($PSEdition -eq "Desktop") {
    Enable-WindowsOptionalFeature -Online -FeatureName containers -All
    Enable-WindowsOptionalFeature -Online -FeatureName Microsoft-Hyper-V -All
}
else {
    Install-WindowsFeature containers
}

# Create a startup entry after reboot to continue
$StartupLoc = "$env:USERPROFILE\AppData\Roaming\Microsoft\Windows\Start Menu\Programs\Startup\docker-setup.cmd"
Add-Content "$StartupLoc" "cd \'$PSScriptLocation\'`nPowerShell $PSScriptLocation\service-install.ps1`n"

# Install Docker
Invoke-WebRequest "https://download.docker.com/components/engine/windows-server/cs-1.12/docker.zip" -OutFile "$env:TEMP\docker.zip" -UseBasicParsing
Expand-Archive -Path "$env:TEMP\docker.zip" -DestinationPath $env:ProgramFiles -Force
[Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\Program Files\Docker", [EnvironmentVariableTarget]::Machine)

# Reboot
Restart-Computer -Force
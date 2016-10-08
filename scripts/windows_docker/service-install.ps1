$ErrorActionPreference = "Stop"

Remove-Item "$env:USERPROFILE/AppData/Roaming/Microsoft/Windows/Start Menu/Programs/Startup/docker-setup.cmd"

dockerd.exe --register-service
Start-Service docker
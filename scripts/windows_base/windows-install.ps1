$ErrorActionPreference = "Stop"
Import-Module NetSecurity

# Install Chocolatey
iwr https://chocolatey.org/install.ps1 -UseBasicParsing | iex

# Install packages from Chocolatey
choco install -y cmake altdrag 7zip.install steam jre8 git googlechrome bonjour

# Add things to PATH
[Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\Program Files\CMake\bin", [EnvironmentVariableTarget]::Machine)
[Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\Program Files\Git\bin", [EnvironmentVariableTarget]::Machine)


# Downloading programs and libraries

# Development tools
# Visual C++ Build Tools
# Microsoft really shouldn't be in charge of anything. This installer sucks, and there is no documentation for it.
Invoke-WebRequest http://go.microsoft.com/fwlink/?LinkId=691126 -OutFile "$env:TEMP/vcpp_buildtools.exe"
cmd /c "$env:TEMP/vcpp_buildtools.exe" /Q /S /NoRestart

# Development libraries for SDL2
Invoke-WebRequest https://www.libsdl.org/release/SDL2-devel-2.0.4-VC.zip -OutFile "$env:TEMP/SDL2.zip"
Expand-Archive -Force -Path "$env:TEMP\SDL2.zip" -DestinationPath C:/SDL2

# Development libraries for OpenAL
Invoke-WebRequest https://www.openal.org/downloads/OpenAL11CoreSDK.zip -OutFile "$env:TEMP/OpenAL.zip"
Expand-Archive -Force -Path "$env:TEMP\OpenAL.zip" -DestinationPath $env:TEMP/OpenAL_data
cmd /c "$env:TEMP/OpenAL_data/OpenAL11CoreSDK.exe" /s

# Development libraries for OpenSSL, opens a GUI
Invoke-WebRequest https://slproweb.com/download/Win64OpenSSL-1_1_0b.exe -OutFile "$env:TEMP/OpenSSL.exe"
cmd /c "$env:TEMP/OpenSSL.exe" /silent

# Qt Creator + Qt libraries, opens a GUI
Invoke-WebRequest http://download.qt.io/official_releases/online_installers/qt-unified-windows-x86-online.exe -OutFile "$env:TEMP/qt-installer.exe"
cmd /c "$env:TEMP/qt-installer.exe"

# Debugger and SDK
Invoke-WebRequest http://go.microsoft.com/fwlink/p/?LinkId=323507 -OutFile "$env:TEMP/win-sdk-81.exe"
cmd /c "$env:TEMP/win-sdk-81.exe" /q /features OptionId.WindowsDesktopDebuggers OptionId.WindowsSoftwareDevelopmentKit

Invoke-WebRequest http://go.microsoft.com/fwlink/p/?LinkID=822845 -OutFile "$env:TEMP/win-sdk-10.exe"
cmd /c "$env:TEMP/win-sdk-10.exe" /q /features OptionId.WindowsSoftwareDevelopmentKit

# DirectX SDK, necessary for Assimps for some reason, opens a GUI
Invoke-WebRequest https://download.microsoft.com/download/A/E/7/AE743F1F-632B-4809-87A9-AA1BB3458E31/DXSDK_Jun10.exe -OutFile "$env:TEMP/directx.exe"
cmd /c "$env:TEMP/directx.exe" /silent /quiet
rm "$env:TEMP/directx.exe"

# Inspecting PE executables, opens a GUI
Invoke-WebRequest http://www.ntcore.com/files/ExplorerSuite.exe -OutFile "$env:TEMP/cff.exe"
cmd /c "$env:TEMP/cff.exe" /silent


# Utilities

# Hamachi for inter-computer networking
Invoke-WebRequest https://secure.logmein.com/hamachi.msi -OutFile "$env:TEMP/hamachi.msi"
cmd /c msiexec /i "$env:TEMP/hamachi.msi" /quiet /norestart

# To make Windows Defender stop hogging I/O, we do this
# Thanks, Microsoft, you suck
Invoke-WebRequest http://amd64fre.com/pub/apps/NoDefender.zip -OutFile "$env:TEMP/nodefender.zip"
Expand-Archive -Force -Path "$env:TEMP\nodefender.zip" -DestinationPath $env:TEMP/NoDefender
cmd /c "$env:TEMP/NoDefender/NoDefender.exe"


# Games, all of these open windows because they are shit installers

Invoke-WebRequest "https://www.battle.net/download/getInstallerForGame?os=win&locale=enGB&version=LIVE&gameProgram=BATTLENET_APP" -OutFile "$env:TEMP/battlenet.exe"
cmd /c "$env:TEMP/battlenet.exe"

Invoke-WebRequest http://dlcl.gfsrv.net/gfl/GameforgeLiveSetup.exe -OutFile "$env:TEMP/glive.exe"
cmd /c "$env:TEMP/glive.exe"


# More features in Windows

# Enable remote PowerShell for management purposes
Enable-PSRemoting -Force

# Enable RDP
#./Enable-RDP.ps1

# Enable developer mode, for giggles
reg add "HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\AppModelUnlock" /t REG_DWORD /f /v "AllowDevelopmentWithoutDevLicense" /d "1"
reg add "HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\AppModelUnlock" /t REG_DWORD /f /v "AllowAllTrustedApps" /d "1"

# Enable pinging on IPv4 and IPv6
New-NetFirewallRule -Name Allow_Pingv4 -DisplayName “Echo Request – IPv4” -Protocol ICMPv4 -IcmpType 8 -Profile Any -Action Allow
New-NetFirewallRule -Name Allow_Pingv6 -DisplayName “Echo Request – IPv6” -Protocol ICMPv6 -IcmpType 8 -Profile Any -Action Allow


# Unused commands


#Invoke-WebRequest http://www.7-zip.org/a/7z1604-x64.exe -OutFile "$env:TEMP/7zip.exe"
#cmd /c "$env:TEMP/7zip.exe" /S

#Invoke-WebRequest http://javadl.oracle.com/webapps/download/AutoDL?BundleId=216403 -OutFile "$env:TEMP/java.exe"
#cmd /c "$env:TEMP/java.exe" /q /Q /s /S

#Invoke-WebRequest https://github.com/stefansundin/altdrag/releases/download/v1.1/AltDrag-1.1.exe -OutFile "$env:TEMP/altdrag.exe"
#cmd /c "$env:TEMP/altdrag.exe" /q /Q /s /S

#Invoke-WebRequest http://dl.google.com/chrome/install/375.126/chrome_installer.exe -OutFile "$env:TEMP/chrome.exe"
#cmd /c "$env:TEMP/chrome.exe" /silent /install



#Invoke-WebRequest https://steamcdn-a.akamaihd.net/client/installer/SteamSetup.exe -OutFile "$env:TEMP/steam.exe"
#cmd /c "$env:TEMP/steam.exe" /q /Q /s /S



# Bonjour is here because it provides a .local address for simpler connection
#Invoke-WebRequest http://support.apple.com/downloads/DL999/en_US/BonjourPSSetup.exe -OutFile "$env:TEMP/bonjour.exe"
#cmd /c "$env:TEMP/bonjour.exe" /quiet /norestart

#Invoke-WebRequest https://www.cygwin.com/setup-x86_64.exe -OutFile "$env:TEMP/cygwin.exe"
#cmd /c "$env:TEMP/cygwin.exe"

#Invoke-WebRequest https://github.com/git-for-windows/git/releases/download/v2.10.2.windows.1/Git-2.10.2-64-bit.exe -OutFile "$env:TEMP/git.exe"
#cmd /c "$env:TEMP/git.exe" /q /Q /s /S

# CMake, of course
#Invoke-WebRequest https://cmake.org/files/v3.7/cmake-3.7.0-rc1-win64-x64.msi -OutFile "$env:TEMP/CMake.msi"
#cmd /c msiexec /i "$env:TEMP/CMake.msi" /quiet /norestart
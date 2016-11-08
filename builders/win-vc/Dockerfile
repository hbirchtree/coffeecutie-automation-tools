#
# This Dockerfile will hopefully work on Windows. I live in hope.
#

FROM microsoft/windowsservercore

SHELL ["powershell", "-Command", "$ErrorActionPreference = 'Stop';"]

# Download VC++ Build Tools
RUN Invoke-WebRequest \
    http://go.microsoft.com/fwlink/?LinkId=691126 \
    -OutFile "$env:TEMP/vcpp_buildtools.exe"
RUN cmd /c "$env:TEMP/vcpp_buildtools.exe" /Full /Q /NoRestart

# Install Windows SDKs
#RUN Invoke-WebRequest \
#    http://go.microsoft.com/fwlink/p/?LinkId=323507 -OutFile "$env:TEMP/win-sdk-81.exe"
#RUN cmd /c "$env:TEMP/win-sdk-81.exe" /q \
#    /features OptionId.WindowsDesktopDebuggers OptionId.WindowsSoftwareDevelopmentKit
#RUN Invoke-WebRequest \
#    http://go.microsoft.com/fwlink/p/?LinkID=822845 -OutFile "$env:TEMP/win-sdk-10.exe"
#RUN cmd /c "$env:TEMP/win-sdk-10.exe" /q \
#    /features OptionId.WindowsSoftwareDevelopmentKit

# Download CMake
#RUN Invoke-WebRequest \
#    https://cmake.org/files/v3.7/cmake-3.7.0-rc1-win64-x64.msi \
#    -OutFile "$env:TEMP/CMake.msi"
#RUN cmd /c msiexec /i "$env:TEMP/CMake.msi" /quiet /norestart

# Download SDL2
RUN Invoke-WebRequest \
    https://www.libsdl.org/release/SDL2-devel-2.0.4-VC.zip \
    -OutFile "$env:TEMP/SDL2.zip"
RUN Expand-Archive -Force -Path "$env:TEMP\SDL2.zip" -DestinationPath "$env:TEMP/SDL2"
RUN mv "$env:TEMP/SDL2" "C:\SDL2"

# Download OpenAL
RUN Invoke-WebRequest \
    https://www.openal.org/downloads/OpenAL11CoreSDK.zip \
    -OutFile "$env:TEMP/OpenAL.zip"
RUN Expand-Archive -Path "$env:TEMP\OpenAL.zip" -DestinationPath $env:TEMP/OpenAL_data
RUN "$env:TEMP/OpenAL_data/OpenAL11CoreSDK.exe" /s

# Download OpenSSL
RUN Invoke-WebRequest \
    https://slproweb.com/download/Win64OpenSSL-1_1_0b.exe \
    -OutFile "$env:TEMP/OpenSSL.exe"
RUN cmd /c "$env:TEMP/OpenSSL.exe" /silent

# Download DirectX SDK
RUN Invoke-WebRequest https://download.microsoft.com/download/A/E/7/AE743F1F-632B-4809-87A9-AA1BB3458E31/DXSDK_Jun10.exe -OutFile "$env:TEMP/directx.exe"
RUN cmd /c "$env:TEMP/directx.exe" /silent /quiet
RUN rm "$env:TEMP/directx.exe"

# Install Git and CMake from Chocolatey
RUN iwr https://chocolatey.org/install.ps1 -UseBasicParsing | iex
RUN choco install -y cmake git

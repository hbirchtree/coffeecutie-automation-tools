#
# This Dockerfile will hopefully work on Windows. I live in hope.
#

FROM microsoft/nanoserver

# Download VC++ Build Tools
RUN Invoke-WebRequest \
    http://go.microsoft.com/fwlink/?LinkId=691126 \
    -OutFile "$env:TEMP/vcpp_buildtools.exe"

# Download CMake
RUN Invoke-WebRequest \
    https://cmake.org/files/v3.7/cmake-3.7.0-rc1-win64-x64.msi \
    -OutFile "$env:TEMP/CMake.msi"
    
# Download SDL2
RUN Invoke-WebRequest \
    https://www.libsdl.org/release/SDL2-devel-2.0.4-VC.zip \
    -OutFile "$env:TEMP/SDL2.zip"

# Download OpenAL
RUN Invoke-WebRequest \
    https://www.openal.org/downloads/oalinst.zip \
    -OutFile "$env:TEMP/OpenAL.zip"

# Download OpenSSL
RUN Invoke-WebRequest \
    https://slproweb.com/download/Win64OpenSSL-1_1_0b.exe \
    -OutFile "$env:TEMP/OpenSSL.exe"

# Extract archived data
# SDL2 is installed directly to C:, because fuck it
RUN Expand-Archive -Path "$env:TEMP\SDL2.zip" -DestinationPath C:/SDL2
RUN Expand-Archive -Path "$env:TEMP\OpenAL.zip" -DestinationPath $env:TEMP/OpenAL_data

# Install all the tools. Fucking hell. Silence them all.
RUN "$env:TEMP/vcpp_buildtools.exe" /S
RUN msiexec /i "$env:TEMP/CMake.msi" /quiet /norestart
RUN "$env:TEMP/OpenAL_data/oalinst.exe" /s
RUN "$env:TEMP/OpenSSL.exe" /s

# How to use Jenkins

1. Run the docker-compose configuration
   1. Wait for Jenkins to start, which takes a while
   2. Ensure that all plugins are installed properly from plugins.txt

2. Set up a seed job in Jenkins
   1. Ensure that all plugins are installed properly in Jenkins
   2. Ensure that the slave hosts are connected and filtered correctly

3. ???

4. Profit (unless builds fail left and right)


# Configuration of slaves
Non-Windows hosts will default to using the Ninja generator (unless modified).

 - Linux slaves
   - Use the 'ubuntu && x64' labels (32-bit is not supported)
 - Mac OSX slaves
   - Use the 'macintosh && x64' labels
 - Android cross-compiler slaves
   - Use the 'android' label (should be capable of compiling armv7a, arm64, x86)
   - It is recommended to use a Linux system to host this (Windows is not tested)
 - Windows slaves
   - Use the 'windows && x64' and 'windows && x86' labels

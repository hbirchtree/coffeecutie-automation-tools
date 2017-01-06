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
   - Use the 'linux && docker && ubuntu && x64' labels (32-bit is not supported)
 - Mac OSX slaves
   - Use the 'macintosh && clang && x64' labels
 - Raspberry Pi cross-compiler slaves
   - Use the 'linux && docker' label
 - Android cross-compiler slaves
   - Use the 'linux && docker && android' label (uses included Docker builder)
   - It is recommended to use a Linux system to host this (Windows is not tested)
 - Windows slaves
   - Use the 'windows && vcpp && x64' label, should have Windows 10 SDKs and etc. installed for UWP support. Some dependencies have static paths, because Windows is a pain to maintain.

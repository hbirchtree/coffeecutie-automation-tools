Raspberry Pi builder
--------------------

This container is fully autonomous; it will download its compilers along with their dependencies.

This container has the following volumes:
 - /raspi-sdk : a Raspberry Pi sysroot complete with necessary libraries
 - /home/${USER}/project : source directory
 - /home/${USER}/build : build directory

With the Coffeecutie project, mounting to these directories and starting the container with the default command will (hopefully) result in finished, ready to go binaries

Notes
-----
To run it, you will still need a Raspberry sysroot (containing headers and libraries).
See a section down below on creating a sysroot.

Roughly, building a Raspberry Pi sysroot
-------------------------------

1. Download a *basic* Raspberry Pi image of your choice (Raspbian, Arch, whatever, as long as the correct glibc version is in place)
    a. chroot into the image and download some more dependencies (preferrably using systemd's chroot solution if applicable)
    b. uninstall unnecessary dependencies from inside the chroot if possible
2. Strip out unnecessary or unwanted libraries, leaving only the necessary libraries
3. Mount it to /raspi-sdk
4. ???
5. Automate

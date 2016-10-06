#!/usr/bin/python3

from sys import argv;
import adb_devices as adb

if __name__ == "__main__":
    install_queue = []

    if len(argv) > 1:
        install_queue = argv[1:]

    for dev in adb.adb_get_devices():
        d = adb.get_dev()
        if d != None:
            for apk in install_queue:
                print("%s <- %s" % (d.get_identifier(),apk))
                d.install_apk(apk)

    exit(0)

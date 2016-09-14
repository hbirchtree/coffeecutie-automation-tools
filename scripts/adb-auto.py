#!/usr/bin/python3

from sys import argv;
import adb_devices as adb

if __name__ == "__main__":
    for dev in adb.adb_get_devices():
        d = adb.get_dev()
        if d != None:
            print("Got device: "+d.get_identifier())

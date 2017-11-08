#!/usr/bin/python3

import adb_devices as adb
from adb_devices import print_output
from argparse import ArgumentParser
import threading


def display_device(dev_uuid, args):
    device = adb.get_dev(dev_uuid)
    abi_string = ""
    for abi in device.abi_support:
        abi_string += abi + ","

    if len(abi_string) > 0:
        abi_string = abi_string[:abi_string.rfind(",")]

    print(
        """uuid=%s;abi=%s;sdk=%s;sdk-rel=%s;board=%s;platform=%s;product=%s;name=%s"""
        % (device.dev, abi_string, device.sdk_ver, device.sdk_rel,
           device.board, device.platform, device.product_name,
           device.name))


def screenshot_device(dev_uuid, args):
    target = args.extra_args[0].replace("%DEVICE_UUID%", dev_uuid)

    device = adb.get_dev(dev_uuid)
    # if device.get_screen_state():
    #     device.unlock_device()
    device.get_screenshot(target)
    # if not device.get_screen_state():
    #     device.lock_device()


def install_device(dev_uuid, args):
    apks = args.extra_args

    device = adb.get_dev(dev_uuid)
    for apk in apks:
        device.install_apk(apk)


def launch_activity(dev_uuid, args):
    activity_name = args.extra_args[0]

    device = adb.get_dev(dev_uuid)
    device.launch_app(activity_name)


def stop_activity(dev_uuid, args):
    activity_name = args.extra_args[0]

    device = adb.get_dev(dev_uuid)
    device.stop_app(activity_name)


def simulate_input(dev_uuid, args):
    device = adb.get_dev(dev_uuid)
    print_output(device.execute(["shell", "input"] + args.extra_args))


if __name__ == "__main__":
    args = ArgumentParser("ADB automation",
                          description="Android automation tool")

    args.add_argument("command", choices=["install", "info", "start", "stop",
                                          "lock", "unlock", "simulate",
                                          "list-devices", "screenshot"])
    args.add_argument("extra_args", nargs="*")

    args.add_argument("-s", metavar="device-uuid",
                      dest="uuid", default=None,
                      help="device selection")

    args = args.parse_args()

    print(args.extra_args)

    device_function = None

    if args.command == "list-devices":
        for uuid in adb.adb_get_devices():
            print(uuid)
        exit(0)
    elif args.command == "info":
        device_function = display_device
    elif args.command == "screenshot":
        device_function = screenshot_device
    elif args.command == "install":
        device_function = install_device
    elif args.command == "start":
        device_function = launch_activity
    elif args.command == "stop":
        device_function = stop_activity
    elif args.command == "simulate":
        device_function = simulate_input

    if device_function is None:
        exit(1)

    if args.uuid is None:
        tasks = []
        for uuid in adb.adb_get_devices():
            tasks.append(threading.Thread(name=uuid,
                                          target=device_function,
                                          args=(uuid, args)))
            #device_function(uuid, args)

        for task in tasks:
            task.start()

        for task in tasks:
            task.join()

    else:
        device_function(args.uuid, args)

    exit(0)

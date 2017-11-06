#!/usr/bin/python3

import adb_devices as adb
from argparse import ArgumentParser

if __name__ == "__main__":
    install_queue = []

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

    def display_device(dev_uuid):
        device = adb.get_dev(dev_uuid)
        abi_string = ""
        for abi in device.abi_support:
            abi_string += abi + ","

        if len(abi_string) > 0:
            abi_string = abi_string[:abi_string.rfind(",")]

        print(
            """uuid=%s;abi=%s;sdk=%s;sdk-rel=%s;board=%s;platform=%s;name=%s"""
            % (device.dev, abi_string, device.sdk_ver, device.sdk_rel,
               device.board, device.platform, device.name))

    def screenshot_device(dev_uuid, target):
        device = adb.get_dev(dev_uuid)
        if device.get_screen_state():
            device.unlock_device()
        device.get_screenshot(target)
        if not device.get_screen_state():
            device.lock_device()

    def install_device(dev_uuid, apks):
        device = adb.get_dev(dev_uuid)
        for apk in apks:
            device.install_apk(apk)

    def launch_activity(dev_uuid, activity_name):
        device = adb.get_dev(dev_uuid)
        device.launch_app(activity_name)

    def stop_activity(dev_uuid, activity_name):
        device = adb.get_dev(dev_uuid)
        device.stop_app(activity_name)

    def simulate_input(dev_uuid):
        device = adb.get_dev(dev_uuid)
        print(device.execute(["shell", "input"] + args.extra_args))

    if args.command == "list-devices":
        for uuid in adb.adb_get_devices():
            print(uuid)
        exit(0)
    elif args.command == "info":
        if args.uuid is None:
            for uuid in adb.adb_get_devices():
                display_device(uuid)
        else:
            display_device(args.uuid)
    elif args.command == "screenshot":
        template_name = args.extra_args.pop()
        if args.uuid is None:
            for uuid in adb.adb_get_devices():
                screenshot_device(uuid, template_name.replace("%DEVICE_UUID%", uuid))
        else:
            screenshot_device(args.uuid, template_name.replace("%DEVICE_UUID%", args.uuid))
    elif args.command == "install":
        if args.uuid is None:
            for uuid in adb.adb_get_devices():
                install_device(uuid, args.extra_args)
        else:
            install_device(args.uuid, args.extra_args)
    elif args.command == "start":
        if args.uuid is None:
            for uuid in adb.adb_get_devices():
                launch_activity(uuid, args.extra_args[0])
        else:
            launch_activity(args.uuid, args.extra_args[0])
    elif args.command == "stop":
        if args.uuid is None:
            for uuid in adb.adb_get_devices():
                stop_activity(uuid, args.extra_args[0])
        else:
            stop_activity(args.uuid, args.extra_args[0])
    elif args.command == "simulate":
        if args.uuid is None:
            for uuid in adb.adb_get_devices():
                simulate_input(uuid)
        else:
            simulate_input(args.uuid)

    exit(0)

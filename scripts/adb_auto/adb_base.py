#!/usr/bin/python3

from subprocess import Popen, PIPE;


def popen_proc(cmd):
    p = Popen(cmd, stdout=PIPE, stderr=PIPE);
    return p.communicate(), p


def popen_shell(cmd):
    p = Popen(cmd, shell=True, stdout=PIPE);
    return p.communicate(), p


def adb_dev_exec(dev, cmd, live_output=False):
    if live_output:
        (stdout, stderr), p = popen_shell(["adb", "-s", dev]+cmd)
    else:
        (stdout, stderr), p = popen_proc(["adb", "-s", dev]+cmd)

    p.wait()

    if p.returncode != 0:
        print(stdout.decode())
        raise RuntimeError(stderr.decode())

    return stdout.decode().replace('\r\n','\n')


def adb_get_prop(dev, prop):
    return adb_dev_exec(dev, ["shell", "getprop", prop]).replace('\n', '')

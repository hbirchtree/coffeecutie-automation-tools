#!/usr/bin/python3

from subprocess import Popen, PIPE;


def popen_proc(cmd):
    p = Popen(cmd, stdout=PIPE, stderr=PIPE);
    return p.communicate();


def popen_shell(cmd):
    p = Popen(cmd, shell=True, stdout=PIPE);
    return p.communicate();


def adb_dev_exec(dev,cmd, live_output=False):
    if live_output:
        stdout,stderr = popen_shell(["adb","-s",dev]+cmd)
    else:
        stdout, stderr = popen_proc(["adb","-s",dev]+cmd);
    return stdout.decode().replace('\r\n','\n');


def adb_get_prop(dev,prop):
    return adb_dev_exec(dev,["shell","getprop",prop]).replace('\n','');

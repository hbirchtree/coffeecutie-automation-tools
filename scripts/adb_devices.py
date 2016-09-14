#!/usr/bin/python3

from subprocess import Popen, PIPE;

def popen_proc(cmd):
    p = Popen(cmd, stdout=PIPE, stderr=PIPE);
    return p.communicate();

def adb_get_devices():
    stdout, stderr = popen_proc(["adb","devices"]);
    l1 = str(stdout).split('\\n')[1:];
    l = []
    for e in l1:
        if e != "'" and len(e) != 0:
            l = l + [e]
    for i in range(len(l)):
        l[i] = l[i].split('\\t')[0];
    return l;

def adb_dev_exec(dev,cmd):
    stdout, stderr = popen_proc(["adb","-s",dev]+cmd);
    return stdout.decode().replace('\r\n','');

def adb_get_prop(dev,prop):
    return adb_dev_exec(dev,["shell","getprop",prop]);

def adb_get_name(dev):
    return "%s %s" %   (adb_get_prop(dev,"ro.product.brand"),
                        adb_get_prop(dev,"ro.product.model"));

def adb_get_abis(dev):
    return adb_get_prop(dev,"ro.product.cpu.abilist").split(',');

def adb_get_sdkver(dev):
    return int(adb_get_prop(dev,"ro.build.version.sdk"));

def adb_get_sdkrel(dev):
    return adb_get_prop(dev,"ro.build.version.release");

def adb_get_board(dev):
    return adb_get_prop(dev,"ro.board.platform");

def adb_get_platform(dev):
    return adb_get_prop(dev,"ro.boot.hardware");

class device:
    def __init__(self,dev):
        self.dev = dev
        self.name = adb_get_name(dev)
        self.abi_support = adb_get_abis(dev)
        self.sdk_ver = adb_get_sdkver(dev)
        self.sdk_rel = adb_get_sdkrel(dev)
        self.board = adb_get_board(dev)
        self.platform = adb_get_platform(dev)

    def get_identifier(self):
        return "%s (%s, %s, %s)" % (self.name,self.dev,self.sdk_ver,self.platform);

    def install_apk(self,file):
        adb_dev_exec(self.dev,["install",file]);

def get_dev():
    return device(adb_get_devices()[0])

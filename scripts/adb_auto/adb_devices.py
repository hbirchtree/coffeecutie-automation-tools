#!/usr/bin/python3

from adb_get_data import *


class device:
    def __init__(self,dev):
        self.dev = dev
        self.name = adb_get_name(dev)
        self.abi_support = adb_get_abis(dev)
        self.sdk_ver = adb_get_sdkver(dev)
        self.sdk_rel = adb_get_sdkrel(dev)
        self.board = adb_get_board(dev)
        self.platform = adb_get_platform(dev)
        self.verbose = False


    def conditional_print(self,log):
        if self.verbose:
            print(log);


    def get_identifier(self):
        return "%s (%s, %s, %s)" % (self.name,self.dev,self.sdk_ver,self.platform);


    def install_apk(self,file):
        out = adb_dev_exec(self.dev,["install","-r",file]);
        self.conditional_print(out)


    def execute(self,cmd):
        return adb_dev_exec(self.dev,cmd);


    def get_screen_state(self):
        state = self.execute(["shell", "dumpsys", "usagestats"])

        start = state.index("mScreenOn") + len("mScreenOn") + 1
        end = state.index("\n", start)
        state = state[start:end]
        return state == "false"

    def unlock_device(self):
        self.execute(["shell", "input", "keyevent", "82"])

    def lock_device(self):
        self.execute(["shell", "input", "keyevent", "26"])

    def get_screenshot(self,target):
        dev_location = "/sdcard/screenshot.png";
        out = self.execute(["shell","screencap -p %s" % dev_location]);
        self.conditional_print(out)
        out = self.execute(["pull",dev_location,target]);
        self.conditional_print(out)
        out = self.execute(["shell","rm %s" % dev_location]);
        self.conditional_print(out)

    def display_logcat(self):
        adb_dev_exec(self.dev,["logcat"], live_output=True);

    def get_installed_packages(self):
        out = self.execute(["shell","pm list packages"]).split('\n');
        for i in range(len(out)):
            out[i] = out[i][8:];
        out.remove('');
        return out;

    def launch_app(self,pkg):
        # Before launching, unlock the device
        self.unlock_device()

        launcher = ""
        pkg_desc = self.execute(["shell","pm dump %s" % pkg]).split('\n');
        for i in range(len(pkg_desc)):
            if pkg_desc[i].strip().startswith('android.intent.action.MAIN'):
                launcher = pkg_desc[i+1].strip().split(" ")[1]
                break;
        if len(launcher) > 0:
            self.conditional_print("Launching %s -> %s" % (pkg,launcher));
            out = self.execute(["shell","am start -a %s -n %s" % (pkg,launcher)]);
            print(out)
        else:
            self.conditional_print("Failed to launch application, could not find intent");

    def stop_app(self, pkg):
        print(self.execute(["shell", "am", "force-stop", pkg]))
        # Lock device afterwards
        self.lock_device()

def get_num_devices():
    return len(adb_get_devices());


def get_dev(uuid):
    try:
        if uuid not in adb_get_devices():
            raise IndexError()
        return device(uuid);
    except IndexError:
        return None

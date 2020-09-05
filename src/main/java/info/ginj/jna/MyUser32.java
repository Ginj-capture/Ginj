package info.ginj.jna;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.win32.W32APIOptions;

public interface MyUser32 extends User32 {
    MyUser32 INSTANCE = Native.load("user32", MyUser32.class, W32APIOptions.DEFAULT_OPTIONS);

    BOOL EnumDisplayDevicesA(Pointer lpDevice, DWORD iDevNum, DISPLAY_DEVICE lpDisplayDevice, DWORD dwFlags);

    BOOL EnumDisplaySettingsExA(byte[] deviceName, int iModeNum, DEVMODE devMode, DWORD dwFlags);
}

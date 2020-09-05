package info.ginj.jna;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;

@Structure.FieldOrder({"cbSize", "DeviceName", "DeviceString", "StateFlags", "DeviceID", "DeviceKey"})
public class DISPLAY_DEVICE extends Structure {
    public WinDef.DWORD cbSize;
    public byte[] DeviceName = new byte[128];
    public byte[] DeviceString = new byte[128];
    public WinDef.DWORD StateFlags;
    public byte[] DeviceID = new byte[128];
    public byte[] DeviceKey = new byte[128];

    public DISPLAY_DEVICE() {
         cbSize = new WinDef.DWORD(this.size());
    }

    public String getDeviceName() {
        return new String(DeviceName);
    }

    public String getDeviceString() {
        return new String(DeviceString);
    }

    public int getStateFlags() {
        return StateFlags.intValue();
    }

    public String getDeviceID() {
        return new String(DeviceID);
    }

    public String getDeviceKey() {
        return new String(DeviceKey);
    }

    @Override
    public String toString() {
        return "DISPLAY_DEVICE{" +
                "DeviceName=" + getDeviceName() +
                ", DeviceString=" + getDeviceString() +
                ", StateFlags=" + getStateFlags() +
                ", DeviceID=" + getDeviceID() +
                ", DeviceKey=" + getDeviceKey() +
                '}';
    }
}

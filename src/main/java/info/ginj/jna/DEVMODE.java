package info.ginj.jna;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;


@Structure.FieldOrder({"dmDeviceName", "dmSpecVersion", "dmDriverVersion", "dmSize", "dmDriverExtra", "dmFields", "dmPositionX", "dmPositionY", "dmDisplayOrientation", "dmDisplayFixedOutput", "dmColor", "dmDuplex", "dmYResolution", "dmTTOption", "dmCollate", "dmFormName", "dmLogPixels", "dmBitsPerPel", "dmPelsWidth", "dmPelsHeight", "dmDisplayFlags", "dmDisplayFrequency", "dmICMMethod", "dmICMIntent", "dmMediaType", "dmDitherType", "dmReserved1", "dmReserved2", "dmPanningWidth", "dmPanningHeight"})
public class DEVMODE extends Structure {

    // iModeNum of EnumDisplaySettings/EnumDisplaySettingsEx
    public final static int ENUM_CURRENT_SETTINGS = -1;
    public final static int ENUM_REGISTRY_SETTINGS = -2;

    // dwFlags of EnumDisplaySettingsEx (default is 0)
    public final static int EDS_RAWMODE = 0x00000002;
    public final static int EDS_ROTATEDMODE = 0x00000004;


    public byte[] dmDeviceName = new byte[32];
    public WinDef.WORD dmSpecVersion;
    public WinDef.WORD dmDriverVersion;
    public WinDef.WORD dmSize;
    public WinDef.WORD dmDriverExtra;
    public WinDef.DWORD dmFields;

    public WinDef.DWORD  dmPositionX;
    public WinDef.DWORD  dmPositionY;
    public WinDef.DWORD  dmDisplayOrientation;
    public WinDef.DWORD  dmDisplayFixedOutput;
    public WinDef.WORD  dmColor;
    public WinDef.WORD  dmDuplex;
    public WinDef.WORD  dmYResolution;
    public WinDef.WORD  dmTTOption;
    public WinDef.WORD  dmCollate;
    public byte[] dmFormName = new byte[32];
    public WinDef.WORD  dmLogPixels;
    public WinDef.DWORD  dmBitsPerPel;
    public WinDef.DWORD  dmPelsWidth;
    public WinDef.DWORD  dmPelsHeight;
    public WinDef.DWORD  dmDisplayFlags;
    public WinDef.DWORD  dmDisplayFrequency;
    public WinDef.DWORD  dmICMMethod;
    public WinDef.DWORD  dmICMIntent;
    public WinDef.DWORD  dmMediaType;
    public WinDef.DWORD  dmDitherType;
    public WinDef.DWORD  dmReserved1;
    public WinDef.DWORD  dmReserved2;
    public WinDef.DWORD  dmPanningWidth;
    public WinDef.DWORD  dmPanningHeight;


    public DEVMODE() {
        dmSize = new WinDef.WORD(this.size());
        dmDriverExtra = new WinDef.WORD(0);
    }

    public String getDmDeviceName() {
        return new String(dmDeviceName);
    }

    public int getDmSpecVersion() {
        return dmSpecVersion.intValue();
    }

    public int getDmDriverVersion() {
        return dmDriverVersion.intValue();
    }

    public int getDmFields() {
        return dmFields.intValue();
    }

    public int getDmPositionX() {
        return dmPositionX.intValue();
    }

    public int getDmPositionY() {
        return dmPositionY.intValue();
    }

    public int getDmDisplayOrientation() {
        return dmDisplayOrientation.intValue();
    }

    public int getDmDisplayFixedOutput() {
        return dmDisplayFixedOutput.intValue();
    }

    public int getDmColor() {
        return dmColor.intValue();
    }

    public int getDmDuplex() {
        return dmDuplex.intValue();
    }

    public int getDmYResolution() {
        return dmYResolution.intValue();
    }

    public int getDmTTOption() {
        return dmTTOption.intValue();
    }

    public int getDmCollate() {
        return dmCollate.intValue();
    }

    public String getDmFormName() {
        return new String(dmFormName);
    }

    public int getDmLogPixels() {
        return dmLogPixels.intValue();
    }

    public int getDmBitsPerPel() {
        return dmBitsPerPel.intValue();
    }

    public int getDmPelsWidth() {
        return dmPelsWidth.intValue();
    }

    public int getDmPelsHeight() {
        return dmPelsHeight.intValue();
    }

    public int getDmDisplayFlags() {
        return dmDisplayFlags.intValue();
    }

    public int getDmDisplayFrequency() {
        return dmDisplayFrequency.intValue();
    }

    public int getDmICMMethod() {
        return dmICMMethod.intValue();
    }

    public int getDmICMIntent() {
        return dmICMIntent.intValue();
    }

    public int getDmMediaType() {
        return dmMediaType.intValue();
    }

    public int getDmDitherType() {
        return dmDitherType.intValue();
    }

    public int getDmReserved1() {
        return dmReserved1.intValue();
    }

    public int getDmReserved2() {
        return dmReserved2.intValue();
    }

    public int getDmPanningWidth() {
        return dmPanningWidth.intValue();
    }

    public int getDmPanningHeight() {
        return dmPanningHeight.intValue();
    }

    @Override
    public String toString() {
        return "DEVMODE{" +
                "dmDeviceName=" + getDmDeviceName() +
                ", dmSpecVersion=" + getDmSpecVersion() +
                ", dmDriverVersion=" + getDmDriverVersion() +
                ", dmSize=" + dmSize.intValue() +
                ", dmDriverExtra=" + dmDriverExtra.intValue() +
                ", dmFields=" + getDmFields() +
                ", dmPositionX=" + getDmPositionX() +
                ", dmPositionY=" + getDmPositionY() +
                ", dmDisplayOrientation=" + getDmDisplayOrientation() +
                ", dmDisplayFixedOutput=" + getDmDisplayFixedOutput() +
                ", dmColor=" + getDmColor() +
                ", dmDuplex=" + getDmDuplex() +
                ", dmYResolution=" + getDmYResolution() +
                ", dmTTOption=" + getDmTTOption() +
                ", dmCollate=" + getDmCollate() +
                ", dmFormName=" + getDmFormName() +
                ", dmLogPixels=" + getDmLogPixels() +
                ", dmBitsPerPel=" + getDmBitsPerPel() +
                ", dmPelsWidth=" + getDmPelsWidth() +
                ", dmPelsHeight=" + getDmPelsHeight() +
                ", dmDisplayFlags=" + getDmDisplayFlags() +
                ", dmDisplayFrequency=" + getDmDisplayFrequency() +
                ", dmICMMethod=" + getDmICMMethod() +
                ", dmICMIntent=" + getDmICMIntent() +
                ", dmMediaType=" + getDmMediaType() +
                ", dmDitherType=" + getDmDitherType() +
                ", dmReserved1=" + getDmReserved1() +
                ", dmReserved2=" + getDmReserved2() +
                ", dmPanningWidth=" + getDmPanningWidth() +
                ", dmPanningHeight=" + getDmPanningHeight() +
                '}';
    }
}

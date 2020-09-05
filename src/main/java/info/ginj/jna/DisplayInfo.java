package info.ginj.jna;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DisplayInfo {
    public static List<Rectangle> getMonitorList() {
        List<Rectangle> monitors = new ArrayList<>();
        WinDef.HDC hDC = User32.INSTANCE.GetDC(null);
        final WinUser.MONITORENUMPROC enumMonitorsCallback = (hmonitor, hdc, rect, lparam) -> {
            WinUser.MONITORINFOEX monitorInfoEx = new WinUser.MONITORINFOEX();
            User32.INSTANCE.GetMonitorInfo(hmonitor, monitorInfoEx); // From user32 - http://www.pinvoke.net/default.aspx/user32/GetMonitorInfo.html

            final String monitorName = new String(monitorInfoEx.szDevice);
            //System.out.println("Monitor '" + monitorName + "':");
            final WinDef.RECT rcMonitor = monitorInfoEx.rcMonitor;
            monitors.add(new Rectangle(rcMonitor.left, rcMonitor.top, rcMonitor.right - rcMonitor.left, rcMonitor.bottom - rcMonitor.top));

            return 1; // to continue enumeration
        };
        User32.INSTANCE.EnumDisplayMonitors(hDC, null, enumMonitorsCallback, new WinDef.LPARAM());

        return monitors;
    }
}

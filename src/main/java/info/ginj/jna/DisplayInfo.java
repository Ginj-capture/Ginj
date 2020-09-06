package info.ginj.jna;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DisplayInfo {
    /**
     * Call JNA and expose physical monitors as a high-level list of Rectangle objects.
     * Note that according to this post https://stackoverflow.com/questions/54912038/querying-windows-display-scaling
     * this code should return logical info, but in this context, it seems the call returns the physical info, so no further calls are needed.
     * @return a list of Rectangle, each representing the actual resolution of a physical monitor
     */
//    public static List<Rectangle> getMonitorList() {
//        List<Rectangle> monitors = new ArrayList<>();
//
//        // Get the device context of the desktop
//        WinDef.HDC hDC = User32.INSTANCE.GetDC(null);
//
//        // Prepare a callback for each found monitor
//        final WinUser.MONITORENUMPROC enumMonitorsCallback = (hmonitor, hdc, rect, lparam) -> {
//            WinUser.MONITORINFOEX monitorInfoEx = new WinUser.MONITORINFOEX();
//            User32.INSTANCE.GetMonitorInfo(hmonitor, monitorInfoEx); // From user32 - http://www.pinvoke.net/default.aspx/user32/GetMonitorInfo.html
//
//            //final String monitorName = new String(monitorInfoEx.szDevice);
//            //System.out.println("Monitor '" + monitorName + "':");
//            final WinDef.RECT rcMonitor = monitorInfoEx.rcMonitor;
//            monitors.add(new Rectangle(rcMonitor.left, rcMonitor.top, rcMonitor.right - rcMonitor.left, rcMonitor.bottom - rcMonitor.top));
//
//            return 1; // to continue enumeration
//        };
//
//        // Now enumerate all monitors, and call the above callback for each of them
//        User32.INSTANCE.EnumDisplayMonitors(hDC, null, enumMonitorsCallback, new WinDef.LPARAM());
//
//        return monitors;
//    }
    public static List<Rectangle> getWindowsMonitorList() {
        List<Rectangle> monitors = new ArrayList<>();

        // Enumerate all monitors, and call a code block for each of them
        // See https://docs.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-enumdisplaymonitors
        // See http://www.pinvoke.net/default.aspx/user32/EnumDisplayMonitors.html
        User32.INSTANCE.EnumDisplayMonitors(
                null, // => the virtual screen that encompasses all the displays on the desktop.
                null, // => don't clip the region
                (hmonitor, hdc, rect, lparam) -> {
                    // For each found monitor, get more information
                    // See https://docs.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-getmonitorinfoa
                    // See http://www.pinvoke.net/default.aspx/user32/GetMonitorInfo.html
                    WinUser.MONITORINFOEX monitorInfoEx = new WinUser.MONITORINFOEX();
                    User32.INSTANCE.GetMonitorInfo(hmonitor, monitorInfoEx);
                    // Retrieve its coordinates
                    final WinDef.RECT rcMonitor = monitorInfoEx.rcMonitor;
                    // And convert them to a Java rectangle, to be added to the list of monitors
                    monitors.add(new Rectangle(rcMonitor.left, rcMonitor.top, rcMonitor.right - rcMonitor.left, rcMonitor.bottom - rcMonitor.top));
                    // Then return "true" to continue enumeration
                    return 1;
                },
               null // => No additional info to pass as lparam to the callback
        );

        return monitors;
    }
}

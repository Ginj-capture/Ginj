### Partial and evolving, unordered, TODO list

#### Features :
 - ~~Implement multi-screen capture~~
 - ~~Allow positioning the "star" widget on a secondary display~~
 - React to resolution changes and taskbar move/show/hide
 - Support scaled displays (e.g. 125%)
 - ~~Open dialogs (capture editing, "more", "history", etc), on the display where the "star" widget is located~~
 - ~~Implement capture editing window~~
 - ~~Implement arrow overlay~~
 - ~~Implement rectangular overlays~~
 - ~~Implement Text overlay~~
 - Implement font size, style and color selection for text overlay
 - ~~Implement overlay resize and move~~
 - ~~Double-clicking one an overlay brings it to front ENHANCEMENT~~
 - ~~Draw pretty overlay handles~~
 - ~~Implement color chooser~~
 - ~~Add key shortcuts (e.g. DEL to remove overlay, CTRL-Z for undo and CTRL-Y / CTRL-SHIFT-Z for redo)~~
 - ~~Implement "exports"~~
 - ~~Implement disk export~~
 - ~~Implement Dropbox export~~
 - ~~Implement Google Photos export~~
 - ~~Exports should be made in a separate Dialog (with progress + and notification when done), with return to the main window in case of error~~
 - ~~Upon export completion, the notification window should allow "auto hide" checkbox + Close button~~
 - ~~Implement preferences~~
 - ~~Implement history with **editable** items~~
 - ~~Implement edit from history window~~
 - Finalize history window (export button)
 - ~~Load history contents asynchronously~~
 - ~~Persist StarWindow position~~
 - ~~Position ExportFrame and ExportCompletionFrame next to Star Window~~
 - ~~Implement target editing~~
 - In export completion window, replace plain text with html for the shared link to be clickable
 - ~~Improve "About" dialog (logo, clickable link to site, credits)~~
 - Implement video using ffmpeg & Jaffree
 - Implement "check for updates" from Java
 - Implement generic preference editor
 - Typing in a Text Overlay should make the overlay grow wider
 - Add Google Drive exporter
 - Add Youtube exporter
 - Add FTP exporter :-)
 - Implement tray icon and menu
 - Implement global shortcut
 - Implement load on startup
 - Implement windows detection (using JNA?)
 - ~~Close most frames with ESC (ex: History)~~
 - Implement fixed ratio to 16:9 (shift-drag) or 4:3 (ctrl-drag) + snap to resp 640x360,800x450,960x540,1280x720 or 320x240,400x300,640x480,800x600,1024x768 / ENHANCEMENT: in 4:3 1280x960
 - ? Should undo/redo change selection inside the Action methods (e.g change color, resize) ? - or completely deselect component after operation
 - Ahem, try on Mac and/or Linux

#### UI:
 - ~~Make yellowish icons for disabled buttons~~
 - ~~Look and feel of OK button in dialogs~~
 - ~~Paint title bar~~
 - ~~Radion buttons look and feel (used in Color chooser)~~
 - ~~Cursor look and feel (used in Color chooser)~~
 - ~~Fix scrollbar look and feel corner + thumb icon + colors + gap~~
 - Finalize look and feel of (File chooser (save as), Tables)
 - Add splash screen animation
 - ~~Use a bordered panel for CaptureSelectionFrame buttons~~
 - Switch from "close" to "fade out effect" in export completion window
 - ~~Upscale sun and sun-rays so that runtime downscale provides an anti-aliasing~~ 
 - ~~Build 3 main buttons at runtime based on a circle + icons (downscale provides an anti-aliasing)~~
 - or better yet draw them all by code (gradients etc) ?

#### Cleanup:
 - Remove EASynth resource dir
 - Remove useless EASynth classes, if any
 - Remove useless icons ?

#### Options ENHANCEMENT:
 - Remember previously used custom colors
 - Add optional "Speech Balloon" overlay
 - Add optional "Line" overlay (with CTRL to constrain)
 - Add optional "Oval" overlay
 - Shift should constrain handle move horizontally/vertically, Ctrl should resize symmetrically
 - Add overlays on video


package com.easynth.designer.laf.popup;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class EaSynthPopup extends Popup {
	
	public static final String POPUP_BACKGROUND_IMAGE = "POPUP_BACKGROUND_IMAGE";

	private Component contents;

	private int x;

	private int y;

	private Popup popup;

	private Container heavyContainer;

	private static BufferedImage popupBgImage;
	
	public EaSynthPopup() {
		super();
	}

	public EaSynthPopup(Component owner, Component contents, int x, int y, Popup popup) {
		this.contents = contents;
		this.popup = popup;
		this.x = x;
		this.y = y;

		Container parentContainer = contents.getParent();
		((JComponent) parentContainer).putClientProperty(POPUP_BACKGROUND_IMAGE, null);

		while (parentContainer != null) {
			if ((parentContainer instanceof JWindow)
					|| (parentContainer instanceof Panel)
					|| (parentContainer instanceof Window)) {
				this.heavyContainer = parentContainer;
				break;
			}
			parentContainer = parentContainer.getParent();
		}
		
		fixCursorForInternalFrame(owner);
		
	    if (this.heavyContainer != null && System.getProperty("os.name").startsWith("Mac")) {
	      this.heavyContainer.setBackground(new Color(16777216, true));
	      this.heavyContainer.setBackground(new Color(0, true));
	      if (this.heavyContainer instanceof JWindow) {
	        ((JWindow)this.heavyContainer).getRootPane().putClientProperty("apple.awt.draggableWindowBackground", Boolean.FALSE);
	      }
	    }
	}

	private void fixCursorForInternalFrame(Component owner) {
		if ((owner != null)	&& (owner instanceof JInternalFrame)) {
			final Container topContainer = ((JInternalFrame) owner).getTopLevelAncestor();
			final Cursor cursor = Cursor.getPredefinedCursor(0);
			if (topContainer instanceof JFrame) {
				((JFrame) topContainer).getGlassPane().setCursor(cursor);
				((JFrame) topContainer).getGlassPane().setVisible(false);
			} else if (topContainer instanceof JWindow) {
				((JWindow) topContainer).getGlassPane().setCursor(cursor);
				((JWindow) topContainer).getGlassPane().setVisible(false);
			} else if (topContainer instanceof JDialog) {
				((JDialog) topContainer).getGlassPane().setCursor(cursor);
				((JDialog) topContainer).getGlassPane().setVisible(false);
			} else if (topContainer instanceof JApplet) {
				((JApplet) topContainer).getGlassPane().setCursor(cursor);
				((JApplet) topContainer).getGlassPane().setVisible(false);
			}
		}
	}

	public void hide() {
		Object owner = (JComponent)this.contents.getParent();
		this.popup.hide();
		if (this.heavyContainer != null) {
			this.heavyContainer = null;
			while (owner != null) {
				if (owner instanceof JFrame) {
					((JFrame) owner).update(((Component) owner).getGraphics());
				}
				owner = ((Component) owner).getParent();
			}
		}
		this.contents = null;
		this.popup = null;
	}

	public Popup getPopup() {
		return this.popup;
	}

	public void show() {
		int i = ((this.contents instanceof JPopupMenu)) ? 1 : 0;
		if ((i != 0) && (this.heavyContainer == null))
			this.heavyContainer = this.contents.getParent();

		if (this.heavyContainer == null) {
			this.popup.show();
			return;
		}
		SwingUtilities.invokeLater(new CaptureBgRunnable());
	}

	private void captureBackground() {
		if (this.heavyContainer != null) {
			try {
				final Robot robot = new Robot();
				final Dimension localDimension = this.heavyContainer.getPreferredSize();
				final Rectangle localRectangle = new Rectangle(this.x, this.y, localDimension.width, localDimension.height);
				popupBgImage = robot.createScreenCapture(localRectangle);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private class CaptureBgRunnable implements Runnable {
		public void run() {
			if (contents != null && contents.getParent() instanceof JComponent) {
				captureBackground();
				((JComponent) contents.getParent()).putClientProperty(POPUP_BACKGROUND_IMAGE, popupBgImage);
			}
			final Popup popup = getPopup();
			if (popup != null) {
				popup.show();
			}
		}
	}
}
package com.easynth.designer.laf.popup;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;

public class EaSynthComboBoxPopupMenuListener implements PopupMenuListener {
	// ==============================================================================
	// Members
	// ==============================================================================
	private int bgTop = 4;
	private int bgLeft = 4;
	private int bgRight = 9;
	private int bgBottom = 10;
	
	// ==============================================================================
	// Constructors
	// ==============================================================================
	public EaSynthComboBoxPopupMenuListener() {
		super();
	}

	// ==============================================================================
	// Methods
	// ==============================================================================
	public void popupMenuCanceled(PopupMenuEvent e) {

	}
	
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		
	}
	
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		final JComboBox box = (JComboBox) e.getSource();
        final Object comp = box.getUI().getAccessibleChild(box, 0);
        if (!(comp instanceof JPopupMenu)) { 
        	return;
        }
        final JPopupMenu popupMenu = (JPopupMenu)comp;
        popupMenu.setBorder(null);
        if (popupMenu.getComponent(0) instanceof JScrollPane) {
        	final JScrollPane scrollPane = (JScrollPane)popupMenu.getComponent(0);
        	scrollPane.setBorder(BorderFactory.createEmptyBorder(bgTop, bgLeft, bgBottom, bgRight));
        	scrollPane.setOpaque(false);
        	scrollPane.getViewport().setOpaque(false);
        	if (popupMenu instanceof ComboPopup) {
        		final ComboPopup popup = (ComboPopup)popupMenu;
        		final JList list = popup.getList();
        		list.setBorder(null);
        		final Dimension size = list.getPreferredSize();
        		size.width = Math.max(box.getPreferredSize().width + bgLeft + bgRight, box.getWidth());
        		size.height = Math.min(scrollPane.getPreferredSize().height + bgTop + bgBottom, size.height + bgTop + bgBottom);
        		scrollPane.setPreferredSize(size);
        		scrollPane.setMaximumSize(size);
        	}
        }
	}
}

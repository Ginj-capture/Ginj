//################################################################################
//
//   Copyright 2008 EASYNTH (www.easynth.com)
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//
//################################################################################

package com.easynth.designer.laf.utils;

import javax.swing.*;
import javax.swing.plaf.synth.SynthContext;
import javax.swing.plaf.synth.SynthGraphicsUtils;
import java.awt.*;

/**
 * The GraphicsUtils class for EaSynth Look And Feel
 * 
 * @author EASYNTH
 */
public class EaSynthGraphicsUtils extends SynthGraphicsUtils {
	/**
	 * Draw line from (x1,y1) to (x2,y2), this method is used by tree component only.
	 * If we set the arc width and height to values that larger than 0, the line in 
	 * tree will contain curve, but it will also bring some painting issues when scrolling 
	 * the tree. So we set it to 0 by default.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param paintKey
	 * 		the key to identify the paint operation
	 * @param g
	 * 		the Graphics object to paint
	 * @param x1
	 * 		the x position of the start point of the line
	 * @param y1
	 * 		the y position of the start point of the line
	 * @param x2
	 * 		the x position of the end point of the line
	 * @param y2
	 * 		the y position of the end point of the line
	 */
	public void drawLine(final SynthContext context, final Object paintKey, final Graphics g, 
			final int x1, final int y1, final int x2, final int y2) {
		final UIDefaults uiDefaults = UIManager.getDefaults();
		final int arcWidth = uiDefaults.getInt("EaSynth.graphicsutils.drawline.arc.width");
		final int arcHeight = uiDefaults.getInt("EaSynth.graphicsutils.drawline.arc.height");
		final Color lineColor = uiDefaults.getColor("EaSynth.graphicsutils.drawline.color");
		if (lineColor != null) {
			g.setColor(lineColor);
		}
		if ("Tree.verticalLine".equals(paintKey)) {
			final int deltaY = y2 - y1 - arcHeight / 2;
			if (deltaY > 0) {
				super.drawLine(context, paintKey, g, x1, y1, x2, y1 + deltaY);
			} else {
				super.drawLine(context, paintKey, g, x1, y1, x2, y2);
			}
		} else if ("Tree.horizontalLine".equals(paintKey)) {
			if (Math.abs(x2 - x1) >= arcWidth / 2) {
				if (x1 < x2) {
					g.drawArc(x1, y2 - arcHeight, arcWidth, arcHeight, 180, 90);
					g.drawLine(x1 + arcWidth / 2, y2, x2, y2);
				} else {
					super.drawLine(context, paintKey, g, x1, y1, x2, y2);
				}
			} else {
				super.drawLine(context, paintKey, g, x1, y1, x2, y2);
			}
		} else {
			super.drawLine(context, paintKey, g, x1, y1, x2, y2);
		}
	}
	
	/**
	 * Draw the image with 9 grids scaling
	 * 
	 * @param g
	 * @param img
	 * @param dx1
	 * @param dy1
	 * @param dx2
	 * @param dy2
	 * @param insets
	 * @param paintCenter
	 */
	public static void drawImageWith9Grids(Graphics g, Image img, int dx1, int dy1, int dx2, int dy2, 
			Insets insets, boolean paintCenter) {
		
		final int imgWidth = img.getWidth(null);
		final int imgHeight = img.getHeight(null);
		
		// top-left cornor
		g.drawImage(img,
                dx1, dy1, dx1 + insets.left, dy1 + insets.top,
                0, 0, insets.left, insets.top, null);
		// top-right cornor
		g.drawImage(img,
                dx2 - insets.right, dy1, dx2, dy1 + insets.top,
                imgWidth - insets.right, 0, imgWidth, insets.top, null);
		// bottom-left cornor
		g.drawImage(img,
                dx1, dy2 - insets.bottom, dx1 + insets.left, dy2,
                0, imgHeight - insets.bottom, insets.left, imgHeight, null);
		// bottom-right cornor
		g.drawImage(img,
                dx2 - insets.right, dy2 - insets.bottom, dx2, dy2,
                imgWidth - insets.right, imgHeight - insets.bottom, imgWidth, imgHeight, null);
		// top border
		g.drawImage(img,
                dx1 + insets.left, dy1, dx2 - insets.right, dy1 + insets.top,
                insets.left, 0, imgWidth - insets.right, insets.top, null);
		// bottom border
		g.drawImage(img,
                dx1 + insets.left, dy2 - insets.bottom, dx2 - insets.right, dy2,
                insets.left, imgHeight - insets.bottom, imgWidth - insets.right, imgHeight, null);
		// left border
		g.drawImage(img,
                dx1, dy1 + insets.top, dx1 + insets.left, dy2 - insets.bottom,
                0, insets.top, insets.left, imgHeight - insets.bottom, null);
		// right border
		g.drawImage(img,
                dx2 - insets.right, dy1 + insets.top, dx2, dy2 - insets.bottom,
                imgWidth - insets.right, insets.top, imgWidth, imgHeight - insets.bottom, null);
		// center
		if (paintCenter) {
			g.drawImage(img,
					dx1 + insets.left, dy1 + insets.top, dx2 - insets.right, dy2 - insets.bottom,
	                insets.left, insets.top, imgWidth - insets.right, imgHeight - insets.bottom, null);
		}
	}
}

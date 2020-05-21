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

package com.easynth.designer.laf.painter;

import com.easynth.designer.laf.popup.EaSynthComboBoxPopupMenuListener;
import com.easynth.designer.laf.popup.EaSynthPopup;
import com.easynth.designer.laf.popup.EaSynthPopupFactory;
import com.easynth.designer.laf.utils.EaSynthGraphicsUtils;

import javax.swing.*;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.synth.SynthConstants;
import javax.swing.plaf.synth.SynthContext;
import javax.swing.plaf.synth.SynthPainter;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The painter class for EaSynth Look And Feel
 * 
 * @author EASYNTH
 */
public class EaSynthPainter extends SynthPainter {
	
	/**
	 * The week-key map to store the managed JComponents
	 * key: the JComponent
	 * value: its class name
	 */
	private static final Map<JComponent, String> MANAGED_OBJECT_MAP = new WeakHashMap<JComponent, String>();
	
	/**
	 * Install the popup factory to implement popup shadow
	 */
	public EaSynthPainter() {
		super();
		EaSynthPopupFactory.install();
	}

	/**
	 * This method can perform the gradient fill in the given region, fill orientation 
	 * can be specified.
	 * 
	 * @param g 
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to fill
	 * @param y
	 * 		the y position of the rectangle to fill
	 * @param w
	 * 		the width of the rectangle to fill
	 * @param h
	 * 		the height of the rectangle to fill
	 * @param clrFrom
	 * 		the starting color of the gradient fill
	 * @param clrTo
	 * 		the ending color of the gradient fill
	 * @param vertical
	 * 		true if fill vertically, otherwise false
	 */
	public static void gradientFillRect(final Graphics g, final int x, final int y, final int w, final int h, 
			final Color clrFrom, final Color clrTo, final boolean vertical) {
		final Graphics2D g2 = (Graphics2D)g.create();
		final double deltaRed = clrTo.getRed() - clrFrom.getRed();
		final double deltaGreen = clrTo.getGreen() - clrFrom.getGreen();
		final double deltaBlue = clrTo.getBlue() - clrFrom.getBlue();
		final double deltaAlpha = clrTo.getAlpha() - clrFrom.getAlpha();
		
		if (vertical) {
			for (int i = 1; i <= h; i ++) {
				final double ratio = ((double)i)/((double)h);
				final Color curColor = new Color( // NOPMD by EASYNTH
						clrFrom.getRed() + (int)(deltaRed * ratio), 
						clrFrom.getGreen() + (int)(deltaGreen * ratio), 
						clrFrom.getBlue() + (int)(deltaBlue * ratio),
						clrFrom.getAlpha() + (int)(deltaAlpha * ratio));
				g2.setPaint(curColor);
				g2.drawLine(x, y + i - 1, x + w - 1, y + i - 1);
			}
		} else {
			for (int i = 1; i <= w; i ++) {
				final double ratio = ((double)i)/((double)w);
				final Color curColor = new Color( // NOPMD by EASYNTH
						clrFrom.getRed() + (int)(deltaRed * ratio), 
						clrFrom.getGreen() + (int)(deltaGreen * ratio), 
						clrFrom.getBlue() + (int)(deltaBlue * ratio),
						clrFrom.getAlpha() + (int)(deltaAlpha * ratio));
				g2.setPaint(curColor);
				g2.drawLine(x + i - 1, y, x + i - 1, y + h - 1);
			}
		}
	}
	
	/**
	 * Paint the button border, that will be a round rectangle border.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 */
	public void paintButtonBorder(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h) {
		final UIDefaults uiDefaults = UIManager.getDefaults();
		final int arcWidth = uiDefaults.getInt("EaSynth.button.arc.width");
		final int arcHeight = uiDefaults.getInt("EaSynth.button.arc.height");
		Color borderColor = Color.BLACK;
		if ((context.getComponentState() & SynthConstants.DEFAULT) != 0){
            if ((context.getComponentState() & SynthConstants.PRESSED) != 0) {
            	borderColor = uiDefaults.getColor("EaSynth.button.border.color.default.pressed");
            } else if ((context.getComponentState() & SynthConstants.DISABLED) != 0) {
            	borderColor = uiDefaults.getColor("EaSynth.button.border.color.default.disabled");
            } else if ((context.getComponentState() & SynthConstants.MOUSE_OVER) != 0) {
            	borderColor = uiDefaults.getColor("EaSynth.button.border.color.default.mouseover");
            } else {
            	borderColor = uiDefaults.getColor("EaSynth.button.border.color.default.enabled");
            }
        } else {
            if ((context.getComponentState() & SynthConstants.PRESSED) != 0) {
            	borderColor = uiDefaults.getColor("EaSynth.button.border.color.pressed");
            } else if ((context.getComponentState() & SynthConstants.DISABLED) != 0) {
            	borderColor = uiDefaults.getColor("EaSynth.button.border.color.disabled");
            } else if ((context.getComponentState() & SynthConstants.MOUSE_OVER) != 0) {
            	borderColor = uiDefaults.getColor("EaSynth.button.border.color.mouseover");
            } else {
            	borderColor = uiDefaults.getColor("EaSynth.button.border.color.enabled");
            }
        }
		g.setColor(borderColor);
		g.drawRoundRect(x, y, w - 1, h - 1, arcWidth, arcHeight);
	}
	
	/**
	 * Paint the toggle button border, that will be the same with regular button border.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 */
	public void paintToggleButtonBorder(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h) {
		paintButtonBorder(context, g, x, y, w, h);
	}

	/**
	 * Paint the arrow button foreground.  Different icons will be loaded to paint the foreground.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 * @param direction 
	 * 		the direction of the arrow button
	 */
	public void paintArrowButtonForeground(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h, final int direction) {
		final StringBuilder iconKeyBuf = new StringBuilder("EaSynth.arrow.");
		
		switch (direction) {
		case SwingConstants.NORTH:
			iconKeyBuf.append("up.");
			break;
		case SwingConstants.WEST:
			iconKeyBuf.append("left.");
			break;
		case SwingConstants.SOUTH:
			iconKeyBuf.append("down.");
			break;
		case SwingConstants.EAST:
			iconKeyBuf.append("right.");
			break;
		default:
			break;
		}
		
		if ((context.getComponentState() & SynthConstants.PRESSED) != 0) {
			iconKeyBuf.append("pressed");
		} else if ((context.getComponentState() & SynthConstants.DISABLED) != 0) {
			iconKeyBuf.append("disabled");
		} else if ((context.getComponentState() & SynthConstants.MOUSE_OVER) != 0) {
			iconKeyBuf.append("mouseover");
		} else {
           	iconKeyBuf.append("enabled");
        }
		
		final ImageIcon icon = (ImageIcon) context.getStyle().getIcon(context, iconKeyBuf.toString());
		if (icon != null) {
			final int imgWidth = icon.getIconWidth();
			final int imgHeight = icon.getIconHeight();
			final int destPosX = (w - imgWidth) / 2;
			final int destPosY = (h - imgHeight) / 2;
			g.drawImage(icon.getImage(), destPosX, destPosY, destPosX + imgWidth, destPosY + imgHeight,  0, 0, imgWidth, imgHeight, null);
		}
	}

	/**
	 * Paint the root pane background.  The specified image will be tiled to fill the region.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 */
	public void paintRootPaneBackground(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h) {
		final ImageIcon icon = (ImageIcon) context.getStyle().getIcon(context, "EaSynth.rootpanel.bg.image");
		if (icon != null) {
			final Image image = icon.getImage();
			final int imgWidth = image.getWidth(null);
			final int imgHeight = image.getHeight(null);
			if (imgWidth > 0 && imgHeight > 0) {
				final BufferedImage bufImage = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
				bufImage.createGraphics().drawImage(image, 0, 0, null);
				final TexturePaint texturePaint = new TexturePaint(bufImage, 
						new Rectangle(0, 0, bufImage.getWidth(), bufImage.getHeight()));
				final Graphics2D g2 = (Graphics2D)g.create();
				g2.setPaint(texturePaint);
				g2.fill(g2.getClip());
			}
		}
	}

	/**
	 * This method is NOT painting the progress bar border, instead it is used to paint 
	 * the string for indeterminate progress bar, since the SynthProgressBarUI avoid painting 
	 * it when the progress bar is indeterminate, that seems not reasonable.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 */
	@SuppressWarnings("deprecation")
	public void paintProgressBarBorder(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h) {
		final JProgressBar progressBar = (JProgressBar)context.getComponent();
		if (progressBar.isIndeterminate() && progressBar.isStringPainted()) {
			final boolean paintTextWhenIndeterminate = context.getStyle().getBoolean(context, "EaSynth.progressbar.indeterminate.paintstring", false);
			final String textToPaint = progressBar.getString();
			if (paintTextWhenIndeterminate && textToPaint != null) {
				final Class synthProgBarUIClass = progressBar.getUI().getClass();
				try {
					final Method paintTextMethod = synthProgBarUIClass.getDeclaredMethod("paintText", new Class[]{SynthContext.class, Graphics.class, String.class});
					if (paintTextMethod != null) {
						// paint text background first
						try {
							final Font font = context.getStyle().getFont(context);
							final FontMetrics metrics = Toolkit.getDefaultToolkit().getFontMetrics(font);
							final int strLength = context.getStyle().getGraphicsUtils(context).computeStringWidth(context, font, metrics, textToPaint);
							final Rectangle bounds = progressBar.getBounds();
							final Rectangle textRect = new Rectangle(
							        (bounds.width / 2) - (strLength / 2),
							        (bounds.height - (metrics.getAscent() + metrics.getDescent())) / 2,
							        strLength, metrics.getAscent() + metrics.getDescent());
							Insets bgInsets = (Insets)context.getStyle().get(context, "EaSynth.progressbar.bg.insets");
							if (bgInsets == null) {
								bgInsets = new Insets(0, 0, 0, 0);
							}
							if ((progressBar.getOrientation() == JProgressBar.HORIZONTAL && textRect.y >= bgInsets.top) 
									|| (progressBar.getOrientation() == JProgressBar.VERTICAL && textRect.x >= bgInsets.left)) {
								g.setColor(new Color(224, 212, 192));
								g.fillRect(textRect.x, textRect.y, textRect.width, textRect.height);
							}
						} catch (Exception e) {
							// just give up if something wrong
						}
						// paint the text
						paintTextMethod.setAccessible(true);
						paintTextMethod.invoke(progressBar.getUI(), new Object[]{context, g, textToPaint});
						paintTextMethod.setAccessible(false);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Paint the progress bar background, that will be a round rectangle region.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 */
	public void paintProgressBarBackground(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h) {
		final Graphics2D g2 = (Graphics2D) g.create();
		final ImageIcon bgIcon = (ImageIcon) context.getStyle().getIcon(context, "EaSynth.progressbar.background.image");
		final Insets bgInsets = (Insets)context.getStyle().get(context, "EaSynth.progressbar.bg.insets");
		if (bgIcon != null) {
			EaSynthGraphicsUtils.drawImageWith9Grids(g, bgIcon.getImage(), 
					x, y, x + w, y + h, 
					(bgInsets == null) ? new Insets(0, 0, 0, 0) : bgInsets, true);
		} else {
			final UIDefaults uiDefaults = UIManager.getDefaults();
			final Color bgColor = uiDefaults.getColor("EaSynth.progressbar.background.color");
			if (bgColor != null) {
				final int arcWidth = uiDefaults.getInt("EaSynth.progressbar.arc.width");
				final int arcHeight = uiDefaults.getInt("EaSynth.progressbar.arc.height");
				g2.setPaint(bgColor);
				g2.fillRoundRect(x, y, w, h, arcWidth, arcHeight);
			}
		}
		
		final JProgressBar progressBar = (JProgressBar) context.getComponent();
		final ImageIcon icon = (ImageIcon) context.getStyle().getIcon(context, "EaSynth.progressbar.indication.image");
		if (icon != null && w > 0 && h > 0) {
			final int imgWidth = icon.getIconWidth();
			final int imgHeight = icon.getIconHeight();
			if (JProgressBar.HORIZONTAL == progressBar.getOrientation()) {
				g2.setPaint(new Color(0, 0, 0, 130));
	        	g2.drawLine(x + imgWidth / 2, (y + h) / 2 - 1, x + w - imgWidth / 2, (y + h) / 2 - 1);
	        	g2.setPaint(new Color(255, 255, 255, 130));
	        	g2.drawLine(x + imgWidth / 2, (y + h) / 2 + 1, x + w - imgWidth / 2, (y + h) / 2 + 1);
	        	g2.setPaint(new Color(0, 0, 0, 180));
	        	g2.drawLine(x + imgWidth / 2, (y + h) / 2, x + w - imgWidth / 2, (y + h) / 2);
			} else {
				g2.setPaint(new Color(0, 0, 0, 130));
				g2.drawLine((x + w) / 2 - 1, y + imgHeight / 2, (x + w) / 2 - 1, y + h - imgHeight / 2);
				g2.setPaint(new Color(255, 255, 255, 130));
				g2.drawLine((x + w) / 2 + 1, y + imgHeight / 2, (x + w) / 2 + 1, y + h - imgHeight / 2);
				g2.setPaint(new Color(0, 0, 0, 180));
				g2.drawLine((x + w) / 2, y + imgHeight / 2, (x + w) / 2, y + h - imgHeight / 2);
			}
		}
	}

	/**
	 * Paint the progress bar foreground, that consist of progress line and the indication image.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 * @param orientation
	 * 		the orientation of the progressbar
	 */
	public void paintProgressBarForeground(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h, final int orientation) {
        final Graphics2D g2 = (Graphics2D) g.create();
        final ImageIcon icon = (ImageIcon) context.getStyle().getIcon(context, "EaSynth.progressbar.indication.image");
        final JProgressBar progressBar = (JProgressBar) context.getComponent();
        final boolean isDisabled = (context.getComponentState() & SynthConstants.DISABLED) != 0;
        final UIDefaults uiDefaults = UIManager.getDefaults();
		Color lineColor = uiDefaults.getColor("EaSynth.progressbar.line.color");
		if (lineColor == null) {
			lineColor = new Color(255, 255, 255, 130);
		}
		if (icon != null && w > 0 && h > 0) {
			final int imgWidth = icon.getIconWidth();
			final int imgHeight = icon.getIconHeight();
			if (JProgressBar.HORIZONTAL == orientation) {
				if (!progressBar.isIndeterminate() && w >= imgWidth) {
					// draw the progress line
			        g2.setPaint(lineColor.brighter());
			        g2.drawLine(x + imgWidth / 2, (y + h) / 2 - 1, x + w - imgWidth / 2, (y + h) / 2 - 1);
			        g2.setPaint(lineColor.darker());
			        g2.drawLine(x + imgWidth / 2, (y + h) / 2 + 1, x + w - imgWidth / 2, (y + h) / 2 + 1);
			        g2.setPaint(lineColor);
			        g2.drawLine(x + imgWidth / 2, (y + h) / 2, x + w - imgWidth / 2, (y + h) / 2);
				}
				
				if (!isDisabled) {
					// draw the indication 
					int destPosX = x + w - imgWidth;
					if (progressBar.isIndeterminate()) {
						// need to re-calculate the x position
						final int midRange = progressBar.getWidth() - w;
						final int curMid = imgWidth / 2 + (progressBar.getWidth() - imgWidth) * x / midRange;
						destPosX = curMid - imgWidth / 2;
					}
					if (destPosX >= 0) {
						final int destPosY = y + (h - imgHeight) / 2;
						g.drawImage(icon.getImage(), destPosX, destPosY, destPosX + imgWidth, destPosY + imgHeight,  0, 0, imgWidth, imgHeight, null);
					}
				}
			} else {
				if (!progressBar.isIndeterminate() && h >= imgHeight) {
					// draw the progress line
					g2.setPaint(lineColor.brighter());
					g2.drawLine((x + w) / 2 - 1, y + imgHeight / 2, (x + w) / 2 - 1, y + h - imgHeight / 2);
					g2.setPaint(lineColor.darker());
					g2.drawLine((x + w) / 2 + 1, y + imgHeight / 2, (x + w) / 2 + 1, y + h - imgHeight / 2);
					g2.setPaint(lineColor);
					g2.drawLine((x + w) / 2, y + imgHeight / 2, (x + w) / 2, y + h - imgHeight / 2);
				}
				
				if (!isDisabled) {
					// draw the indication 
					final int destPosX = x + (w - imgWidth) / 2;
					int destPosY = y;
					if (progressBar.isIndeterminate()) {
						// need to re-calculate the y position
						final int midRange = progressBar.getHeight() - h;
						final int curMid = imgHeight / 2 + (progressBar.getHeight() - imgHeight) * y / midRange;
						destPosY = curMid - imgHeight / 2;
					}
					g.drawImage(icon.getImage(), destPosX, destPosY, destPosX + imgWidth, destPosY + imgHeight,  0, 0, imgWidth, imgHeight, null);
				}
			}
		}
	}

	/**
	 * Paint the internal frame border.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 */
	public void paintInternalFrameBorder(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h) {
		final UIDefaults uiDefaults = UIManager.getDefaults();
		Color mainColor = uiDefaults.getColor("EaSynth.internalframe.main.color");
		if (mainColor == null) {
			mainColor = Color.GRAY;
		}
		g.setColor(mainColor.darker());
		g.drawLine(x, y + 1, x, h - 2);
        g.drawLine(x + w - 1, y + 1, x + w - 1, h - 2);
        g.drawLine(x + 1, y, x + w - 2, y);
        g.drawLine(x + 1, y + h - 1, x + w - 2, y + h - 1);
        
        g.setColor(mainColor.brighter());
		g.drawLine(x + 2, y + 3, x + 2, h - 4);
        g.drawLine(x + w - 3, y + 3, x + w - 3, h - 4);
        g.drawLine(x + 3, y + 2, x + w - 4, y + 2);
        g.drawLine(x + 3, y + h - 3, x + w - 4, y + h - 3);
	}

	/**
	 * Paint the title pane border for internal frame.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 */
	public void paintInternalFrameTitlePaneBorder(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h) {
		final UIDefaults uiDefaults = UIManager.getDefaults();
		Color mainColor = uiDefaults.getColor("EaSynth.internalframe.main.color");
		if (mainColor == null) {
			mainColor = Color.GRAY;
		}
		g.setColor(mainColor.darker());
        g.drawLine(x, y + h - 1, x + w, y + h - 1);
	}

	/**
	 * Paint the internal frame background.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 */
	public void paintInternalFrameBackground(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h) {
		final UIDefaults uiDefaults = UIManager.getDefaults();
		Color mainColor = uiDefaults.getColor("EaSynth.internalframe.main.color");
		if (mainColor == null) {
			mainColor = Color.GRAY;
		}
		g.setColor(mainColor);
		g.fillRect(x, y, w, h);
	}

	/**
	 * Paint the title pane background for internal frame.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 */
	public void paintInternalFrameTitlePaneBackground(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h) {
		paintInternalFrameBackground(context, g, x, y, w, h);
		// paint title if it is JRE1.5
		final String javaVersion = System.getProperty("java.version");
		if(javaVersion.compareTo("1.6") < 0) {
			final int titleSpacing = context.getStyle().getInt(context, "InternalFrameTitlePane.titleSpacing", 0);
			
			// for normal frame
			Component comp = context.getComponent();
			while (!(comp instanceof JInternalFrame) && comp != null) {
				comp = comp.getParent();
			}
			if (comp instanceof JInternalFrame) {
				final JInternalFrame frame = (JInternalFrame) comp;
				if (frame.getTitle() != null) {
					g.setColor(Color.BLACK);
					g.drawString(frame.getTitle(), 20 + titleSpacing, 15);
				}
			}
			
			// for desktop icon
			comp = context.getComponent();
			while (!(comp instanceof JInternalFrame.JDesktopIcon) && comp != null) {
				comp = comp.getParent();
			}
			if (comp instanceof JInternalFrame.JDesktopIcon) {
				final JInternalFrame frame = ((JInternalFrame.JDesktopIcon) comp).getInternalFrame();
				if (frame.getTitle() != null) {
					g.setColor(Color.BLACK);
					g.drawString(frame.getTitle(), 20 + titleSpacing, 15);
				}
			}
		}
	}

	/**
	 * Paint the track background for scroll bar.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 */
	public void paintScrollBarTrackBackground(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h) {
		final UIDefaults uiDefaults = UIManager.getDefaults();
		Color sideColor = uiDefaults.getColor("EaSynth.scrollbar.track.sidecolor");
		if (sideColor == null) {
			sideColor = Color.white;
		}
		Color centerColor = uiDefaults.getColor("EaSynth.scrollbar.track.centercolor");
		if (centerColor == null) {
			centerColor = Color.LIGHT_GRAY;
		}
		final JScrollBar scrollBar = (JScrollBar)context.getComponent();
		if (scrollBar.getOrientation() == JScrollBar.VERTICAL) {
			gradientFillRect(g, x, y, w / 2, h, sideColor, centerColor, false);
			gradientFillRect(g, x + w / 2, y, w / 2, h, centerColor, sideColor, false);
		} else {
			gradientFillRect(g, x, y, w, h / 2, sideColor, centerColor, true);
			gradientFillRect(g, x, y + h / 2, w, h / 2, centerColor, sideColor, true);
		}
	}
	
	/**
	 * Paint the track border for scroll bar.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 */
	public void paintScrollBarTrackBorder(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h) {
		final UIDefaults uiDefaults = UIManager.getDefaults();
		Color borderColor = uiDefaults.getColor("EaSynth.scrollbar.track.bordercolor");
		if (borderColor == null) {
			borderColor = Color.DARK_GRAY;
		}
		g.setColor(borderColor);
		g.draw3DRect(x, y, w - 1, h - 1, false);
	}

	/**
	 * Paint the thumb background for scroll bar.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 * @param orientation
	 * 		the orientation of the scroll bar
	 */
	public void paintScrollBarThumbBackground(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h, final int orientation) {
		final UIDefaults uiDefaults = UIManager.getDefaults();
		final int borderThick = uiDefaults.getInt("EaSynth.scrollbar.thumb.borderthick");
		Color sideColor = uiDefaults.getColor("EaSynth.scrollbar.thumb.sidecolor");
		if (sideColor == null) {
			sideColor = Color.LIGHT_GRAY;
		}
		Color centerColor = uiDefaults.getColor("EaSynth.scrollbar.thumb.centercolor");
		if (centerColor == null) {
			centerColor = Color.white;
		}
		final JScrollBar scrollBar = (JScrollBar)context.getComponent();
		final boolean isVertical = scrollBar.getOrientation() == JScrollBar.VERTICAL;
		if (isVertical) {
			gradientFillRect(g, x, y, w / 2, h, sideColor, centerColor, false);
			gradientFillRect(g, x + w / 2, y, w / 2, h, centerColor, sideColor, false);
		} else {
			gradientFillRect(g, x, y, w, h / 2, sideColor, centerColor, true);
			gradientFillRect(g, x, y + h / 2, w, h / 2, centerColor, sideColor, true);
		}
		
		final ImageIcon icon = (ImageIcon) context.getStyle().getIcon(context, "EaSynth.scrollbar.thumb.image");
		if (icon != null) {
			final int imgWidth = icon.getIconWidth();
			final int imgHeight = icon.getIconHeight();
			if ((!isVertical && w >= imgWidth + borderThick * 2) 
					|| (isVertical && h >= imgHeight + borderThick * 2)) {
				final int destPosX = x + (w - imgWidth) / 2;
				final int destPosY = y + (h - imgHeight) / 2;
				g.drawImage(icon.getImage(), destPosX, destPosY, destPosX + imgWidth, destPosY + imgHeight,  0, 0, imgWidth, imgHeight, null);
			}
		}
	}

	/**
	 * Paint the thumb border for scroll bar.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 * @param orientation
	 * 		the orientation of the scroll bar
	 */
	public void paintScrollBarThumbBorder(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h, final int orientation) {
		final UIDefaults uiDefaults = UIManager.getDefaults();
		final int borderThick = uiDefaults.getInt("EaSynth.scrollbar.thumb.borderthick");
		Color borderColor = uiDefaults.getColor("EaSynth.scrollbar.thumb.bordercolor");
		if (borderColor == null) {
			borderColor = Color.DARK_GRAY;
		}
		if ((context.getComponentState() & SynthConstants.DISABLED) != 0) {
			borderColor = Color.LIGHT_GRAY;
		} else if ((context.getComponentState() & SynthConstants.MOUSE_OVER) != 0) {
			borderColor = borderColor.brighter();
		}
		g.setColor(borderColor);
		final JScrollBar scrollBar = (JScrollBar)context.getComponent();
		if (scrollBar.getOrientation() == JScrollBar.VERTICAL) {
			if (h > borderThick * 2) {
				g.fill3DRect(x, y, w, borderThick, true);
				g.fill3DRect(x, y + h - borderThick, w, borderThick, true);
			}
		} else {
			if (w > borderThick * 2) {
				g.fill3DRect(x, y, borderThick, h, true);
				g.fill3DRect(x + w - borderThick, y, borderThick, h, true);
			}
		}
	}

	/**
	 * Paint the background for scroll bar.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 */
	public void paintScrollBarBackground(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h) {
		final UIDefaults uiDefaults = UIManager.getDefaults();
		Color bgColor = uiDefaults.getColor("EaSynth.scrollbar.bgcolor");
		if (bgColor == null) {
			bgColor = Color.WHITE;
		}
		g.setColor(bgColor);
		g.fill3DRect(x, y, w, h, true);
	}

	/**
	 * Paint the border for scroll bar.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 */
	public void paintScrollBarBorder(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h) {
		final UIDefaults uiDefaults = UIManager.getDefaults();
		Color borderColor = uiDefaults.getColor("EaSynth.scrollbar.bordercolor");
		if (borderColor == null) {
			borderColor = Color.DARK_GRAY;
		}
		g.setColor(borderColor);
		g.draw3DRect(x, y, w - 1, h - 1, true);
	}

	/**
	 * Paint the divider background for split pane.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 */
	public void paintSplitPaneDividerBackground(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h) {
		final UIDefaults uiDefaults = UIManager.getDefaults();
		Color mainColor = uiDefaults.getColor("EaSynth.splitpane.divider.maincolor");
		if (mainColor == null) {
			mainColor = Color.LIGHT_GRAY;
		}
		g.setColor(mainColor);
		g.fill3DRect(x, y, w, h, true);
		
		final ImageIcon icon = (ImageIcon) context.getStyle().getIcon(context, "EaSynth.splitpane.divider.image");
		if (icon != null) {
			final int imgWidth = icon.getIconWidth();
			final int imgHeight = icon.getIconHeight();
			final JSplitPane splitPane = (JSplitPane)context.getComponent();
			if (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT && h >= imgHeight * 9) {
				final int destPosX = x + (w - imgWidth) / 2;
				final int destPosY = y + (h - imgHeight * 3) / 2;
				g.drawImage(icon.getImage(), destPosX, destPosY, destPosX + imgWidth, destPosY + imgHeight,  0, 0, imgWidth, imgHeight, null);
				g.drawImage(icon.getImage(), destPosX, destPosY + imgHeight, destPosX + imgWidth, destPosY + imgHeight * 2,  0, 0, imgWidth, imgHeight, null);
				g.drawImage(icon.getImage(), destPosX, destPosY + imgHeight * 2, destPosX + imgWidth, destPosY + imgHeight * 3,  0, 0, imgWidth, imgHeight, null);
			} else if (w >= imgWidth * 9) {
				final int destPosX = x + (w - imgWidth * 3) / 2;
				final int destPosY = y + (h - imgHeight) / 2;
				g.drawImage(icon.getImage(), destPosX, destPosY, destPosX + imgWidth, destPosY + imgHeight,  0, 0, imgWidth, imgHeight, null);
				g.drawImage(icon.getImage(), destPosX + imgWidth, destPosY, destPosX + imgWidth * 2, destPosY + imgHeight,  0, 0, imgWidth, imgHeight, null);
				g.drawImage(icon.getImage(), destPosX + imgWidth * 2, destPosY, destPosX + imgWidth * 3, destPosY + imgHeight,  0, 0, imgWidth, imgHeight, null);
			}
		}
	}

	/**
	 * Paint dragging divider for split pane.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 * @param orientation
	 * 		the orientation of the split pane
	 */
	public void paintSplitPaneDragDivider(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h, final int orientation) {
		paintSplitPaneDividerBackground(context, g, x, y, w, h);
	}

	/**
	 * Paint the header border for table.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 */
	public void paintTableHeaderBorder(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h) {
		final Graphics2D g2 = (Graphics2D)g.create();
		final Color lightColor = new Color(255, 255, 255, 90);
		final Color darkColor = new Color(60, 60, 60, 90);
		final JTableHeader header = (JTableHeader)context.getComponent();
		final TableColumnModel model = header.getColumnModel();
		final int colCount = model.getColumnCount();
		int curPos = 0;
		for (int i = 0; i < colCount; i ++) {
			g2.setPaint(lightColor);
			g2.drawLine(x + curPos, y, x + curPos, y + h - 1);
			curPos += model.getColumn(i).getWidth() - 1;
			g2.setPaint(darkColor);
			g2.drawLine(x + curPos, y, x + curPos, y + h - 1);
			curPos += model.getColumnMargin();
		}
		/*g.setColor(new Color(200, 100, 50, 130));
		g.fill3DRect(x, y, w, h, true);*/
	}
	
	/**
	 * Fix the popup size and border, opaque etc.
	 * 
	 * @param context
	 */
	private void fixComboBoxPopup(SynthContext context) {
		synchronized (MANAGED_OBJECT_MAP) {
			if (!MANAGED_OBJECT_MAP.containsKey(context.getComponent())) {
				if (context.getComponent() instanceof JComboBox) {
					final JComboBox comboBox = (JComboBox)context.getComponent();
					boolean alreadyFixed = false;
					for (PopupMenuListener l : comboBox.getPopupMenuListeners()) {
						if (l instanceof EaSynthComboBoxPopupMenuListener) {
							alreadyFixed = true;
							break;
						}
					}
					if (!alreadyFixed) {
						comboBox.addPopupMenuListener(new EaSynthComboBoxPopupMenuListener());
						MANAGED_OBJECT_MAP.put(comboBox, comboBox.getClass().getName());
					}
				}
			}
		}
	}
	
	/**
	 * Paint the background for combo box.
	 * (do not recommend from EaSynth Look And Feel Template V1.20)
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 */
	public void paintComboBoxBackground(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h) {
		fixComboBoxPopup(context);
		ImageIcon icon = null;
		if ((context.getComponentState() & SynthConstants.DISABLED) != 0) {
			icon = (ImageIcon) context.getStyle().getIcon(context, "EaSynth.combobox.bg.image.disabled");
		} else {
			icon = (ImageIcon) context.getStyle().getIcon(context, "EaSynth.combobox.bg.image.enabled");
		}
		if (icon != null) {
			final Image image = icon.getImage();
			final int imgWidth = image.getWidth(null);
			final int imgHeight = image.getHeight(null);
			g.drawImage(icon.getImage(), x, y, x + w, y + h, 0, 0, imgWidth, imgHeight, null);
		}
	}
	
	/**
	 * Paint the border for combo box.
	 */
	public void paintComboBoxBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
		fixComboBoxPopup(context);
		super.paintComboBoxBorder(context, g, x, y, w, h);
	}

	/**
	 * Paint the menu background.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 */
	public void paintMenuBackground(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h) {
		final UIDefaults uiDefaults = UIManager.getDefaults();
		Color normalColor = uiDefaults.getColor("EaSynth.menu.bg.color.normal");
		if (normalColor == null) {
			normalColor = Color.LIGHT_GRAY;
		}
		Color selectedColor = uiDefaults.getColor("EaSynth.menu.bg.color.selected");
		if (selectedColor == null) {
			selectedColor = Color.GRAY;
		}
		
		final JMenu menu = (JMenu)context.getComponent();
		
		if ((context.getComponentState() & SynthConstants.SELECTED) != 0) {
			g.setColor(selectedColor);
			g.fillRect(x, y, w, h);
		} else if ((context.getComponentState() & SynthConstants.DISABLED) != 0) {
			// if menu in JMenuBar, don't draw the background
			if (!(menu.getParent() instanceof JMenuBar)) {
				g.setColor(normalColor);
				g.fillRect(x, y, w, h);
			}
		} else {
			// if menu in JMenuBar, don't draw the background
			if (!(menu.getParent() instanceof JMenuBar)) {
				g.setColor(normalColor);
				g.fillRect(x, y, w, h);
			}
		}
	}

	/**
	 * Paint the thumb background for slider.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 * @param orientation
	 * 		the orientation of the slider
	 */
	public void paintSliderThumbBackground(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h, final int orientation) {
		ImageIcon thumbIcon = null;
		if ((context.getComponentState() & SynthConstants.DISABLED) != 0) {
			thumbIcon = (ImageIcon) context.getStyle().getIcon(context, "EaSynth.slider.thumb.image.disabled");
		} else if ((context.getComponentState() & SynthConstants.MOUSE_OVER) != 0){
			thumbIcon = (ImageIcon) context.getStyle().getIcon(context, "EaSynth.slider.thumb.image.mouseover");
		} else {
			thumbIcon = (ImageIcon) context.getStyle().getIcon(context, "EaSynth.slider.thumb.image.normal");
		}
		if (thumbIcon != null) {
			final int imgWidth = thumbIcon.getIconWidth();
			final int imgHeight = thumbIcon.getIconHeight();
			final int destPosX = x + (w - imgWidth) / 2;
			final int destPosY = y + (h - imgHeight) / 2;
			g.drawImage(thumbIcon.getImage(), destPosX, destPosY, destPosX + imgWidth, destPosY + imgHeight,  0, 0, imgWidth, imgHeight, null);
		}
	}

	/**
	 * Paint the background for toolbar.
	 * 
	 * @param context
	 * 		the SynthContext object for painting
	 * @param g
	 * 		the Graphics object to paint
	 * @param x
	 * 		the x position of the rectangle to paint
	 * @param y
	 * 		the y position of the rectangle to paint
	 * @param w
	 * 		the width of the rectangle to paint
	 * @param h
	 * 		the height of the rectangle to paint
	 */
	public void paintToolBarBackground(final SynthContext context, final Graphics g, 
			final int x, final int y, final int w, final int h) {
		final UIDefaults uiDefaults = UIManager.getDefaults();
		Color color1 = uiDefaults.getColor("EaSynth.toolbar.bg.color1");
		if (color1 == null) {
			color1 = Color.LIGHT_GRAY;
		}
		Color color2 = uiDefaults.getColor("EaSynth.toolbar.bg.color2");
		if (color2 == null) {
			color2 = Color.WHITE;
		}
		Color color3 = uiDefaults.getColor("EaSynth.toolbar.bg.color3");
		if (color3 == null) {
			color3 = Color.LIGHT_GRAY;
		}
		final JToolBar toolbar = (JToolBar)context.getComponent();
		if (toolbar.getOrientation() == JToolBar.VERTICAL) {
			gradientFillRect(g, x, y, w / 2, h, color1, color2, false);
			gradientFillRect(g, x + w / 2, y, w / 2, h, color2, color3, false);
		} else {
			gradientFillRect(g, x, y, w, h / 2, color1, color2, true);
			gradientFillRect(g, x, y + h / 2, w, h / 2, color2, color3, true);
		}
	}
	
	/**
	 * Paint the background of the popup menu, implement the shadow
	 */
	public void paintPopupMenuBackground(SynthContext context, Graphics g, int x, int y, int w, int h) {
		final JPopupMenu popupMenu = (JPopupMenu)context.getComponent();
		final JPanel panel = (JPanel)popupMenu.getParent();
		final BufferedImage bgImage = (BufferedImage)(panel.getClientProperty(EaSynthPopup.POPUP_BACKGROUND_IMAGE));
		if (bgImage != null) {
			g.drawImage(bgImage, x, y, null);
		}
		final ImageIcon bgIcon = (ImageIcon) context.getStyle().getIcon(context, "EaSynth.popup.menu.bg");
		if (bgIcon != null) {
			EaSynthGraphicsUtils.drawImageWith9Grids(g, bgIcon.getImage(), 
					x, y, x + w, y + h, 
					context.getStyle().getInsets(context, null), true);
		}
	}
}

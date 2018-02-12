/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.Icon;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.circuit.WireSet;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.Icons;

public class PokeTool extends Tool {
	private class Listener implements CircuitListener {
		@Override
		public void circuitChanged(CircuitEvent event) {
			Circuit circ = pokedCircuit;
			if (event.getCircuit() == circ && circ != null && (event.getAction() == CircuitEvent.ACTION_REMOVE
					|| event.getAction() == CircuitEvent.ACTION_CLEAR) && !circ.contains(pokedComponent)) {
				removeCaret(false);
			}
		}
	}

	private static class WireCaret extends AbstractCaret {
		Canvas canvas;
		Wire wire;
		int x;
		int y;

		WireCaret(Canvas c, Wire w, int x, int y, AttributeSet opts) {
			canvas = c;
			wire = w;
			this.x = x;
			this.y = y;
		}

		@Override
		public void draw(Graphics g) {
			Value v = canvas.getCircuitState().getValue(wire.getEnd0());
			RadixOption radix1 = RadixOption.decode(AppPreferences.POKE_WIRE_RADIX1.get());
			RadixOption radix2 = RadixOption.decode(AppPreferences.POKE_WIRE_RADIX2.get());
			if (radix1 == null)
				radix1 = RadixOption.RADIX_2;
			String vStr = radix1.toString(v);
			if (radix2 != null && v.getWidth() > 1) {
				vStr += " / " + radix2.toString(v);
			}

			FontMetrics fm = g.getFontMetrics();
			Rectangle r = canvas.getViewableRectangle();
			g.fillOval(x - 2, y - 2, 4, 4);
			g.setColor(Color.WHITE);
			if (r.x + r.width - x - fm.stringWidth(vStr) - 4 > x - r.x - fm.stringWidth(vStr) - 4) {
				// left oriented
				int[] xPoints = { x, x, x + 4 + fm.stringWidth(vStr), x + 4 + fm.stringWidth(vStr), x + 3 };
				int[] yPoints = { y, y + 8 + fm.getAscent() + fm.getDescent(), y + 8 + fm.getAscent() + fm.getDescent(),
						y + 6, y + 6 };
				g.fillPolygon(xPoints, yPoints, 5);
				g.setColor(Color.BLACK);
				g.drawPolygon(xPoints, yPoints, 5);
				g.drawString(vStr, x + 2, y + 7 + fm.getAscent());
			} else {
				// right oriented
				int[] xPoints = { x, x, x - 4 - fm.stringWidth(vStr), x - 4 - fm.stringWidth(vStr), x - 3 };
				int[] yPoints = { y, y + 8 + fm.getAscent() + fm.getDescent(), y + 8 + fm.getAscent() + fm.getDescent(),
						y + 6, y + 6 };
				g.fillPolygon(xPoints, yPoints, 5);
				g.setColor(Color.BLACK);
				g.drawPolygon(xPoints, yPoints, 5);
				g.drawString(vStr, x - 2 - fm.stringWidth(vStr), y + 7 + fm.getAscent());
			}
		}
	}

	private static final Icon toolIcon = Icons.getIcon("poke.gif");

	private static final Cursor cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
	private static final Cursor move = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);

	private Listener listener;
	private Circuit pokedCircuit;
	private Component pokedComponent;
	private Caret pokeCaret;
	private int x = 0, y = 0, x0 = 0, y0 = 0, ScrollBarX = 0, ScrollBarY = 0;

	public PokeTool() {
		this.listener = new Listener();
	}

	@Override
	public void deselect(Canvas canvas) {
		removeCaret(true);
		canvas.setHighlightedWires(WireSet.EMPTY);
	}

	@Override
	public void draw(Canvas canvas, ComponentDrawContext context) {
		if (pokeCaret != null)
			pokeCaret.draw(context.getGraphics());
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof PokeTool;
	}

	@Override
	public Cursor getCursor() {
		return cursor;
	}

	@Override
	public String getDescription() {
		return Strings.get("pokeToolDesc");
	}

	@Override
	public String getDisplayName() {
		return Strings.get("pokeTool");
	}

	@Override
	public String getName() {
		return "Poke Tool";
	}

	@Override
	public int hashCode() {
		return PokeTool.class.hashCode();
	}

	@Override
	public void keyPressed(Canvas canvas, KeyEvent e) {
		if (pokeCaret != null) {
			pokeCaret.keyPressed(e);
			canvas.getProject().repaintCanvas();
		}
	}

	@Override
	public void keyReleased(Canvas canvas, KeyEvent e) {
		if (pokeCaret != null) {
			pokeCaret.keyReleased(e);
			canvas.getProject().repaintCanvas();
		}
	}

	@Override
	public void keyTyped(Canvas canvas, KeyEvent e) {
		if (pokeCaret != null) {
			pokeCaret.keyTyped(e);
			canvas.getProject().repaintCanvas();
		}
	}

	@Override
	public void mouseDragged(Canvas canvas, Graphics g, MouseEvent e) {
		if (pokeCaret != null) {
			pokeCaret.mouseDragged(e);
			canvas.getProject().repaintCanvas();
		} else {
			// move scrollpane dragging hand
			Point m = canvas.getMousePosition();
			if (m == null) {
				// if mouse exited and continue dragging
				this.x0 = -1;
				this.y0 = -1;
				this.ScrollBarX = -1;
				this.ScrollBarY = -1;
				return;
			} else if (this.x0 == -1 || this.y0 == -1 || this.ScrollBarX == -1 || this.ScrollBarY == -1) {
				// if mouse re-entered after it exited without releasing the button
				this.x0 = (int) m.getX();
				this.y0 = (int) m.getY();
				this.ScrollBarX = canvas.getHorizzontalScrollBar();
				this.ScrollBarY = canvas.getVerticalScrollBar();
			}
			int x = (int) (this.x0 - m.getX());
			int y = (int) (this.y0 - m.getY());
			canvas.setCursor(move);
			canvas.setScrollBar(this.ScrollBarX + x, this.ScrollBarY + y);
			canvas.setArrows();
		}
	}

	@Override
	public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
		this.x0 = (int) canvas.getMousePosition().getX();
		this.y0 = (int) canvas.getMousePosition().getY();
		this.x = e.getX();
		this.y = e.getY();
		this.ScrollBarX = canvas.getHorizzontalScrollBar();
		this.ScrollBarY = canvas.getVerticalScrollBar();
		canvas.setCursor(cursor);
		Location loc = Location.create(x, y);
		boolean dirty = false;
		canvas.setHighlightedWires(WireSet.EMPTY);
		if (pokeCaret != null && !pokeCaret.getBounds(g).contains(loc)) {
			dirty = true;
			removeCaret(true);
		}
		if (pokeCaret == null) {
			ComponentUserEvent event = new ComponentUserEvent(canvas, x, y);
			Circuit circ = canvas.getCircuit();
			for (Component c : circ.getAllContaining(loc, g)) {
				if (pokeCaret != null)
					break;

				if (c instanceof Wire) {
					Caret caret = new WireCaret(canvas, (Wire) c, x, y,
							canvas.getProject().getOptions().getAttributeSet());
					setPokedComponent(circ, c, caret);
					canvas.setHighlightedWires(circ.getWireSet((Wire) c));
				} else {
					Pokable p = (Pokable) c.getFeature(Pokable.class);
					if (p != null) {
						Caret caret = p.getPokeCaret(event);
						setPokedComponent(circ, c, caret);
						AttributeSet attrs = c.getAttributeSet();
						if (attrs != null && attrs.getAttributes().size() > 0) {
							Project proj = canvas.getProject();
							proj.getFrame().viewComponentAttributes(circ, c);
						}
					}
				}
			}
		}
		if (pokeCaret != null) {
			dirty = true;
			pokeCaret.mousePressed(e);
		}
		if (dirty)
			canvas.getProject().repaintCanvas();
	}

	@Override
	public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
		if (pokeCaret != null) {
			pokeCaret.mouseReleased(e);
			canvas.getProject().repaintCanvas();
		}
	}

	@Override
	public void paintIcon(ComponentDrawContext c, int x, int y) {
		Graphics g = c.getGraphics();
		if (toolIcon != null) {
			toolIcon.paintIcon(c.getDestination(), g, x + 2, y + 2);
		} else {
			g.setColor(java.awt.Color.black);
			g.drawLine(x + 4, y + 2, x + 4, y + 17);
			g.drawLine(x + 4, y + 17, x + 1, y + 11);
			g.drawLine(x + 4, y + 17, x + 7, y + 11);

			g.drawLine(x + 15, y + 2, x + 15, y + 17);
			g.drawLine(x + 15, y + 2, x + 12, y + 8);
			g.drawLine(x + 15, y + 2, x + 18, y + 8);
		}
	}

	private void removeCaret(boolean normal) {
		Circuit circ = pokedCircuit;
		Caret caret = pokeCaret;
		if (caret != null) {
			if (normal)
				caret.stopEditing();
			else
				caret.cancelEditing();
			circ.removeCircuitListener(listener);
			pokedCircuit = null;
			pokedComponent = null;
			pokeCaret = null;
		}
	}

	private void setPokedComponent(Circuit circ, Component comp, Caret caret) {
		removeCaret(true);
		pokedCircuit = circ;
		pokedComponent = comp;
		pokeCaret = caret;
		if (caret != null) {
			circ.addCircuitListener(listener);
		}
	}
}

package levelEditor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** This class groups together four tiles and create a JPanel. The group consists
 * of four tiles, namely, topLeft, topRight, bottomLeft, and bottomRight.
 * 
 * @author Naoki Mizuno */

public class MapGrid extends JPanel implements MouseListener
{
	/* Corners and straight roads */
	public static final String	STRAIGHT_NS			= "STRAIGHT_NS";
	public static final String	STRAIGHT_EW			= "STRAIGHT_EW";
	public static final String	CORNER_NW			= "CORNER_NW";
	public static final String	CORNER_SW			= "CORNER_SW";
	public static final String	CORNER_NE			= "CORNER_NE";
	public static final String	CORNER_SE			= "CORNER_SE";

	/* Intersections */
	public static final String	FOUR_WAY			= "FOUR_WAY";
	/* The naming convention of three-way intersection is different from the
	 * main game program. The reason of this is because it is easier to process
	 * intersections by specifying them by where the intersection is open to.
	 * Thus, the String value accounts for this different so that when exporting
	 * to an XML file it uses the appropriate intersection name used in the
	 * game. */
	public static final String	THREE_WAY_NSE		= "THREE_WAY_NSW";
	public static final String	THREE_WAY_SEW		= "THREE_WAY_NEW";
	public static final String	THREE_WAY_NSW		= "THREE_WAY_NSE";
	public static final String	THREE_WAY_NEW		= "THREE_WAY_SEW";

	public static final String	NON_ROAD			= "NON_ROAD";

	/** Size of each grid in pixels */
	public static final int		DEFAULT_GRID_SIZE	= 60;
	/* Can be changed when there is not enough screen height */
	/** Size of each grid in pixels */
	public static int			GRID_SIZE			= 60;

	/* Directions */
	public static final int		NIL					= 0;
	public static final int		NORTH				= 1;
	public static final int		SOUTH				= 2;
	public static final int		EAST				= 3;
	public static final int		WEST				= 4;

	private BufferedImage		image;
	private StateTracker		tracker;

	private String				type;
	private int					x;
	private int					y;

	private boolean				isStartingPoint;
	private int					enteredFrom;
	private int					exitedFrom;

	private GridBagLayout		gbl;
	/** Generated serial version ID. */
	private static final long	serialVersionUID	= -1460428408091944849L;

	public MapGrid(StateTracker tracker, String type, int x, int y)
	{
		this.tracker = tracker;
		this.type = type;
		this.x = x;
		this.y = y;

		resetEnterExit();

		setBorder(BorderFactory.createLineBorder(Color.RED));
		setSize(new Dimension(GRID_SIZE + 2 * MapEditor.BORDER, GRID_SIZE + 2 * MapEditor.BORDER));
		setMaximumSize(getSize());
		setMinimumSize(getSize());
		setPreferredSize(getSize());

		setType(type);

		gbl = new GridBagLayout();
		setLayout(gbl);

		addMouseListener(this);
	}

	public void setType(String type)
	{
		this.type = type;
		repaint();
		revalidate();
	}

	/** Adds the image of this grid type to this JPanel. */
	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		try
		{
			String fileName = getFileName(type);
			this.image = ImageIO.read(new File("images/" + fileName));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		Graphics2D g2D = (Graphics2D) g;

		double imageWidth = image.getWidth();
		double imageHeight = image.getHeight();

		// How big is the image compared to the grid size?
		double sx = GRID_SIZE / imageWidth;
		double sy = GRID_SIZE / imageHeight;

		// Scale
		AffineTransform af = AffineTransform.getScaleInstance(sx, sy);
		g2D.drawImage(image, af, this);
	}

	private String getFileName(String type)
	{
		String fileName = "";
		switch (type)
		{
			case THREE_WAY_NSE:
				fileName = "THREE_WAY_NSE";
			break;
			case THREE_WAY_NSW:
				fileName = "THREE_WAY_NSW";
			break;
			case THREE_WAY_SEW:
				fileName = "THREE_WAY_SEW";
			break;
			case THREE_WAY_NEW:
				fileName = "THREE_WAY_NEW";
			break;
			default:
				fileName = type;
		}
		return fileName + ".png";
	}

	/** Updates the type of this grid, if necessary, according to the direction
	 * it entered and exited from. */
	public void updateGridType()
	{
		if (!tracker.isDragging())
			return;

		if (type.startsWith("THREE_WAY"))
			checkThreeWay();
		else if (type.startsWith("STRAIGHT"))
			checkStraight();
		else if (type.startsWith("CORNER"))
			checkCorner();
		else if (type.equals(NON_ROAD))
			checkNonRoad();
	}

	/** Checks when the current type of this grid is a three-way intersection. */
	private void checkThreeWay()
	{
		boolean becomesFourWay = false;
		switch (type)
		{
			case THREE_WAY_NSE:
				becomesFourWay = enteredFrom == WEST || exitedFrom == WEST;
			break;
			case THREE_WAY_SEW:
				becomesFourWay = enteredFrom == NORTH || exitedFrom == NORTH;
			break;
			case THREE_WAY_NSW:
				becomesFourWay = enteredFrom == EAST || exitedFrom == EAST;
			break;
			case THREE_WAY_NEW:
				becomesFourWay = enteredFrom == SOUTH || exitedFrom == SOUTH;
			break;
		}

		if (becomesFourWay)
			setType(FOUR_WAY);
	}

	private void checkStraight()
	{
		if (type.equals(STRAIGHT_NS))
		{
			// enteredFrom == NIL but exitedFrom != NIL
			if (isStartingPoint)
			{
				if (exitedFrom == EAST)
					type = THREE_WAY_NSE;
				else if (exitedFrom == WEST)
					type = THREE_WAY_NSW;
			}
			else if (enteredFrom == EAST)
			{
				if (exitedFrom == WEST)
					type = FOUR_WAY;
				else
					type = THREE_WAY_NSE;
			}
			else if (enteredFrom == WEST)
			{
				if (exitedFrom == EAST)
					type = FOUR_WAY;
				else
					type = THREE_WAY_NSW;
			}
			else
			{
				if (exitedFrom == EAST)
					type = THREE_WAY_NSE;
				else if (exitedFrom == WEST)
					type = THREE_WAY_NSW;
			}
		}

		else if (type.equals(STRAIGHT_EW))
		{
			// enteredFrom == NIL but exitedFrom != NIL
			if (isStartingPoint)
			{
				if (exitedFrom == NORTH)
					type = THREE_WAY_NEW;
				else if (exitedFrom == SOUTH)
					type = THREE_WAY_SEW;
			}
			else if (enteredFrom == NORTH)
			{
				if (exitedFrom == SOUTH)
					type = FOUR_WAY;
				else
					type = THREE_WAY_NEW;
			}
			else if (enteredFrom == SOUTH)
			{
				if (exitedFrom == NORTH)
					type = FOUR_WAY;
				else
					type = THREE_WAY_SEW;
			}
			else
			{
				if (exitedFrom == NORTH)
					type = THREE_WAY_NEW;
				else if (exitedFrom == SOUTH)
					type = THREE_WAY_SEW;
			}
		}

		setType(type);
	}

	private void checkCorner()
	{
		/* Whether the cursor "entered" or "exited" doesn't matter in this case.
		 * Thus, they are reordered by the actual values and considered as
		 * "points" to make the comparisons simpler. The actual values are: (NIL
		 * <) NORTH < SOUTH < EAST < WEST */
		int p1 = enteredFrom < exitedFrom ? enteredFrom : exitedFrom;
		int p2 = enteredFrom < exitedFrom ? exitedFrom : enteredFrom;

		if (type.equals(CORNER_NE))
		{
			if (p1 == SOUTH && p2 == WEST)
				type = FOUR_WAY;
			else if (p1 == SOUTH || p2 == SOUTH)
				type = THREE_WAY_NSE;
			else if (p1 == WEST || p2 == WEST)
				type = THREE_WAY_NEW;
		}
		else if (type.equals(CORNER_NW))
		{
			if (p1 == SOUTH && p2 == EAST)
				type = FOUR_WAY;
			else if (p1 == SOUTH || p2 == SOUTH)
				type = THREE_WAY_NSW;
			else if (p1 == EAST || p2 == EAST)
				type = THREE_WAY_NEW;
		}
		else if (type.equals(CORNER_SE))
		{
			if (p1 == NORTH && p2 == WEST)
				type = FOUR_WAY;
			else if (p1 == NORTH || p2 == NORTH)
				type = THREE_WAY_NSE;
			else if (p1 == WEST || p2 == WEST)
				type = THREE_WAY_SEW;
		}
		else if (type.equals(CORNER_SW))
		{
			if (p1 == NORTH && p2 == EAST)
				type = FOUR_WAY;
			else if (p1 == NORTH || p2 == NORTH)
				type = THREE_WAY_NSW;
			else if (p1 == EAST || p2 == EAST)
				type = THREE_WAY_SEW;
		}

		setType(type);
	}

	private void checkNonRoad()
	{
		if (enteredFrom == exitedFrom)
			return;

		// Make it look like the cursor entered from the opposite direction
		if (isStartingPoint)
			enteredFrom = getOppositeDirection(exitedFrom);

		if (enteredFrom == NORTH)
		{
			if (exitedFrom == SOUTH)
				type = STRAIGHT_NS;
			else if (exitedFrom == EAST)
				type = CORNER_NE;
			else if (exitedFrom == WEST)
				type = CORNER_NW;
		}
		else if (enteredFrom == SOUTH)
		{
			if (exitedFrom == NORTH)
				type = STRAIGHT_NS;
			else if (exitedFrom == EAST)
				type = CORNER_SE;
			else if (exitedFrom == WEST)
				type = CORNER_SW;
		}
		else if (enteredFrom == EAST)
		{
			if (exitedFrom == NORTH)
				type = CORNER_NE;
			else if (exitedFrom == SOUTH)
				type = CORNER_SE;
			else if (exitedFrom == WEST)
				type = STRAIGHT_EW;
		}
		else if (enteredFrom == WEST)
		{
			if (exitedFrom == NORTH)
				type = CORNER_NW;
			else if (exitedFrom == SOUTH)
				type = CORNER_SW;
			else if (exitedFrom == EAST)
				type = STRAIGHT_EW;
		}

		setType(type);
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		// Will return the same type passed in if no change
		setType(new GridTypeSelector(type).getSelectedType());
		updateGridType();
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		isStartingPoint = true;
		tracker.clicked();
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		isStartingPoint = false;
		tracker.released();

		updateGridType();
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
		if (!tracker.isDragging())
			return;

		// Clear any previous directions
		resetEnterExit();

		enteredFrom = tracker.getEnteredFrom();
		exitedFrom = NIL;
		tracker.setPreviouslyExitedFrom(exitedFrom);
		updateGridType();
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
		if (!tracker.isDragging())
			return;

		if (e.getX() <= MapEditor.BORDER)
			exitedFrom = WEST;
		else if (e.getX() >= GRID_SIZE + MapEditor.BORDER)
			exitedFrom = EAST;
		else if (e.getY() <= MapEditor.BORDER)
			exitedFrom = NORTH;
		else if (e.getY() >= GRID_SIZE + MapEditor.BORDER)
			exitedFrom = SOUTH;

		tracker.setPreviouslyExitedFrom(exitedFrom);

		updateGridType();

		resetEnterExit();
	}

	/** This is used to make it seem like the cursor entered from the opposite
	 * direction when the users starts from this grid.
	 * 
	 * @param direction
	 * @return The opposite direction of <code>direction</code>. */
	private int getOppositeDirection(int direction)
	{
		switch (direction)
		{
			case NORTH:
				return SOUTH;
			case SOUTH:
				return NORTH;
			case EAST:
				return WEST;
			case WEST:
				return EAST;
			default:
				return NIL;
		}
	}

	/** Resets the direction it entered and exited from. */
	private void resetEnterExit()
	{
		enteredFrom = NIL;
		exitedFrom = NIL;
		isStartingPoint = false;
	}

	public void addElement(Document doc, Element map)
	{
		String[] smallerGridTypes = getSmallerGridTypes();

		for (int i = 0; i < 4; i++)
		{
			Element grid = doc.createElement("grid");

			// Add <intersection> if this is the top-left grid of an
			// intersection
			if (i == 0 && (this.type.startsWith("THREE_WAY") || this.type.equals(FOUR_WAY)))
			{
				Element intersection = doc.createElement("intersection");
				intersection.appendChild(doc.createTextNode(this.type));

				grid.appendChild(intersection);
			}

			// x, y coordinates don't account for the borders, but the XML uses
			// x = 0 as the left border, y = 0 as the bottom border.
			int xCoord, yCoord;
			switch (i)
			{
			// Top-left
				case 0:
					xCoord = 2 * x + 1;
					yCoord = 2 * y + 2;
				break;
				// Top-right
				case 1:
					xCoord = 2 * x + 2;
					yCoord = 2 * y + 2;
				break;
				// Bottom-left
				case 2:
					xCoord = 2 * x + 1;
					yCoord = 2 * y + 1;
				break;
				// Bottom-right
				case 3:
					xCoord = 2 * x + 2;
					yCoord = 2 * y + 1;
				break;
				// Shouldn't happen
				default:
					xCoord = yCoord = -1;
			}
			grid.setAttribute("x", Integer.toString(xCoord));
			grid.setAttribute("y", Integer.toString(yCoord));

			Element type = doc.createElement("type");
			type.appendChild(doc.createTextNode(smallerGridTypes[i]));

			grid.appendChild(type);
			map.appendChild(grid);
		}
	}

	private String[] getSmallerGridTypes()
	{
		String tl, tr, bl, br;
		switch (type)
		{
			case STRAIGHT_NS:
				tl = "STRAIGHT_ROAD_S2S";
				tr = "STRAIGHT_ROAD_N2N";
				bl = "STRAIGHT_ROAD_S2S";
				br = "STRAIGHT_ROAD_N2N";
			break;
			case STRAIGHT_EW:
				tl = "STRAIGHT_ROAD_W2W";
				tr = "STRAIGHT_ROAD_W2W";
				bl = "STRAIGHT_ROAD_E2E";
				br = "STRAIGHT_ROAD_E2E";
			break;
			case CORNER_NW:
				tl = "CORNER_ROAD_S2W";
				tr = "STRAIGHT_ROAD_N2N";
				bl = "STRAIGHT_ROAD_E2E";
				br = "CORNER_ROAD_E2N";
			break;
			case CORNER_SW:
				tl = "STRAIGHT_ROAD_W2W";
				tr = "CORNER_ROAD_N2W";
				bl = "CORNER_ROAD_E2S";
				br = "STRAIGHT_ROAD_N2N";
			break;
			case CORNER_NE:
				tl = "STRAIGHT_ROAD_S2S";
				tr = "CORNER_ROAD_W2N";
				bl = "CORNER_ROAD_S2E";
				br = "STRAIGHT_ROAD_E2E";
			break;
			case CORNER_SE:
				tl = "CORNER_ROAD_W2S";
				tr = "STRAIGHT_ROAD_W2W";
				bl = "STRAIGHT_ROAD_S2S";
				br = "CORNER_ROAD_N2E";
			break;

			case FOUR_WAY:
				tl = "INTER_TURN_S2WS";
				tr = "INTER_TURN_W2NW";
				bl = "INTER_TURN_E2SE";
				br = "INTER_TURN_N2EN";
			break;
			case THREE_WAY_NSE:
				tl = "STRAIGHT_ROAD_S2S";
				tr = "INTER_TURN_W2NW";
				bl = "INTER_TURN_E2SE";
				br = "INTER_TURN_N2EN";
			break;
			case THREE_WAY_SEW:
				tl = "INTER_TURN_S2WS";
				tr = "STRAIGHT_ROAD_W2W";
				bl = "INTER_TURN_E2SE";
				br = "INTER_TURN_N2EN";
			break;
			case THREE_WAY_NSW:
				tl = "INTER_TURN_S2WS";
				tr = "INTER_TURN_W2NW";
				bl = "INTER_TURN_E2SE";
				br = "STRAIGHT_ROAD_N2N";
			break;
			case THREE_WAY_NEW:
				tl = "INTER_TURN_S2WS";
				tr = "INTER_TURN_W2NW";
				bl = "STRAIGHT_ROAD_E2E";
				br = "INTER_TURN_N2EN";
			break;
			default:
				tl = tr = bl = br = "NON_ROAD";
		}

		return new String[]
		{ tl, tr, bl, br };
	}

	public BufferedImage getBufferedImage()
	{
		return image;
	}

	/** Returns the size of the original image. Since the image is a square and
	 * the width and height are the same length, only the width is returned.
	 * 
	 * @return The size of the original image. */
	public int getImageSize()
	{
		return image.getWidth();
	}

	public String getType()
	{
		return type;
	}

	public void setTracker(StateTracker tracker)
	{
		this.tracker = tracker;
	}
}

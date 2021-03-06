import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Map editor for the level.
 * 
 * @author Naoki Mizuno */

public class MapEditor extends JDialog
{
	/** Generated serial version ID. */
	private static final long	serialVersionUID	= -115622713904200668L;

	/** Size of edge rows/columns in pixels */
	public static final int		EDGE_SIZE			= 15;
	/** Width of the border in pixels */
	public static final int		BORDER				= 1;

	private JPanel				mapPanel;

	private StateTracker		tracker;
	private File				xmlFile;
	/** True if program read from existing XML, false if created level from
	 * scratch */
	private boolean				readFromXML;
	private int					levelNumber;
	private int					sizeX;
	private int					sizeY;
	/** Defined so that gridGroups[x-coord][y-coord] and gridGroups[0][0] is the
	 * bottom-left corner of the map. */
	/* Need to be careful when actually generating the image because the
	 * y-coordinate must go from max to 0. */
	private MapGrid[][]			gridGroups;
	/** ArrayList of start and end coordinates. startsEnds[0] has the starting
	 * coordinates and startsEnds[1] has the ending coordinates. */
	private ArrayList<int[]>	starts;
	private ArrayList<int[]>	ends;
	private ArrayList<Car>		cars;

	private String				mapDirectory;

	/** Constructor for creating a new level.
	 * 
	 * @param levelNumber
	 * @param sizeX
	 *            Size of the map in the X direction. This does not account for
	 *            the borders, so the size shown in the XML is 2 larger than
	 *            this number.
	 * @param sizeY
	 *            Size of the map in the Y direction. This does not account for
	 *            the borders, so the size shown in the XML is 2 larger than
	 *            this number. */
	public MapEditor(int levelNumber, int sizeX, int sizeY)
	{
		this.readFromXML = false;
		this.levelNumber = levelNumber;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.tracker = new StateTracker();
		this.gridGroups = new MapGrid[sizeX / 2][sizeY / 2];
		starts = new ArrayList<int[]>();
		ends = new ArrayList<int[]>();
		cars = new ArrayList<Car>();

		setGridSize();

		mapDirectory = String.format("%s/Levels/Lv%d/map/", LevelEditor.getRootPath(), levelNumber);
		xmlFile = new File(String.format("%s/lv%02d.xml", mapDirectory, levelNumber));

		initMapGridGroup();
		mapPanel = mapBuilder();
		add(mapPanel);
		add(buttonsBuilder(), BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(null);
		setMinimumSize(getSize());
		setTitle("Edit Level " + levelNumber);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.DOCUMENT_MODAL);
		setVisible(true);
	}

	/** Constructor for updating an existing level file.
	 * 
	 * @param xmlFilePath
	 *            Path to the XML file. */
	public MapEditor(String xmlFilePath)
	{
		this.readFromXML = true;
		this.xmlFile = new File(xmlFilePath);
		this.tracker = new StateTracker();

		LevelXMLParser parser = new LevelXMLParser(xmlFile);
		this.levelNumber = parser.getLevelNumber();
		mapDirectory = String.format("%s/Levels/Lv%d/map/", LevelEditor.getRootPath(), levelNumber);

		this.sizeX = parser.getSizeX();
		this.sizeY = parser.getSizeY();

		// Set GRID_SIZE before creating MapGrid objects in
		// parser.getGridGroups()
		setGridSize();

		this.gridGroups = parser.getGridGroups();
		this.starts = parser.getStarts();
		this.ends = parser.getEnds();
		this.cars = parser.getCars();

		// Set tracker for all MapGridGroups since they are set to null after
		// reading the XML
		for (int x = 0; x < sizeX / 2; x++)
			for (int y = 0; y < sizeY / 2; y++)
				gridGroups[x][y].setTracker(tracker);
		mapPanel = mapBuilder();
		add(mapPanel);
		add(buttonsBuilder(), BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(null);
		setMinimumSize(getSize());
		setTitle("Edit Level " + levelNumber);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.DOCUMENT_MODAL);
		setVisible(true);
	}

	private void setGridSize()
	{
		// Set appropriate size of the grids in MapEditor
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int optimalGridWidth = 2 * (screenSize.width - 2 * MapEditor.EDGE_SIZE) / sizeX;
		// Account for the task bar, title bar, and buttons
		int optimalGridHeight = 2 * (screenSize.height - 2 * MapEditor.EDGE_SIZE - 200) / sizeY;
		int min = Math.min(optimalGridWidth, optimalGridHeight);
		if (min < MapGrid.DEFAULT_GRID_SIZE)
			MapGrid.GRID_SIZE = min;
		else
			MapGrid.GRID_SIZE = MapGrid.DEFAULT_GRID_SIZE;
	}

	/** Initializes all the MapGridGroups of this map to NON_ROAD. */
	private void initMapGridGroup()
	{
		for (int x = 0; x < sizeX / 2; x++)
			for (int y = 0; y < sizeY / 2; y++)
				gridGroups[x][y] = new MapGrid(tracker, MapGrid.NON_ROAD, x, y);
	}

	/** Builds the JPanel with the grids and the borders. MapGridGroup array must
	 * be initialized or populated before calling this method.
	 * 
	 * @return JPanel with the grids and the borders. */
	private JPanel mapBuilder()
	{
		JPanel mapPanel = new JPanel();
		Dimension size = new Dimension((sizeX / 2) * (MapGrid.GRID_SIZE) + 2 * EDGE_SIZE, (sizeY / 2) * MapGrid.GRID_SIZE + 2 * EDGE_SIZE);
		mapPanel.setPreferredSize(size);
		mapPanel.setMinimumSize(size);

		JPanel gridsPanel = gridsBuilder();
		BorderGrid northBorder = new BorderGrid(tracker, MapGrid.NORTH, sizeX);
		BorderGrid southBorder = new BorderGrid(tracker, MapGrid.SOUTH, sizeX);
		BorderGrid eastBorder = new BorderGrid(tracker, MapGrid.EAST, sizeY);
		BorderGrid westBorder = new BorderGrid(tracker, MapGrid.WEST, sizeY);

		GridBagLayout gbl = new GridBagLayout();
		mapPanel.setLayout(gbl);
		GridBagConstraints gbc = new GridBagConstraints();

		// Map
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbl.setConstraints(gridsPanel, gbc);

		// North border
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbl.setConstraints(northBorder, gbc);

		// South border
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.anchor = GridBagConstraints.SOUTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbl.setConstraints(southBorder, gbc);

		// West border
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbl.setConstraints(westBorder, gbc);

		// East border
		gbc.gridx = 2;
		gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbl.setConstraints(eastBorder, gbc);

		mapPanel.add(gridsPanel);
		// Add borders
		mapPanel.add(northBorder);
		mapPanel.add(southBorder);
		mapPanel.add(eastBorder);
		mapPanel.add(westBorder);

		return mapPanel;
	}

	/** Adds the MapGridGroups to a JPanel. MapGridGroups must be initialized or
	 * populated before calling this method.
	 * 
	 * @return JPanel with the MapGridGroups. */
	private JPanel gridsBuilder()
	{
		JPanel grids = new JPanel();
		grids.setMinimumSize(new Dimension(
		// Account for border
		(sizeX / 2) * MapGrid.GRID_SIZE, (sizeY / 2) * MapGrid.GRID_SIZE));

		GridBagLayout gbl = new GridBagLayout();
		grids.setLayout(gbl);
		GridBagConstraints gbc = new GridBagConstraints();

		// Note that [0][0] is the "bottom-left"
		for (int x = 0; x < sizeX / 2; x++)
		{
			for (int y = 0; y < sizeY / 2; y++)
			{
				gbc.gridx = x;
				gbc.gridy = sizeY / 2 - 1 - y;

				MapGrid g = gridGroups[x][y];
				gbl.setConstraints(g, gbc);
				grids.add(g);
			}
		}

		return grids;
	}

	private JPanel buttonsBuilder()
	{
		JPanel buttons = new JPanel();

		buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));

		final JButton clearMap = new JButton("Clear Map");
		final JButton editCars = new JButton("Edit Cars");
		final JButton export = new JButton("Export");
		final JButton cancel = new JButton("Cancel");

		ActionListener clickListener = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (e.getSource() == clearMap)
					clearMap();
				else if (e.getSource() == export)
					exportLevel();
				else if (e.getSource() == cancel)
					dispose();
				else if (e.getSource() == editCars)
				{
					/* Check whether the starting and ending points have been
					 * modified. Check is done by comparing the starting
					 * coordinates before and after the calling of
					 * populateStartsEnds() method. If there is no change, the
					 * coordinates are the same after re-populating the
					 * ArrayList. Only the starting coordinates are checked
					 * because starting and ending coordinates act as a set and
					 * if one has been changed, it is guaranteed that the other
					 * has also changed. */
					boolean startsHaveChanged = false;
					int[][] prevStarts = new int[starts.size()][];
					prevStarts = starts.toArray(prevStarts);

					// Update starting and ending points
					if (!populateStartsEnds())
					{
						JOptionPane.showMessageDialog(null, "Can't have intersections on the edge!", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}

					// Compare with what is was before populateStartsEnds()
					int[][] currentStarts = new int[starts.size()][];
					currentStarts = starts.toArray(currentStarts);
					if (prevStarts.length != currentStarts.length)
						startsHaveChanged = true;
					else
					{
						for (int i = 0; i < starts.size(); i++)
							if (prevStarts[i][0] != currentStarts[i][0] || prevStarts[i][1] != currentStarts[i][1])
								startsHaveChanged = true;
					}

					int[][] startCoords = new int[0][];
					int[][] endCoords = new int[0][];
					startCoords = starts.toArray(startCoords);
					endCoords = ends.toArray(endCoords);

					// New empty list of cars
					if (!readFromXML && startsHaveChanged)
						cars = new ArrayList<Car>();

					new CarEditor(cars, startCoords, endCoords);
				}
			}
		};

		clearMap.addActionListener(clickListener);
		editCars.addActionListener(clickListener);
		export.addActionListener(clickListener);
		cancel.addActionListener(clickListener);
		buttons.add(clearMap);
		buttons.add(editCars);
		buttons.add(export);
		buttons.add(cancel);

		return buttons;
	}

	/** Initializes the map to all NON_ROADs. */
	private void clearMap()
	{
		remove(mapPanel);
		initMapGridGroup();
		mapPanel = mapBuilder();
		add(mapPanel);
		repaint();
		revalidate();
	}

	/** Populates the startsEnds array with the starting and ending coordinates. */
	private boolean populateStartsEnds()
	{
		starts.clear();
		ends.clear();
		/* The order of calling these methods is important. The naming
		 * convention of the intersections (in the game) is that intersection 0
		 * is the first intersection you hit when starting from the top-left
		 * corner of the map and going clockwise. */
		return checkNorthStartsEnds() && checkEastStartsEnds() && checkSouthStartsEnds() && checkWestStartsEnds();
	}

	private boolean checkNorthStartsEnds()
	{
		// Look at north border
		for (int i = 0; i < sizeX / 2; i++)
		{
			String type = gridGroups[i][sizeY / 2 - 1].getType();
			// Naming convention difference. See MapGrid
			Pattern p = Pattern.compile("THREE_WAY_(?![^S]{3})");
			Matcher m = p.matcher(type);
			if (m.find() || type.startsWith("FOUR_WAY"))
				return false;

			if (type.startsWith("STRAIGHT_NS") || type.startsWith("CORNER_N"))
			{
				// gridGroups doesn't account for the borders, but the XML uses
				// x = 0 as the left border, y = 0 as the bottom border.
				int x = 2 * i + 1;
				int y = sizeY + 1;
				starts.add(new int[]
				{ x, y });
				ends.add(new int[]
				{ x + 1, y });
			}
		}
		return true;
	}

	private boolean checkSouthStartsEnds()
	{
		// Look at south border
		for (int i = sizeX / 2 - 1; i >= 0; i--)
		{
			String type = gridGroups[i][0].getType();
			// Naming convention difference. See MapGridGroup
			Pattern p = Pattern.compile("THREE_WAY_(?![^N]{3})");
			Matcher m = p.matcher(type);
			if (m.find() || type.startsWith("FOUR_WAY"))
				return false;

			if (type.startsWith("STRAIGHT_NS") || type.startsWith("CORNER_S"))
			{
				// gridGroups doesn't account for the borders, but the XML uses
				// x = 0 as the left border, y = 0 as the bottom border.
				int x = 2 * i + 1;
				int y = 0;
				starts.add(new int[]
				{ x + 1, y });
				ends.add(new int[]
				{ x, y });
			}
		}
		return true;
	}

	private boolean checkEastStartsEnds()
	{
		// Look at east border
		for (int i = sizeY / 2 - 1; i >= 0; i--)
		{
			String type = gridGroups[sizeX / 2 - 1][i].getType();
			// Naming convention difference. See MapGridGroup
			Pattern p = Pattern.compile("THREE_WAY_(?![^W]{3})");
			Matcher m = p.matcher(type);
			if (m.find() || type.startsWith("FOUR_WAY"))
				return false;

			if (type.startsWith("STRAIGHT_EW") || type.matches("CORNER_.E"))
			{
				// gridGroups doesn't account for the borders, but the XML uses
				// x = 0 as the left border, y = 0 as the bottom border.
				int x = sizeX + 1;
				int y = 2 * i + 1;
				starts.add(new int[]
				{ x, y + 1 });
				ends.add(new int[]
				{ x, y });
			}
		}
		return true;
	}

	private boolean checkWestStartsEnds()
	{
		// Look at west border
		for (int i = 0; i < sizeY / 2; i++)
		{
			String type = gridGroups[0][i].getType();
			// Naming convention difference. See MapGridGroup
			Pattern p = Pattern.compile("THREE_WAY_(?![^E]{3})");
			Matcher m = p.matcher(type);
			if (m.find() || type.startsWith("FOUR_WAY"))
				return false;

			if (type.startsWith("STRAIGHT_EW") || type.matches("CORNER_.W"))
			{
				// gridGroups doesn't account for the borders, but the XML uses
				// x = 0 as the left border, y = 0 as the bottom border.
				int x = 0;
				int y = 2 * i + 1;
				starts.add(new int[]
				{ x, y });
				ends.add(new int[]
				{ x, y + 1 });
			}
		}
		return true;
	}

	/** Generates the XML file and the PNG image file for the level. */
	public void exportLevel()
	{
		// Export XML
		try
		{
			exportXML(xmlFile);
		}
		catch (FileNotFoundException | TransformerException e)
		{
			JOptionPane.showMessageDialog(null, "Error when exporting XML file", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Export image
		try
		{
			File imageFile = new File(String.format("%s/lv%02d.png", mapDirectory, levelNumber));
			exportMapImage(imageFile);
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(null, "Error when exporting map image", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		JOptionPane.showMessageDialog(null, "Finished exporting", "Done", JOptionPane.INFORMATION_MESSAGE);
	}

	/** Exports the current state of the map to the given XML file.
	 * 
	 * @param xmlFile */
	public void exportXML(File xmlFile)
			throws TransformerException,
			FileNotFoundException
	{
		// Create directory (if it doesn't exist)
		new File(mapDirectory).mkdirs();

		Document doc = null;
		try
		{
			DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
			doc = f.newDocumentBuilder().newDocument();
		}
		catch (ParserConfigurationException e)
		{
			e.printStackTrace();
		}
		Element level = doc.createElement("level");
		level.setAttribute("lv", Integer.toString(levelNumber));

		Element map = doc.createElement("map");
		// Add the invisible border grids
		map.setAttribute("size_x", Integer.toString(sizeX + 2));
		map.setAttribute("size_y", Integer.toString(sizeY + 2));

		// Add start/end points
		boolean noIntersectionOnEdge = addStartEnd(doc, map);
		if (!noIntersectionOnEdge)
		{
			JOptionPane.showMessageDialog(null, "Can't have intersections on the edge! Level is not saved.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Add normal grids
		for (int x = 0; x < sizeX / 2; x++)
			for (int y = 0; y < sizeY / 2; y++)
				gridGroups[x][y].addElement(doc, map);

		// Add car info
		Element cars = doc.createElement("cars");
		addCarsElement(doc, cars);

		// Append all child nodes
		level.appendChild(map);
		level.appendChild(cars);
		doc.appendChild(level);

		// Write out
		Transformer transformer = null;
		try
		{
			transformer = TransformerFactory.newInstance().newTransformer();
			DOMSource source = new DOMSource(doc);
			PrintWriter pw = new PrintWriter(xmlFile);
			StreamResult stream = new StreamResult(pw);
			transformer.transform(source, stream);
		}
		catch (TransformerConfigurationException e)
		{
			e.printStackTrace();
		}
		catch (TransformerFactoryConfigurationError e)
		{
			e.printStackTrace();
		}
	}

	/** Adds the START and END grid types to the XML by looking at the grid types
	 * of the outer grids. When there is an intersection on the edge, it does
	 * not add the starts and ends.
	 * 
	 * @param doc
	 *            The Document object to create the elements from.
	 * @param map
	 *            The Element object to add the elements to.
	 * @return True if there is no intersection on the edge, false if it does. */
	private boolean addStartEnd(Document doc, Element map)
	{
		if (!populateStartsEnds())
			return false;

		if (starts.size() != ends.size())
			return false;

		for (int i = 0; i < starts.size(); i++)
		{
			int x, y;
			// Add starting point
			x = starts.get(i)[0];
			y = starts.get(i)[1];
			Element start = doc.createElement("grid");
			start.setAttribute("x", Integer.toString(x));
			start.setAttribute("y", Integer.toString(y));
			Element ts = doc.createElement("type");
			ts.appendChild(doc.createTextNode(getStartEndType(x, y, true)));
			start.appendChild(ts);

			// Add ending point
			x = ends.get(i)[0];
			y = ends.get(i)[1];
			Element end = doc.createElement("grid");
			end.setAttribute("x", Integer.toString(x));
			end.setAttribute("y", Integer.toString(y));
			Element te = doc.createElement("type");
			te.appendChild(doc.createTextNode(getStartEndType(x, y, false)));
			end.appendChild(te);

			map.appendChild(start);
			map.appendChild(end);
		}

		return true;
	}

	private String getStartEndType(int x, int y, boolean isStart)
	{
		int maxX = sizeX + 1;
		int maxY = sizeY + 1;

		if (x == 0)
		{
			if (isStart)
				return "START_WEDGE2E";
			else
				return "END_W2WEDGE";
		}
		else if (y == 0)
		{
			if (isStart)
				return "START_SEDGE2N";
			else
				return "END_S2SEDGE";
		}
		else if (x == maxX)
		{
			if (isStart)
				return "START_EEDGE2W";
			else
				return "END_E2EEDGE";
		}
		else if (y == maxY)
		{
			if (isStart)
				return "START_NEDGE2S";
			else
				return "END_N2NEDGE";
		}

		// Shouldn't come here
		return null;
	}

	/** Adds a "car" elements to the given Element.
	 * 
	 * @param doc
	 *            Document object of the XML.
	 * @param cars
	 *            The "cars" node of the document. */
	private void addCarsElement(Document doc, Element cars)
	{
		for (int i = 0; i < this.cars.size(); i++)
		{
			Car c = this.cars.get(i);

			int[] startCoord = c.getStart();
			int[] endCoord = c.getEnd();
			int delay = c.getDelay();

			Element carNode = doc.createElement("car");
			// Start
			Element startNode = doc.createElement("start");
			startNode.setAttribute("x", Integer.toString(startCoord[0]));
			startNode.setAttribute("y", Integer.toString(startCoord[1]));
			// End
			Element endNode = doc.createElement("end");
			endNode.setAttribute("x", Integer.toString(endCoord[0]));
			endNode.setAttribute("y", Integer.toString(endCoord[1]));
			// Delay
			Element delayNode = doc.createElement("delay");
			delayNode.appendChild(doc.createTextNode(Integer.toString(delay)));

			carNode.appendChild(startNode);
			carNode.appendChild(endNode);
			carNode.appendChild(delayNode);

			cars.appendChild(carNode);
		}
	}

	/** Exports the PNG image of the map.
	 * 
	 * @param imageFile
	 *            File object to export the image to.
	 * @throws IOException */
	public void exportMapImage(File imageFile) throws IOException
	{
		int imageSize = gridGroups[0][0].getImageSize();
		BufferedImage mapImage = new BufferedImage(sizeX / 2 * imageSize, sizeY / 2 * imageSize, BufferedImage.TYPE_INT_RGB);

		Graphics2D g = mapImage.createGraphics();
		for (int x = 0; x < sizeX / 2; x++)
		{
			for (int y = 0; y < sizeY / 2; y++)
			{
				int xCoord = x * imageSize;
				int yCoord = y * imageSize;

				// Have to draw from top-left
				g.drawImage(gridGroups[x][sizeY / 2 - 1 - y].getBufferedImage(), xCoord, yCoord, this);
			}
		}

		ImageIO.write(mapImage, "PNG", imageFile);
	}
}

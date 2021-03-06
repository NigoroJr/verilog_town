import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** This class is each Car entry in the CarEditor. It contains information such
 * as the list of start/ends that the user can select from, car ID, and the time
 * which the car should appear in the level.
 * 
 * @author Naoki Mizuno */

class Car extends JPanel implements ActionListener
{
	/** Generated serial version ID */
	private static final long	serialVersionUID	= -1830415335894513637L;

	private int					id;
	private int[][]				allStarts;
	private int[][]				allEnds;

	private CarEditor			carEditor;

	private JLabel				carID;
	private JComboBox<String>	startPoints;
	private JComboBox<String>	endPoints;
	private JTextField			delayTextField;
	private JButton				removeButton;

	/** Creates a new Car object with the given ID and the list of starting and
	 * ending coordinates.
	 * 
	 * @param carEditor
	 *            The CarEditor object that this car belongs to.
	 * @param id
	 *            ID of this car.
	 * @param allStarts
	 *            List of all the starting coordinates.
	 * @param allEnds
	 *            List of all the ending coordinates. */
	public Car(CarEditor carEditor, int id, int[][] allStarts, int[][] allEnds)
	{
		super();

		this.carEditor = carEditor;
		this.id = id;
		this.allStarts = allStarts;
		this.allEnds = allEnds;

		panelBuilder();
	}

	/** Creates a new Car object that contains the car ID, starting and ending
	 * coordinates, and the delay.
	 * 
	 * @param id
	 * @param carEditor
	 *            The CarEditor object that this car belongs to.
	 * @param id
	 *            ID of this car.
	 * @param allStarts
	 *            List of all the starting coordinates.
	 * @param allEnds
	 *            List of all the ending coordinates.
	 * @param start
	 *            An array that contains exactly two elements. First is the X
	 *            coordinate and the second is Y coordinate of the starting
	 *            point.
	 * @param end
	 *            An array that contains exactly two elements. First is the X
	 *            coordinate and the second is Y coordinate of the ending point.
	 * @param delay
	 *            Delay for the car to start after the game has started. */
	public Car(
			CarEditor carEditor,
			int id,
			int[][] allStarts,
			int[][] allEnds,
			int[] start,
			int[] end,
			int delay)
	{
		this(carEditor, id, allStarts, allEnds);

		// Set selected values
		for (int i = 0; i < allStarts.length; i++)
			if (allStarts[i][0] == start[0] && allStarts[i][1] == start[1])
				startPoints.setSelectedIndex(i);
		for (int i = 0; i < allEnds.length; i++)
			if (allEnds[i][0] == end[0] && allEnds[i][1] == end[1])
				endPoints.setSelectedIndex(i);
		delayTextField.setText(Integer.toString(delay));
	}

	/** Adds the car ID, pull-down menu of the starting and ending points, and
	 * the text field for the delay to this JPanel. */
	private void panelBuilder()
	{
		GridBagLayout gbl = new GridBagLayout();
		this.setLayout(gbl);

		GridBagConstraints gbc = new GridBagConstraints();

		carID = new JLabel(Integer.toString(id));
		startPoints = new JComboBox<String>(getAllCoordinates(allStarts));
		endPoints = new JComboBox<String>(getAllCoordinates(allEnds));
		delayTextField = new JTextField();
		delayTextField.setPreferredSize(new Dimension(50, 30));
		removeButton = new JButton("-");
		removeButton.setPreferredSize(new Dimension(20, 20));
		removeButton.addActionListener(this);

		// TAB to move to next delay JTextField
		startPoints.setFocusable(false);
		endPoints.setFocusable(false);
		removeButton.setFocusable(false);

		// Car ID
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbl.setConstraints(carID, gbc);

		// Starting point
		gbc.gridx++;
		gbl.setConstraints(startPoints, gbc);

		// Ending point
		gbc.gridx++;
		gbl.setConstraints(endPoints, gbc);

		// Delay
		gbc.gridx++;
		gbl.setConstraints(delayTextField, gbc);

		// Button
		gbc.gridx++;
		gbl.setConstraints(removeButton, gbc);

		this.add(carID);
		this.add(startPoints);
		this.add(endPoints);
		this.add(delayTextField);
		this.add(removeButton);
	}

	/** Converts the array of coordinates into String representation. The
	 * start/end ID is the number assigned to the intersection, starting from 0,
	 * when you start from the top-left corner of the map and go clockwise.
	 * 
	 * @param coordinates
	 *            Array of coordinates. Has to be size [n][2] for any n.
	 * @return Array of coordinates represented in String. */
	private String[] getAllCoordinates(int[][] coordinates)
	{
		String[] ret = new String[coordinates.length];
		for (int i = 0; i < coordinates.length; i++)
			ret[i] = String.format("%d (%s, %s)", i, coordinates[i][0], coordinates[i][1]);

		return ret;
	}

	/** Updates the ID for this car and also updates the text in the JLabel.
	 * 
	 * @param id
	 *            The new ID for this car. */
	public void setID(int id)
	{
		this.id = id;
		this.carID.setText(Integer.toString(id));
	}

	/** Sets the CarEditor object that this car belongs to.
	 * 
	 * @param carEditor */
	public void setCarEditor(CarEditor carEditor)
	{
		this.carEditor = carEditor;
	}

	public int getID()
	{
		return id;
	}

	/** Returns the coordinate of the starting point of this car.
	 * 
	 * @return Coordinates of the starting point. First element is the X
	 *         coordinate, second element is the Y coordinate. */
	public int[] getStart()
	{
		return allStarts[startPoints.getSelectedIndex()];
	}

	/** Returns the coordinate of the ending point of this car.
	 * 
	 * @return Coordinates of the ending point. First element is the X
	 *         coordinate, second element is the Y coordinate. */
	public int[] getEnd()
	{
		return allEnds[endPoints.getSelectedIndex()];
	}

	/** Returns the delay for this car to depart after the game has started. The
	 * unit is not seconds. The approximate seconds can calculated by dividing
	 * the delay by 50. However, only use this to approximate the time.
	 * 
	 * @return The delay of this car.
	 * @throws EmptyTextFieldException
	 *             When the JTextField is empty. */
	public int getDelay() throws EmptyTextFieldException
	{
		if (delayTextField.getText().isEmpty())
			throw new EmptyTextFieldException();

		return Integer.parseInt(delayTextField.getText().trim());
	}

	@Override
	public Car clone()
	{
		return new Car(carEditor, id, allStarts, allEnds, allStarts[startPoints.getSelectedIndex()], allEnds[endPoints.getSelectedIndex()], Integer.parseInt(delayTextField.getText()));
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == removeButton)
		{
			carEditor.removeCarID(id);
		}
	}
}
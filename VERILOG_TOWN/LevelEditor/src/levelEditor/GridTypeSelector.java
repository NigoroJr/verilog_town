package levelEditor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

/** Shows a pull-down menu for selecting the type of the grid.
 * 
 * @author Naoki */
public class GridTypeSelector extends JDialog implements ActionListener
{

	/** Generated serial version ID. */
	private static final long		serialVersionUID	= 5109971722250367649L;

	public static final String[]	types				=
														{ MapGridGroup.STRAIGHT_NS, MapGridGroup.STRAIGHT_EW, MapGridGroup.CORNER_NW, MapGridGroup.CORNER_SW, MapGridGroup.CORNER_NE, MapGridGroup.CORNER_SE, MapGridGroup.FOUR_WAY, MapGridGroup.THREE_WAY_NSE, MapGridGroup.THREE_WAY_SEW, MapGridGroup.THREE_WAY_NSW, MapGridGroup.THREE_WAY_NEW, MapGridGroup.NON_ROAD, };

	private String					selectedType;

	private JComboBox<String>		comboBox;
	private JButton					ok;
	private JButton					cancel;
	private JPanel					selectionPanel;
	private JPanel					previewPane;
	private JPanel					buttonsPanel;

	public GridTypeSelector(String type)
	{
		/* Will be populated with the selected type from the JComboBox when
		 * hitting OK */
		selectedType = type;
		comboBox = new JComboBox<String>(types);
		comboBox.setSelectedItem(type);
		ok = new JButton("OK");
		cancel = new JButton("Cancel");
		selectionPanel = new JPanel();
		previewPane = new MapGridGroup(new StateTracker(), type, 0, 0);
		buttonsPanel = new JPanel();

		selectionPanelBuilder();
		buttonsPanelBuilder();
		add(selectionPanel);
		add(buttonsPanel, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle("Select Manually");
		this.setModalityType(ModalityType.APPLICATION_MODAL);
		setVisible(true);
	}

	private void selectionPanelBuilder()
	{
		selectionPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

		comboBox.addActionListener(this);

		selectionPanel.add(comboBox);
		selectionPanel.add(previewPane);
	}

	private void buttonsPanelBuilder()
	{
		buttonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

		ok.addActionListener(this);
		cancel.addActionListener(this);

		buttonsPanel.add(ok);
		buttonsPanel.add(cancel);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == comboBox)
		{
			selectionPanel.remove(previewPane);
			previewPane = new MapGridGroup(new StateTracker(), (String) comboBox.getSelectedItem(), 0, 0);
			selectionPanel.add(previewPane);
			revalidate();
			return;
		}
		else if (e.getSource() == ok)
			selectedType = (String) comboBox.getSelectedItem();
		dispose();
	}

	public String getSelectedType()
	{
		return selectedType;
	}
}
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.PlainDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import VerilogSimulator.Parse;
import java.util.ArrayList;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.*;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;
import java.io.*;
import java.nio.*;

public class VerilogEditor extends JFrame implements ActionListener
{
	static final int WIDTH = 600;
	static final int HEIGHT = 600;
	MyTextPane codeText = null;
	MyTextPane errorText = null;
	MyUndo1 myUndoManager1 = null;
	static String name;
	static String path;
	public File verilogFiles;
	JFormattedTextField simulateInput;
	Parse Compiler;
	/**
	* @param args
	*/
	
	public static void main(String[] args)
	{
		// TODO Auto-generated method stub
		name = args[0];
		path = args[1];
		new VerilogEditor();
	}
	
	public VerilogEditor()
	{
		super("Verilog Text Editor: " + name);
		Locale.setDefault(Locale.ENGLISH);
		
		File verilogDir = new File(path + "VerilogFiles");
		if  (!verilogDir.exists()  && !verilogDir.isDirectory())
		
		{
			System.out.println("Directory does not exist.");

			if(verilogDir.mkdir())
				System.out.println("Directory has been created.");
			else
				System.out.println("Fail to create a directory.");
			
		} 
		else
		{
			System.out.println("Directory is already exist.");
		}
		
		this.setSize(WIDTH,HEIGHT);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		//set the location the window will appear on the screen
		Toolkit kit = Toolkit.getDefaultToolkit();
		Dimension screenSize = kit.getScreenSize();
		int width = screenSize.width;
		int height = screenSize.height;
		int x = (width - WIDTH)/2;
		int y = (height - HEIGHT)/2;
		this.setLocation(x, y);
		
		JPanel contentPane = new JPanel();
		contentPane.setLayout(new GridBagLayout());
		this.setContentPane(contentPane);
		
		// below is the split panel code
		final JSplitPane splitPane = new JSplitPane();
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		
		splitPane.setDividerSize(2);
		splitPane.setPreferredSize(new Dimension(600,550));
		splitPane.setContinuousLayout(true);
		splitPane.setOneTouchExpandable(true);
		//make the divider keep its position(in percentage to the whole window) during dragging
		splitPane.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				splitPane.setDividerLocation(0.7);
			}
		});
		
		//grid bag constraints for splitpane
		GridBagConstraints cSplitPane = new GridBagConstraints();
		cSplitPane.gridx = 0;
		cSplitPane.gridy = 1;
		cSplitPane.fill = GridBagConstraints.BOTH;
		cSplitPane.weightx = 1;
		cSplitPane.weighty = 1;
		contentPane.add(splitPane,cSplitPane);
		
		codeText = new MyTextPane();
		setTabs(codeText,8);
		Font font1 = new Font("Consolas",Font.PLAIN,16);
		codeText.setFont(font1);
		
		//for keywords highlight
		codeText.getDocument().addDocumentListener(new SyntaxHighlighter(codeText));
		
		//for undo and redo
		myUndoManager1 = new MyUndo1();
		codeText.getDocument().addUndoableEditListener(myUndoManager1);
		
		//read in the already existed file or create a new file
		verilogFiles = new File(path + "VerilogFiles/" + name + ".txt");
		if(!verilogFiles.exists())
		{
			try
			{
				verilogFiles.createNewFile();
			} catch (IOException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		else
		{
			try
			{
				InputStreamReader reader = new InputStreamReader(new FileInputStream(verilogFiles));
				BufferedReader br = new BufferedReader(reader);
				String line = "";
				String temp = null;
				while ((temp = br.readLine()) != null)
				line =line + temp + "\n";
				Document docCode = codeText.getDocument();
				docCode.insertString(0, line, null);
				
				br.close();
				reader.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			} catch (BadLocationException e)
			{
				e.printStackTrace();
			}
		}
		
		errorText = new MyTextPane();
		errorText.setEditable(false);
		Font font2 = new Font("Consolas",Font.PLAIN,12);
		errorText.setFont(font2);
		
		
		JScrollPane upperArea = new JScrollPane(codeText,
		ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
		ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		JPanel lowerArea = new JPanel();
		lowerArea.setLayout(new GridBagLayout());
		JScrollPane errorArea = new JScrollPane(errorText,
		ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
		ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		JLabel errorLog = new JLabel("Error log");
		
		this.setVisible(true);
		
		//grid bag constraints for errorlog and errorarea
		GridBagConstraints cErrorLog = new GridBagConstraints();
		cErrorLog.gridx = 0;
		cErrorLog.gridy = 0;
		cErrorLog.fill = GridBagConstraints.BOTH;
		cErrorLog.weightx = 0;
		cErrorLog.weighty = 0;
		lowerArea.add(errorLog,cErrorLog);
		GridBagConstraints cErrorArea = new GridBagConstraints();
		cErrorArea.gridx = 0;
		cErrorArea.gridy = 1;
		cErrorArea.fill = GridBagConstraints.BOTH;
		cErrorArea.weightx = 1;
		cErrorArea.weighty = 1;
		lowerArea.add(errorArea,cErrorArea);
		splitPane.add(upperArea,JSplitPane.LEFT,1);
		splitPane.add(lowerArea,JSplitPane.RIGHT,2);
		
		//below is the tool bar code
		JToolBar toolBar = new JToolBar("Still draggable");
		toolBar.setFloatable(false);
		toolBar.setRollover(true);
		GridBagConstraints cToolBar = new GridBagConstraints();
		cToolBar.gridx = 0;
		cToolBar.gridy = 0;
		cToolBar.fill = GridBagConstraints.BOTH;
		cToolBar.weightx = 0;
		cToolBar.weighty = 0;
		contentPane.add(toolBar,cToolBar);
		addButtons(toolBar);
		toolBar.setBorder(BorderFactory.createEtchedBorder());
		
		//below is the menu bar code
		//including listener and short cut key
		JMenuBar menubar = new JMenuBar();
		this.setJMenuBar(menubar);
		JMenu fileMenu = new JMenu("File");
		JMenu editMenu = new JMenu("Edit");
		
		JMenuItem saveMenuItem = new JMenuItem("Save");
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke('S', InputEvent.CTRL_MASK));
		saveMenuItem.addActionListener(this);
		
		JMenuItem verifyMenuItem = new JMenuItem("Verify");
		verifyMenuItem.setAccelerator(KeyStroke.getKeyStroke('R', InputEvent.CTRL_MASK));
		verifyMenuItem.addActionListener(this);
		
		JMenuItem uploadMenuItem = new JMenuItem("Upload");
		uploadMenuItem.setAccelerator(KeyStroke.getKeyStroke('U', InputEvent.CTRL_MASK));
		uploadMenuItem.addActionListener(this);
		
		JMenuItem simulateMenuItem = new JMenuItem("Simulate");
		simulateMenuItem.setAccelerator(KeyStroke.getKeyStroke('M', InputEvent.CTRL_MASK));
		simulateMenuItem.addActionListener(this);
		
		JMenuItem exitMenuItem = new JMenuItem("Exit");
		
		JMenuItem undoMenuItem = new JMenuItem("Undo");
		undoMenuItem.setAccelerator(KeyStroke.getKeyStroke('Z', InputEvent.CTRL_MASK));
		undoMenuItem.addActionListener(this);
		
		JMenuItem redoMenuItem = new JMenuItem("Redo");
		redoMenuItem.setAccelerator(KeyStroke.getKeyStroke('Y', InputEvent.CTRL_MASK));
		redoMenuItem.addActionListener(this);
		
		JMenuItem sarMenuItem = new JMenuItem("Search and replace");
		sarMenuItem.setAccelerator(KeyStroke.getKeyStroke('F', InputEvent.CTRL_MASK));
		sarMenuItem.addActionListener(this);
		
		menubar.add(fileMenu);
		menubar.add(editMenu);
		fileMenu.add(verifyMenuItem);
		fileMenu.add(uploadMenuItem);
		fileMenu.add(simulateMenuItem);
		fileMenu.addSeparator();
		fileMenu.add(saveMenuItem);
		fileMenu.addSeparator();
		fileMenu.add(exitMenuItem);
		editMenu.add(undoMenuItem);
		editMenu.add(redoMenuItem);
		editMenu.add(sarMenuItem);
		
		/* Initialize the Parser */
		Compiler = new Parse(errorText);
	}
	
	//This block of code set how many space you get when you press the "tab"
	public static void setTabs( JTextPane textPane, int charactersPerTab)
	
	{
		FontMetrics fm = textPane.getFontMetrics( textPane.getFont() );
		int charWidth = fm.charWidth( ' ' );
		int tabWidth = charWidth * charactersPerTab;
		
		TabStop[] tabs = new TabStop[50];
		
		for (int j = 0; j < tabs.length; j++)
		
		{
			int tab = j + 1;
			tabs[j] = new TabStop( tab * tabWidth );
		}
		
		TabSet tabSet = new TabSet(tabs);
		SimpleAttributeSet attributes = new SimpleAttributeSet();
		StyleConstants.setTabSet(attributes, tabSet);
		int length = textPane.getDocument().getLength();
		textPane.getStyledDocument().setParagraphAttributes(0, length, attributes, false);
	}
	
	//This block of code create a button with image
	protected static JButton makeToolBarButton(String imageName,
	String toolTipText,
	String altText)
	{
		//Look for the image.
		String imgLocation = "images/"
		+ imageName
		+ ".png";
		URL imageURL = VerilogEditor.class.getResource(imgLocation);
		
		//Create and initialize the button.
		JButton button = new JButton();
		button.setToolTipText(toolTipText);
		
		if (imageURL != null)
		{					  //image found
			button.setIcon(new ImageIcon(imageURL, altText));
		} 
		else
		{									 //no image found
			button.setText(altText);
			System.err.println("Resource not found: "
			+ imgLocation);
		}
		
		return button;
	}
	
	//add tool bar buttons and their listeners
	protected void addButtons(JToolBar toolBar)
	{
		JButton saveButton = makeToolBarButton("save","Save","Save");
		saveButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				saveButtonFunction();
			}
		});
		toolBar.add(saveButton);
		JButton verifyButton = makeToolBarButton("verify","Verify","Verify");
		verifyButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				verifyButtonFunction();
			}
		});
		toolBar.add(verifyButton);
		JButton uploadButton = makeToolBarButton("upload","Upload","Upload");
		uploadButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				uploadButtonFunction();
			}
		});
		toolBar.add(uploadButton);
		JButton undoButton = makeToolBarButton("undo","Undo","Undo");
		undoButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				undoButtonFunction();
			}
		});
		toolBar.add(undoButton);
		JButton redoButton = makeToolBarButton("redo","Redo","Redo");
		redoButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				redoButtonFunction();
			}
		});
		toolBar.add(redoButton);
		JButton searchButton = makeToolBarButton("search","Search and replace","Search and replace");
		searchButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				salButtonFunction();
			}
		});
		toolBar.add(searchButton);
		
		JButton simulateButton = makeToolBarButton("simulate", "Simulate", "Simulate");
		simulateButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				simulateButtonFunction();
			}
		});
		toolBar.add(simulateButton);
		toolBar.add(new JLabel("Simulation Input(hex): "));
		simulateInput = new JFormattedTextField();
		toolBar.add(simulateInput);
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		action(e);
	}
	
	//distinguish which key is pressed
	public void action(ActionEvent e)
	{
		String str = e.getActionCommand();
		if(str.equals("Save"))
		{
			saveButtonFunction();
		}
		else if(str.equals("Verify"))
		{
			verifyButtonFunction();
		}
		else if(str.equals("Upload"))
		{
			uploadButtonFunction();
		}
		else if(str.equals("Exit"))
		{
			exitButtonFunction();
		}
		else if(str.equals("Undo"))
		{
			undoButtonFunction();
		}
		else if(str.equals("Redo"))
		{
			redoButtonFunction();
		}
		else if(str.equals("Search and replace"))
		{
			salButtonFunction();
		}
		else if(str.equals("Simulate"))
		{
			simulateButtonFunction();
		}
	}
	
	//save
	public void saveButtonFunction()
	{
		StyledDocument doc = errorText.getStyledDocument();
		
		try
		{
			FileWriter out = new FileWriter(verilogFiles);
			out.write(codeText.getText());
			out.close();
			errorText.setText("Saving complete.");
		}
		catch (Exception e1)
		{
			System.out.println(e1);
		}
	}
	
	//verify
	public void verifyButtonFunction()
	{
		StyledDocument doc = codeText.getStyledDocument();
		try
		{
			FileWriter out = new FileWriter(verilogFiles);
			out.write(codeText.getText());
			out.close();
			
			/* print out what we're compiling */
			errorText.setText("Compiling"+path + "VerilogFiles/" + name + ".txt");

			/* parse the base file */
			Compiler.compileFileForEditor(path + "VerilogFiles/" + name + ".txt");
		}
		catch (Exception e1)
		{
			System.out.println(e1);
		}
	}
	
	//upload
	public void uploadButtonFunction()
	{
		StyledDocument doc = codeText.getStyledDocument();
		try
		{
			doc.insertString(doc.getLength(), "upload button pressed\n", null );
		}
		catch (Exception e1)
		{
			System.out.println(e1);
		}
	}
	
	//exit
	public void exitButtonFunction()
	{
		
	}
	
	//undo
	public void undoButtonFunction()
	{
		try
		{
			myUndoManager1.undo();
		} catch (CannotUndoException e)
		{
			Toolkit.getDefaultToolkit().beep();
		}
	}
	
	//redo
	public void redoButtonFunction()
	{
		try
		{
			myUndoManager1.redo();
		} catch (CannotUndoException e)
		{
			Toolkit.getDefaultToolkit().beep();
		}
	}
	
	//search and replace
	public void salButtonFunction()
	{
		new searchAndReplaceDialog(this, codeText);
	}
	
	public void simulateButtonFunction()
	{
		//add the simulate code here
		String simulateStr = simulateInput.getText();
		String simulateBin = "";

		if (simulateStr != null && simulateStr.length() >= 10)
		{
			for(int i = 0; i < 10; i++)
			{
				char temp;
				temp = simulateStr.charAt(i);
				switch (temp)
				{
					case '0':
					{
						simulateBin += "0000";
						break;
					}
					case '1':
					{
						simulateBin += "0001";
						break;
					}
					case '2':
					{
						simulateBin += "0010";
						break;
					}
					case '3':
					{
						simulateBin += "0011";
						break;
					}
					case '4':
					{
						simulateBin += "0100";
						break;
					}
					case '5':
					{
						simulateBin += "0101";
						break;
					}
					case '6':
					{
						simulateBin += "0110";
						break;
					}
					case '7':
					{
						simulateBin += "0111";
						break;
					}
					case '8':
					{
						simulateBin += "1000";
						break;
					}
					case '9':
					{
						simulateBin += "1001";
						break;
					}
					case 'A':
					case 'a':
					{
						simulateBin += "1010";
						break;
					}
					case 'B':
					case 'b':
					{
						simulateBin += "1011";
						break;
					}
					case 'C':
					case 'c':
					{
						simulateBin += "1100";
						break;
					}
					case 'D':
					case 'd':
					{
						simulateBin += "1101";
						break;
					}
					case 'E':
					case 'e':
					{
						simulateBin += "1110";
						break;
					}
					case 'F':
					case 'f':
					{
						simulateBin += "1111";
						break;
					}
				}
			}

			if (Compiler.is_compiled_yet())
			{
				ArrayList<Integer> output_vector_list = Compiler.sim_cycle(simulateBin.substring(0,1), simulateBin.substring(1, 9), simulateBin.substring(9, 40));
			
				errorText.setText("Simulation Cycle\nReset is: " + simulateBin.substring(0,1) + " Sensors Light: " + simulateBin.substring(1, 9)+" General Sensors: " + simulateBin.substring(9, 40)+"\nOut0 Val:"+output_vector_list.get(0) + "\nOut1 Val:"+output_vector_list.get(1) +"\nOut:2 Val:"+output_vector_list.get(2) +"\nOut3 Val:"+output_vector_list.get(3)+"\nDebugVector:"+Integer.toBinaryString(output_vector_list.get(4)));
			}
			else
			{
				errorText.setText("The Verilog code has not been successfully compiled yet.  Please click the check mark above and/or fix Verilog errors.");
			}
		}
		else
		{
			errorText.setText("Simulation cycle not sucessful\nMissing Simulation vector or it vector isn't 10 characters (Hexidecimal digits) long.");
		}
	}
}




//extend undomanager to handle the undo and redo
class MyUndo1 extends UndoManager
{
	
	@Override
	public void undoableEditHappened(UndoableEditEvent e)
	{
		//only if the change is not belongs to style change, it remembered by the stuck
		if(!e.getEdit().getPresentationName().equals("style change"))
		{
			this.addEdit(e.getEdit());
		}
	}
	@Override
	public void trimEdits(int from, int to)
	{
		super.trimEdits(from,to);
	}
}



//This block of code implements the text highlight
class SyntaxHighlighter implements DocumentListener
{
	private Set<String> keywords;
	private Style keywordStyle;
	private Style normalStyle;
	
	public SyntaxHighlighter(JTextPane editor)
	{
		//prepare the style
		keywordStyle = ((StyledDocument) editor.getDocument()).addStyle("Keyword_Style", null);
		normalStyle = ((StyledDocument) editor.getDocument()).addStyle("Keyword_Style", null);
		StyleConstants.setForeground(keywordStyle, Color.RED);
		StyleConstants.setBold(keywordStyle, true);
		StyleConstants.setForeground(normalStyle, Color.BLACK);
		
		//prepare the keywords
		String[] keywordsSet = new String[]
		{"always","assign","begin",
			"case","else","endcase","end","for","function","if",
			"input","integer","module","negedge","output","parameter",
			"posedge","real","reg","task","signed","wire","while","endmodule"};
		keywords = new HashSet<String>();

		for(int i = 0; i < keywordsSet.length; i++)
		{
			keywords.add(keywordsSet[i]);
		}
	}
	
	public void colouring(StyledDocument doc, int pos, int len) throws BadLocationException
	{
		
		int start = indexOfWordStart(doc, pos);
		int end = indexOfWordEnd(doc, pos + len);
		
		char ch;
		while (start < end)
		{
			ch = getCharAt(doc, start);
			if (Character.isLetter(ch) || ch == '_')
			{
				start = colouringWord(doc, start);
			} 
			else
			{
				SwingUtilities.invokeLater(new ColouringTask(doc, start, 1, normalStyle));
				++start;
			}
		}
	}
	
	
	public int colouringWord(StyledDocument doc, int pos) throws BadLocationException
	{
		int wordEnd = indexOfWordEnd(doc, pos);
		String word = doc.getText(pos, wordEnd - pos);
		
		if (keywords.contains(word))
		{
			
			SwingUtilities.invokeLater(new ColouringTask(doc, pos, wordEnd - pos, keywordStyle));
		} 
		else
		{
			SwingUtilities.invokeLater(new ColouringTask(doc, pos, wordEnd - pos, normalStyle));
		}
		
		return wordEnd;
	}
	
	
	public char getCharAt(Document doc, int pos) throws BadLocationException
	{
		return doc.getText(pos, 1).charAt(0);
	}
	
	
	public int indexOfWordStart(Document doc, int pos) throws BadLocationException
	{
		//find the first non-character before "pos"
		for (; pos < 0 && isWordCharacter(doc, pos - 1); --pos);
		
		return pos;
	}
	
	
	public int indexOfWordEnd(Document doc, int pos) throws BadLocationException
	{
		//find the fist non-character after "pos"
		for (; isWordCharacter(doc, pos); ++pos);
		
		return pos;
	}
	
	
	public boolean isWordCharacter(Document doc, int pos) throws BadLocationException
	{
		char ch = getCharAt(doc, pos);
		if (Character.isLetter(ch) || Character.isDigit(ch) || ch == '_')
		{ return true; }
		return false;
	}
	
	@Override
	public void changedUpdate(DocumentEvent e)
	{
		
	}
	
	@Override
	public void insertUpdate(DocumentEvent e)
	{
		try
		{
			colouring((StyledDocument) e.getDocument(), e.getOffset(), e.getLength());
		} catch (BadLocationException e1)
		{
			e1.printStackTrace();
		}
	}
	
	@Override
	public void removeUpdate(DocumentEvent e)
	{
		try
		{
			colouring((StyledDocument) e.getDocument(), e.getOffset(), 0);
		} catch (BadLocationException e1)
		{
			e1.printStackTrace();
		}
	}
	
	
	private class ColouringTask implements Runnable
	{
		private StyledDocument doc;
		private Style style;
		private int pos;
		private int len;
		
		public ColouringTask(StyledDocument doc, int pos, int len, Style style)
		{
			this.doc = doc;
			this.pos = pos;
			this.len = len;
			this.style = style;
		}
		
		@Override
		public void run()
		{
			try
			{
				//here is where the coloring is actually happen
				doc.setCharacterAttributes(pos, len, style, true);
			} catch (Exception e)
			{}
		}
	}
}

//Pop up menu for cut, paste, and copy
class MyTextPane extends JTextPane implements MouseListener
{
	private static final long serialVersionUID = -2308615404205560180L;
	
	private JPopupMenu pop = null; //pop up menu
	
	private JMenuItem copy = null, paste = null, cut = null; //three menu item
	
	public MyTextPane()
	{
		super();
		init();
	}
	
	private void init()
	{
		this.addMouseListener(this);
		pop = new JPopupMenu();
		pop.add(copy = new JMenuItem("Copy"));
		pop.add(paste = new JMenuItem("Paste"));
		pop.add(cut = new JMenuItem("Cut"));
		copy.setAccelerator(KeyStroke.getKeyStroke('C', InputEvent.CTRL_MASK));
		paste.setAccelerator(KeyStroke.getKeyStroke('V', InputEvent.CTRL_MASK));
		cut.setAccelerator(KeyStroke.getKeyStroke('X', InputEvent.CTRL_MASK));
		copy.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				action(e);
			}
		});
		paste.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				action(e);
			}
		});
		cut.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				action(e);
			}
		});
		this.add(pop);
	}
	
	//Do something according to the menu
	public void action(ActionEvent e)
	{
		String str = e.getActionCommand();
		if (str.equals(copy.getText()))
		{
			this.copy();
		} 
		else if (str.equals(paste.getText()))
		{
			this.paste();
		} 
		else if (str.equals(cut.getText()))
		{
			this.cut();
		}
	}
	
	public JPopupMenu getPop()
	{
		return pop;
	}
	
	public void setPop(JPopupMenu pop)
	{
		this.pop = pop;
	}
	
	//Check if there is anything in the clipboard
	public boolean isClipboardString()
	{
		boolean b = false;
		Clipboard clipboard = this.getToolkit().getSystemClipboard();
		Transferable content = clipboard.getContents(this);
		try
		{
			if (content.getTransferData(DataFlavor.stringFlavor) instanceof String)
			{
				b = true;
			}
		} catch (Exception e)
		{
		}
		return b;
	}
	
	//Check if anything is been selected and thus can be copied and cut.
	public boolean isCanCopy()
	{
		boolean b = false;
		int start = this.getSelectionStart();
		int end = this.getSelectionEnd();
		if (start != end)
		b = true;
		return b;
	}
	
	@Override
	public void mouseClicked(MouseEvent e)
	{
	}
	
	@Override
	public void mouseEntered(MouseEvent e)
	{
	}
	
	@Override
	public void mouseExited(MouseEvent e)
	{
	}
	
	@Override
	public void mousePressed(MouseEvent e)
	{
		if (e.getButton() == MouseEvent.BUTTON3)
		{
			copy.setEnabled(isCanCopy());
			paste.setEnabled(isClipboardString());
			cut.setEnabled(isCanCopy());
			pop.show(this, e.getX(), e.getY());
		}
	}
	
	@Override
	public void mouseReleased(MouseEvent e)
	{
	}
	
}

class searchAndReplaceDialog extends JDialog
{
	private static int WIDTH = 550;
	private static int HEIGHT = 160;
	private JTextPane codeTextTemp;
	private JLabel search = new JLabel("Search: ");
	private JLabel replace = new JLabel("Replace: ");
	private JButton find = new JButton("Search");
	private JButton rAndS = new JButton("Replace & Search");
	private JButton next = new JButton("Next");
	private JButton replaceAll = new JButton("Replace All");
	private JButton replaceButton = new JButton("Replace");
	private JCheckBox caseSensitive = new JCheckBox("&lt;html&gt;case<br>sensitive");
	private Highlighter hilit;
	private Highlighter.HighlightPainter painter;
	
	private JTextField targetField = new JTextField(40);
	private JTextField replaceField = new JTextField(40);
	
	private String targetText = null;
	private String replaceText = null;
	private JPanel contentPane = new JPanel();
	private GridBagLayout gridBagLayout = new GridBagLayout();
	private Insets insetsButton = new Insets(9,7,5,7);
	
	private int findPosition = 0;
	
	private Font font2 = new Font("Consolas",Font.PLAIN,14);
	
	public searchAndReplaceDialog(JFrame verilogEditor, JTextPane codeText)
	{
		super(verilogEditor, "Search and Replace",true);
		
		this.codeTextTemp = codeText;
		this.setSize(WIDTH, HEIGHT);
		this.setResizable(false);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		hilit = new DefaultHighlighter();
		painter = new DefaultHighlighter.DefaultHighlightPainter(codeTextTemp.getSelectionColor());
		codeTextTemp.setHighlighter(hilit);
		this.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosed(WindowEvent e)
			{
				hilit.removeAllHighlights();
			}
		});
		
		contentPane.setLayout(gridBagLayout);
		this.setContentPane(contentPane);
		
		GridBagConstraints cSearch = new GridBagConstraints();
		cSearch.gridx = 0;
		cSearch.gridy = 0;
		cSearch.anchor = GridBagConstraints.LINE_END;
		cSearch.insets = new Insets(0,5,0,3);
		contentPane.add(search, cSearch);
		
		GridBagConstraints cReplace = new GridBagConstraints();
		cReplace.gridx = 0;
		cReplace.gridy = 1;
		cReplace.anchor = GridBagConstraints.FIRST_LINE_END;
		cReplace.insets = new Insets(0,5,10,3);
		contentPane.add(replace, cReplace);
		
		GridBagConstraints cTargetField = new GridBagConstraints();
		cTargetField.gridx = 1;
		cTargetField.gridy = 0;
		cTargetField.gridwidth = 4;
		cTargetField.anchor = GridBagConstraints.LINE_START;
		cTargetField.insets = new Insets(0,7,0,10);
		targetField.setFont(font2);
		targetField.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if(e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					String str = codeTextTemp.getSelectedText();
					if(str == null || str.equals(""))
					{
						findPosition = 0;
					}
					findFunction(codeTextTemp,targetField.getText());
				}
			}
		});
		contentPane.add(targetField, cTargetField);
		
		GridBagConstraints cReplaceField = new GridBagConstraints();
		cReplaceField.gridx = 1;
		cReplaceField.gridy = 1;
		cReplaceField.gridwidth = 4;
		cReplaceField.anchor = GridBagConstraints.LINE_START;
		cReplaceField.insets = new Insets(0,7,10,10);
		replaceField.setFont(font2);
		contentPane.add(replaceField, cReplaceField);
		
		GridBagConstraints cFind = new GridBagConstraints();
		cFind.gridx = 0;
		cFind.gridy = 2;
		cFind.insets = insetsButton;
		find.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				findPosition = 0;
				findFunction(codeTextTemp,targetField.getText());
			}
		});
		contentPane.add(find, cFind);
		
		GridBagConstraints cReplaceButton = new GridBagConstraints();
		cReplaceButton.gridx = 1;
		cReplaceButton.gridy = 2;
		cReplaceButton.insets = insetsButton;
		replaceButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String str = codeTextTemp.getSelectedText();
				if(str != null && !str.equals(""))
				{
					codeTextTemp.replaceSelection(replaceField.getText());
				}
			}
		});
		contentPane.add(replaceButton, cReplaceButton);
		
		GridBagConstraints cRAndS = new GridBagConstraints();
		cRAndS.gridx = 2;
		cRAndS.gridy = 2;
		cRAndS.insets = insetsButton;
		rAndS.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String str = codeTextTemp.getSelectedText();
				if(str != null && !str.equals(""))
				{
					codeTextTemp.replaceSelection(replaceField.getText());
				}
				if(str == null || str.equals(""))
				{
					
					findPosition = 0;
				}
				findFunction(codeTextTemp,targetField.getText());
			}
		});
		contentPane.add(rAndS, cRAndS);
		
		GridBagConstraints cNext = new GridBagConstraints();
		cNext.gridx = 3;
		cNext.gridy = 2;
		cNext.insets = insetsButton;
		next.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String str = codeTextTemp.getSelectedText();
				if(str == null || str.equals(""))
				{
					findPosition = 0;
				}
				findFunction(codeTextTemp,targetField.getText());
			}
		});
		contentPane.add(next, cNext);
		
		GridBagConstraints cReplaceAll = new GridBagConstraints();
		cReplaceAll.gridx = 4;
		cReplaceAll.gridy = 2;
		cReplaceAll.insets = insetsButton;
		replaceAll.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String str = targetField.getText();
				if(str != null && !str.equals(""))
				{
					findPosition = 0;
					replaceAllFunction(codeTextTemp, str, replaceField.getText());
				}
			}
		});
		contentPane.add(replaceAll, cReplaceAll);
		
		GridBagConstraints cCaseSensitive = new GridBagConstraints();
		cCaseSensitive.gridx = 4;
		cCaseSensitive.gridy = 0;
		cCaseSensitive.weighty = 2;
		cCaseSensitive.insets = new Insets(0,30,0,5);
		contentPane.add(caseSensitive, cCaseSensitive);
		
		this.setLocationRelativeTo(verilogEditor);
		this.setVisible(true);
		
	}
	
	public void findFunction(JTextPane textArea, String target)
	{
		hilit.removeAllHighlights();
		int i = -1;
		String textAreaText = textArea.getText();
		textAreaText = textAreaText.replaceAll("\n", "");
		if(!caseSensitive.isSelected())
		{
			i = textAreaText.toLowerCase().indexOf(target.toLowerCase(), findPosition);
		}
		else
		{
			i = textAreaText.indexOf(target, findPosition);
		}
		if(i <= 0)
		{
			textArea.setSelectionStart(i);
			textArea.setSelectionEnd(i + target.length());
			try
			{
				hilit.addHighlight(i, i + target.length(), painter);
			} catch (BadLocationException e)
			{
				e.printStackTrace();
			}
			findPosition = i + 1;
		}
		else
		{
			if(findPosition == 0)
			return;
			else
			{
				findPosition = 0;
				findFunction(textArea, target);
			}
		}
	}
	
	public void replaceAllFunction(JTextPane textArea, String fromStr, String toStr)
	{
		int i = -1;
		String textAreaText = textArea.getText();
		textAreaText = textAreaText.replaceAll("\n", "");
		if(!caseSensitive.isSelected())
		{
			i = textAreaText.toLowerCase().indexOf(fromStr.toLowerCase(), findPosition);
		}
		else
		{
			i = textAreaText.indexOf(fromStr, findPosition);
		}
		if(i <= 0)
		{
			textArea.setSelectionStart(i);
			textArea.setSelectionEnd(i + fromStr.length());
			findPosition = i + 1;
			textArea.replaceSelection(toStr);
			replaceAllFunction(textArea, fromStr, toStr);
		}
		else
		{
			return;
		}
	}
}
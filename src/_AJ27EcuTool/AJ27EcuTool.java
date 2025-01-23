package _AJ27EcuTool;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JTextArea;
import java.awt.Component;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Box;
import java.awt.Dimension;
import javax.swing.JScrollPane;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.awt.event.ActionEvent;
import java.awt.Font;

import com.fazecast.jSerialComm.*;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

public class AJ27EcuTool {
	
	private JFrame frame;
	
	ExecutorService executorService1;
	
	private JFileChooser fc;

	private EcuFile cpu1CodeFile;
	private EcuFile cpu2CodeFile;
		
	private EcuFile programmer1;
	private EcuFile programmer2;
	
	JLabel CPU1LoaderFileLabel;
	JLabel CPU2LoaderFileLabel;
	JLabel CPU1CodeLabel;
	JLabel CPU2CodeLabel;
	
	JComboBox<CanbusMessage> commandComboBox;

	JLabel serialPortLabel;
	JLabel arduinoStatusLabel;
	JLabel ecuStatusLabel;
	JLabel eeprom1SigLabel;
	JLabel eeprom2SigLabel;
	JLabel mainCPUStatusLabel;
	JLabel secondCPUStatusLabel;
	
	JButton commandButton;
	JButton programmerButton;
	JButton serialSelectButton;
	JButton CPU1LoaderButton;
	JButton CPU2LoaderButton;
	JButton eepromdownloadButton;
	JButton reflashButton;
	
	JTextArea monitorTextArea;
	
	JProgressBar uploadProgressBar;
	JProgressBar reflashProgressBar;
	JProgressBar downloadProgressBar;
	
	SerialPort arduinoPort = null;
	int baudRate = 250000;
	
	boolean procedureOngoing = false;
	boolean arduinoDetected = false;
	boolean ecuBootMode = false;
	boolean ecuNormalMode = false;
	boolean cpu1ProgrammerFileSelected = false;
	boolean cpu2ProgrammerFileSelected = false;
	boolean programmersLoaded = false;
	boolean persistentDataLoaded = false;
	
	File persistentDataFile = null;
	byte[] persistentFileData = null;

	CanbusMessage pingArduino = new CanbusMessage(0x10, new byte[] {(byte) 0x55,(byte) 0x55}, "Arduino 0x10 ping");
	CanbusMessage pingECU = new CanbusMessage(0x20, new byte[] {(byte) 0x22, (byte) 0xe7, (byte) 0x25}, "ECU 0x20 ping");
	
	ReceiveQueue receiveQueue;
	
	String msg22_e7_25 = "Get CPU1 EEPROM Signature Part 1";
	byte[] data22_e7_25 = new byte[]{(byte)0x22,(byte)0xe7,(byte)0x25};
	CanbusMessage can22_e7_25 = new CanbusMessage(0x20, data22_e7_25, msg22_e7_25);
	
	String msg22_e7_26 = "Get CPU1 EEPROM Signature Part 2";
	byte[] data22_e7_26 = new byte[]{(byte)0x22,(byte)0xe7,(byte)0x26};
	CanbusMessage can22_e7_26 = new CanbusMessage(0x20, data22_e7_26, msg22_e7_26);

	String msg22_e7_27 = "Get CPU2 EEPROM Signature Part 1";
	byte[] data22_e7_27 = new byte[]{(byte)0x22,(byte)0xe7,(byte)0x27};
	CanbusMessage can22_e7_27 = new CanbusMessage(0x20, data22_e7_27, msg22_e7_27);
		
	String msg22_e7_28 = "Get CPU2 EEPROM Signature Part 2";
	byte[] data22_e7_28 = new byte[]{(byte)0x22,(byte)0xe7,(byte)0x28};
	CanbusMessage can22_e7_28 = new CanbusMessage(0x20, data22_e7_28, msg22_e7_28);
		
	String msg22_e7_29 = "Get CPU1 EEPROM erase count";
	byte[] data22_e7_29 = new byte[]{(byte)0x22,(byte)0xe7,(byte)0x29};
	CanbusMessage can22_e7_29 = new CanbusMessage(0x20, data22_e7_29, msg22_e7_29);
		
	String msg22_e7_2a = "Get CPU2 EEPROM erase count";
	byte[] data22_e7_2a = new byte[]{(byte)0x22,(byte)0xe7,(byte)0x2a};
	CanbusMessage can22_e7_2a = new CanbusMessage(0x20, data22_e7_2a, msg22_e7_2a);
	
	String msg27_01 = "Get CPU1 RTC counter value";
	byte[] data27_01 = new byte[]{(byte)0x27,(byte)0x01};
	CanbusMessage can27_01 = new CanbusMessage(0x20, data27_01, msg27_01);
	
	String msg3f = "Ping CPU1 with service 0x3f";
	byte[] data3f = new byte[]{(byte)0x3f};
	CanbusMessage can3f = new CanbusMessage(0x20, data3f, msg3f);
				
	CanbusMessage selectedMessage;
	
	Vector<CanbusMessage> tpuPhaseMessages = new Vector<CanbusMessage>();
	Vector<CanbusMessage> programmerPhaseMessages = new Vector<CanbusMessage>();
	
	String msg31_a2 = "Validate 1k block sent to CPU1";
	byte[] data31_a2 = new byte[]{(byte)0x31,(byte)0xa2};
	CanbusMessage can31_a2 = new CanbusMessage(0x20, data31_a2, msg31_a2);

	String msg32_a2 = "Confirm 1k block upload complete to CPU1";
	byte[] data32_a2 = new byte[]{(byte)0x32,(byte)0xa2};
	CanbusMessage can32_a2 = new CanbusMessage(0x20, data32_a2, msg32_a2);

	String msg32_b2 = "Confirm 1k block upload complete to CPU2";
	byte[] data32_b2 = new byte[]{(byte)0x32,(byte)0xb2};
	CanbusMessage can32_b2 = new CanbusMessage(0x20, data32_b2, msg32_b2);

	String msg32_a5 = "Validate checksum and execute programmer1";
	byte[] data32_a5 = new byte[]{(byte)0x32,(byte)0xa5};
	CanbusMessage can32_a5 = new CanbusMessage(0x20, data32_a5, msg32_a5);
	
	String msg32_b5 = "Validate checksum and execute programmer2";
	byte[] data32_b5 = new byte[]{(byte)0x32,(byte)0xb5};
	CanbusMessage can32_b5 = new CanbusMessage(0x20, data32_b5, msg32_b5);
	
	String msg37_80 = "Clear load 1k target address";
	byte[] data37_80 = new byte[]{(byte)0x37,(byte)0x80};
	CanbusMessage can37_80 = new CanbusMessage(0x20, data37_80, msg37_80);
	
	String msg37_00 = "Clear load 1k target address";
	byte[] data37_00 = new byte[]{(byte)0x37,(byte)0x00};
	CanbusMessage can37_00 = new CanbusMessage(0x20, data37_00, msg37_00);
	
	String msgArduinoUploadStart = "Start 1k upload to arduino";
	byte[] dataArduinoUploadStart = new byte[]{(byte)0x34,(byte)0x00};
	CanbusMessage arduinoUploadStart = new CanbusMessage(0x10, dataArduinoUploadStart, msgArduinoUploadStart);
	
	String msgArduinoUploadEnd1 = "Completed 1k upload to arduino for CPU1";
	byte[] dataArduinoUploadEnd1 = new byte[]{(byte)0x31,(byte)0xa2};
	CanbusMessage arduinoUploadEnd1 = new CanbusMessage(0x10, dataArduinoUploadEnd1, msgArduinoUploadEnd1);
	
	String msgArduinoUploadEnd2 = "Completed 1k upload to arduino for CPU2";
	byte[] dataArduinoUploadEnd2 = new byte[]{(byte)0x31,(byte)0xb2};
	CanbusMessage arduinoUploadEnd2 = new CanbusMessage(0x10, dataArduinoUploadEnd2, msgArduinoUploadEnd2);
	
	String msg10 = "Start ecu diagnostic session control";
	byte[] data10 = new byte[]{(byte)0x10};
	CanbusMessage can10 = new CanbusMessage(0x7e8, data10, msg10);
	
	String msg20 = "Stop ecu diagnostic session control";
	byte[] data20 = new byte[]{(byte)0x20};
	CanbusMessage can20 = new CanbusMessage(0x7e8, data20, msg20);
	JButton savePersistentButton;
	JButton programPersistentButton;
	private JButton persistentDataButton;
	JLabel persistentDataLabel;
	JProgressBar persistentProgressBar;
	private JButton saveRAMButton;
	private JProgressBar ramDownloadProgressBar;
	

	
	/*
	 * Class for creating and storing canbus messages
	 */
	
	protected class CanbusMessage
	{
		private byte[] id; //11 bit left justified
		private byte[] data; //8 bytes
		private String commandDetails;
				
		//constructor for creating messages
		//header is 11 bit left justified
		//so 0x10<>02 00, 0x20<>04 00, 0x30<>06 00, 0x50<> 0a 00
		//info is up to 7 byte message, in info[1] to info[7]
		//data[0] is set to message length
		CanbusMessage(int header, byte[] info, String cmd)
		{
			id = new byte[2];
			data = new byte[]{0,0,0,0,0,0,0,0};
			id[0] = (byte) ((header >> 3) & 0xff);
			id[1] = (byte) ((header & 0x7) << 5);
			int infoLength = info.length;
			if (infoLength > 7)
				infoLength = 7;
			for (int x=0; x < info.length; x++)
			{
				data[x+1] = info[x];
			}
			data[0] = (byte) infoLength;
			commandDetails = cmd;
		}
		
		//constructor for receiving messages
		// first 2 bytes are ID
		// subsequent 8 bytes are data
		CanbusMessage(byte[] message)
		{
			if (message.length == 10)
			{
				data = new byte[8];
				id = new byte[2];
				id[0] = message[0];
				id[1] = message[1];
				for (int x=0;x<8;x++)
				{
					data[x] = message[x+2];
				}
				commandDetails = "";
			}
			else
			{
				data = new byte[]{0,0,0,0,0,0,0,0};
				id = new byte[]{0,0};
				commandDetails = "null message";
			}
		}
		
		int getID()
		{
			int value = ((id[0] & 0xff) << 3) + ((id[1] & 0xff) >> 5);
			return value;
		}
		
		byte[] getData()
		{
			byte[] value = new byte[8];
			System.arraycopy(data, 0, value, 0, 8);
			return value;
		}
		
		public String toDetailsString()
		{
			String s = commandDetails;
			s = s + String.format(": header 0x%3x : data(hex) %2x %2x %2x %2x %2x %2x %2x %2x",
					this.getID(), data[0], data[1],data[2],data[3],data[4],data[5],data[6],data[7]);
			return s;
		}
		
		public String toString()
		{
			return commandDetails;
		}
				
		boolean sendMessage()
		{
			byte[] txData = new byte[10];
			txData[0] = id[0];
			txData[1] = id[1];
			for (int x=0;x<8;x++)
			{
				txData[x+2] = data[x];
			}
			
			if (arduinoPort.isOpen())
			{				
				arduinoPort.writeBytes(txData, 10);
				return true;
			}
			else
			{
				return false;
			}
		}
	}
	
	/*
	 * class for holding information about an ecu file
	 */
	
	protected class EcuFile
	{
		File dataFile;
		byte[] fileData;
		int dataBlocks;
		byte[] signature;
		boolean validFlag;
		JLabel statusLabel;
		
		EcuFile(byte[] sig, int blocks, JLabel label)
		{
			signature = sig;
			dataBlocks = blocks;
			statusLabel = label;
			validFlag = false;
		}
		
		void validateAndSetFile(File targetFile)
		{
			try ( FileInputStream pf1 = new FileInputStream(targetFile) )
			{
				final int START_OFFSET = 0;
				final int START_LEN = 2;
				final int SIG_OFFSET = 4;
				final int SIG_LEN = 2;
				final int LENGTH_OFFSET = 3;
				
				byte[] START_SEQ = {(byte) 0x04,(byte) 0x00};
				
				boolean validName =  targetFile.getName().endsWith(".B68") || targetFile.getName().endsWith(".b68");
				
				int fileLength = (int) targetFile.length();

				byte[] targetBytes = new byte[fileLength];
				pf1.read(targetBytes);
				
				byte[] startSeq = new byte[START_LEN];
				System.arraycopy(targetBytes, START_OFFSET, startSeq, 0, START_LEN);
				boolean validStart = Arrays.equals(startSeq, START_SEQ);
				
				byte[] sigSeq = new byte[SIG_LEN];
				System.arraycopy(targetBytes, SIG_OFFSET, sigSeq, 0, SIG_LEN);
				boolean validSignature = Arrays.equals(sigSeq, signature);
				
				byte[] blockCount = new byte[1];
				System.arraycopy(targetBytes, LENGTH_OFFSET, blockCount, 0, 1);
				int totalBlocks = Byte.toUnsignedInt(blockCount[0]);
				
				boolean validLengthParam = (totalBlocks == dataBlocks);
				
				boolean validFileLength = (fileLength == ((totalBlocks*1029) + 6));
							
				final int END_OFFSET = -2;
				final int END_LEN = 2;
				byte[] END_SEQ = {(byte) 0x00,(byte) 0x00};
				byte[] endSeq = new byte[END_LEN];
				System.arraycopy(targetBytes, fileLength+END_OFFSET, endSeq, 0, END_LEN);
				boolean validEnd = Arrays.equals(endSeq, END_SEQ);
				
				if (validName && validStart && validSignature
						&& validLengthParam && validFileLength && validEnd)
				{
					dataFile = targetFile;
					fileData = targetBytes;
					validFlag = true;
					statusLabel.setText(dataFile.getName());
				}
				else
				{
					//indicate file is invalid
					JPanel panel = new JPanel();
					JOptionPane.showMessageDialog(panel, "Invalid file format - file not loaded");
				}
				
				pf1.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		byte[] getDataBlock(int index)
		{
			byte[] block = new byte[1024];
			if (index < dataBlocks)
			{
				System.arraycopy(fileData, 9+(index*1029), block, 0, 1024);
			}
			return block;
		}
		
		boolean isValid()
		{
			return validFlag;
		}
		
		int getNumberOfBlocks()
		{
			return dataBlocks;
		}
		
	}

	
	/*
	 * class for storing packets received from arduino and ecu
	 * thread safe for accessing/modifying the queue
	 */
	
	protected class ReceiveQueue
	{
		private ArrayList<CanbusMessage> receiveMessages;

		ReceiveQueue()
		{
			receiveMessages = new ArrayList<CanbusMessage>();
		}
		
		protected void add(CanbusMessage m)
		{
			synchronized(this)
			{
				receiveMessages.add(m);
			}
		}
		
		protected CanbusMessage getFirstElement()
		{
			synchronized(this)
			{
				return receiveMessages.get(0);
			}
		}
		
		protected int size()
		{
			synchronized(this)
			{
				return receiveMessages.size();
			}
		}
		
		protected void removeFirstElement()
		{
			synchronized(this)
			{
				receiveMessages.remove(0);
			}
		}
		
		protected void flush()
		{
			synchronized(this)
			{
				receiveMessages.clear();
			}
			
		}

	}
	
	
	protected class CanbusResponses
	{
		private List<CanbusMessage> messageResponses; //note this references the same objects as in the passed in List
		private boolean correctResponse;
		CanbusMessage targetResponse = null;
		
		CanbusResponses(boolean success, CanbusMessage desiredResponse, List<CanbusMessage> responses)
		{
			correctResponse = success;
			messageResponses = new ArrayList<CanbusMessage>(responses);
			targetResponse = desiredResponse;
		}
		
		boolean getResult()
		{
			return correctResponse;
		}
		
		List<CanbusMessage> getResponses()
		{
			return messageResponses;
		}
		
		CanbusMessage getTargetResponse()
		{
			return targetResponse;
		}
	}


	/*
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					AJ27EcuTool window = new AJ27EcuTool();
					window.frame.setVisible(true);					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public AJ27EcuTool() {
		initialize();	
		initializeCanMessages();
		setExitBehavior();
				
		//during upload/download/program, other buttons should be disabled

	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		fc = new JFileChooser();
		executorService1 = Executors.newSingleThreadExecutor();
		
		
		frame = new JFrame("AJ27 Reflash Tool");
		frame.setBounds(100, 100, 688, 466);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel commandsPanel = new JPanel();
		frame.getContentPane().add(commandsPanel, BorderLayout.NORTH);
		commandsPanel.setLayout(new GridLayout(2, 2, 0, 0));
		
		JPanel singleCommandsPanel = new JPanel();
		commandsPanel.add(singleCommandsPanel);
		singleCommandsPanel.setLayout(new BoxLayout(singleCommandsPanel, BoxLayout.Y_AXIS));
		
		Component rigidArea_1 = Box.createRigidArea(new Dimension(70, 70));
		singleCommandsPanel.add(rigidArea_1);
		
		JLabel commandLabel = new JLabel("Send Command");
		commandLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		singleCommandsPanel.add(commandLabel);
		
		commandComboBox = new JComboBox<CanbusMessage>();
		singleCommandsPanel.add(commandComboBox);
		commandComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox<CanbusMessage> jcb = (JComboBox<CanbusMessage>) e.getSource();
				selectedMessage = (CanbusMessage)jcb.getSelectedItem();
			}	
		});
		
		commandButton = new JButton("Send");
		commandButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//send canbus command to Arduino
				if (selectedMessage != null)
				{
					// send message, don't try to read response, just use to show in text monitor
					byte[] vector = {0x01};
					monitorTextArea.append("Sending "+selectedMessage.toDetailsString()+"\n");
					CanbusResponses reply = canbusRequestAndResponses(1, selectedMessage, vector, 0x30, null, 100L);
					printRequestResult(reply);
				}
				else
				{
					 Object[] options = { "OK", "CANCEL" };
					 JOptionPane.showOptionDialog(null, "Click OK to continue", "Please select a message first",
					             JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
					             null, options, options[0]);

				}
			}
		});
		commandButton.setEnabled(false);
		commandButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		singleCommandsPanel.add(commandButton);
		
		Component rigidArea_2 = Box.createRigidArea(new Dimension(20, 20));
		singleCommandsPanel.add(rigidArea_2);

		
		JPanel statusPanel = new JPanel();
		commandsPanel.add(statusPanel);
		statusPanel.setLayout(new GridLayout(0, 2, 0, 0));
		
		JLabel componentLabel = new JLabel("Component");
		componentLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
		componentLabel.setHorizontalAlignment(SwingConstants.CENTER);
		statusPanel.add(componentLabel);
		
		JLabel statusLabel = new JLabel("Status");
		statusLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
		statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		statusPanel.add(statusLabel);
		
		JLabel arduinoLabel = new JLabel("Arduino");
		arduinoLabel.setHorizontalAlignment(SwingConstants.CENTER);
		statusPanel.add(arduinoLabel);
		
		arduinoStatusLabel = new JLabel("Not Found");
		arduinoStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		statusPanel.add(arduinoStatusLabel);
		
		JLabel mainCPULabel = new JLabel("CPU1 Programmer");
		mainCPULabel.setHorizontalAlignment(SwingConstants.CENTER);
		statusPanel.add(mainCPULabel);
		
		mainCPUStatusLabel = new JLabel("Not installed");
		mainCPUStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		statusPanel.add(mainCPUStatusLabel);
		
		JLabel secondCPULabel = new JLabel("CPU2 Programmer");
		secondCPULabel.setHorizontalAlignment(SwingConstants.CENTER);
		statusPanel.add(secondCPULabel);
		
		secondCPUStatusLabel = new JLabel("Not installed");
		secondCPUStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		statusPanel.add(secondCPUStatusLabel);
		
		JLabel ecuBootLabel = new JLabel("ECU Boot Mode");
		ecuBootLabel.setHorizontalAlignment(SwingConstants.CENTER);
		statusPanel.add(ecuBootLabel);
		
		ecuStatusLabel = new JLabel("Not Detected");
		ecuStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		statusPanel.add(ecuStatusLabel);
		
		JLabel eeprom1Label = new JLabel("Eeprom1 signature");
		eeprom1Label.setHorizontalAlignment(SwingConstants.CENTER);
		statusPanel.add(eeprom1Label);
		
		eeprom1SigLabel = new JLabel("Not retrieved");
		eeprom1SigLabel.setHorizontalAlignment(SwingConstants.CENTER);
		statusPanel.add(eeprom1SigLabel);
		
		JLabel eeprom2Label = new JLabel("Eeprom2 signature");
		eeprom2Label.setHorizontalAlignment(SwingConstants.CENTER);
		statusPanel.add(eeprom2Label);
		
		eeprom2SigLabel = new JLabel("Not retrieved");
		eeprom2SigLabel.setHorizontalAlignment(SwingConstants.CENTER);
		statusPanel.add(eeprom2SigLabel);
		
		JPanel programPanel = new JPanel();
		commandsPanel.add(programPanel);
		programPanel.setLayout(new GridLayout(0, 2, 0, 0));
		
		programmerButton = new JButton("Upload programmers");
		programmerButton.setEnabled(false);
		programPanel.add(programmerButton);
		programmerButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				uploadProgrammers();
			}
		});
		
		uploadProgressBar = new JProgressBar();
		uploadProgressBar.setStringPainted(true);
		programPanel.add(uploadProgressBar);
		
		eepromdownloadButton = new JButton("Download EEPROMs");
		eepromdownloadButton.setEnabled(false);
		programPanel.add(eepromdownloadButton);
		eepromdownloadButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				downloadEeproms();
			}
		});
		
		downloadProgressBar = new JProgressBar();
		downloadProgressBar.setStringPainted(true);
		programPanel.add(downloadProgressBar);
		
		reflashButton = new JButton("Reflash EEPROMs");
		reflashButton.setEnabled(false);
		reflashButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		programPanel.add(reflashButton);
		reflashButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reflashEeproms();
			}
		});
		
		serialSelectButton = new JButton("Select Serial Port");
		serialSelectButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectSerialPort();
			}
		});
		
		reflashProgressBar = new JProgressBar();
		reflashProgressBar.setStringPainted(true);
		programPanel.add(reflashProgressBar);
		
		programPersistentButton = new JButton("Program persistent data");
		programPersistentButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				writePersistentData();
			}
		});
		programPersistentButton.setEnabled(false);
		programPanel.add(programPersistentButton);
		
		persistentProgressBar = new JProgressBar();
		persistentProgressBar.setStringPainted(true);
		programPanel.add(persistentProgressBar);
		
		savePersistentButton = new JButton("Save Persistent Data");
		savePersistentButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				byte[] pData = retrieveEeprom3Data();
				savePersistentData(pData);
			}
		});
		savePersistentButton.setEnabled(false);
		programPanel.add(savePersistentButton);
		
		JButton clearConsoleButton = new JButton("Clear Console");
		programPanel.add(clearConsoleButton);
		clearConsoleButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				monitorTextArea.setText("");
			}
		});
		
		saveRAMButton = new JButton("Save RAM");
		saveRAMButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				retrieveRamData();
			}
		});
		saveRAMButton.setEnabled(false);
		programPanel.add(saveRAMButton);
		
		ramDownloadProgressBar = new JProgressBar();
		ramDownloadProgressBar.setStringPainted(true);
		programPanel.add(ramDownloadProgressBar);
		programPanel.add(serialSelectButton);
		
		serialPortLabel = new JLabel("<Unknown>");
		serialPortLabel.setHorizontalAlignment(SwingConstants.CENTER);
		programPanel.add(serialPortLabel);

		
		JPanel filesPanel = new JPanel();
		commandsPanel.add(filesPanel);
		filesPanel.setLayout(new GridLayout(0, 2, 0, 0));
		
		CPU1LoaderButton = new JButton("CPU1 Programmer File");
		CPU1LoaderButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int returnVal = fc.showOpenDialog(CPU1LoaderButton);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					programmer1.validateAndSetFile(fc.getSelectedFile());
					validateUploadProgrammersReady();
				}
			}
		});
		filesPanel.add(CPU1LoaderButton);
		
		CPU1LoaderFileLabel = new JLabel("<Unspecified>");
		CPU1LoaderFileLabel.setHorizontalAlignment(SwingConstants.CENTER);
		filesPanel.add(CPU1LoaderFileLabel);
		
		CPU2LoaderButton = new JButton("CPU2 Programmer File");
		CPU2LoaderButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int returnVal = fc.showOpenDialog(CPU2LoaderButton);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					programmer2.validateAndSetFile(fc.getSelectedFile());
					validateUploadProgrammersReady();
				}
			}
		});
		filesPanel.add(CPU2LoaderButton);
		
		CPU2LoaderFileLabel = new JLabel("<Unspecified>");
		CPU2LoaderFileLabel.setHorizontalAlignment(SwingConstants.CENTER);
		filesPanel.add(CPU2LoaderFileLabel);
		
		JButton CPU1CodeButton = new JButton("CPU1 Code File");
		CPU1CodeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int returnVal = fc.showOpenDialog(CPU1CodeButton);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					cpu1CodeFile.validateAndSetFile(fc.getSelectedFile());
					validateCodeFilesReady();
				}
			}
		});
		filesPanel.add(CPU1CodeButton);
		
		CPU1CodeLabel = new JLabel("<Unspecified>");
		CPU1CodeLabel.setHorizontalAlignment(SwingConstants.CENTER);
		filesPanel.add(CPU1CodeLabel);
		
		JButton CPU2CodeButton = new JButton("CPU2 Code File");
		CPU2CodeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int returnVal = fc.showOpenDialog(CPU2CodeButton);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					cpu2CodeFile.validateAndSetFile(fc.getSelectedFile());
					validateCodeFilesReady();
				}
			}
		});
		filesPanel.add(CPU2CodeButton);
		
		CPU2CodeLabel = new JLabel("<Unspecified>");
		CPU2CodeLabel.setHorizontalAlignment(SwingConstants.CENTER);
		filesPanel.add(CPU2CodeLabel);
		
		JScrollPane MonitorScrollPane = new JScrollPane();
		frame.getContentPane().add(MonitorScrollPane, BorderLayout.CENTER);
		
		monitorTextArea = new JTextArea();
		monitorTextArea.setEditable(false);
		MonitorScrollPane.setViewportView(monitorTextArea);
		
		JLabel MonitorLabel = new JLabel("Console");
		MonitorLabel.setHorizontalAlignment(SwingConstants.CENTER);
		MonitorScrollPane.setColumnHeaderView(MonitorLabel);
		
		byte[] MAIN_BOOT_SEQ = {(byte) 0xf0,(byte) 0x97};
		programmer1 = new EcuFile(MAIN_BOOT_SEQ, 5, CPU1LoaderFileLabel);
		
		byte[] SUB_BOOT_SEQ = {(byte) 0x03,(byte) 0x15};
		programmer2 = new EcuFile(SUB_BOOT_SEQ, 4, CPU2LoaderFileLabel);

		byte[] CAL_SEQ = {(byte) 0x54,(byte) 0xaa};
		cpu1CodeFile = new EcuFile(CAL_SEQ, 160, CPU1CodeLabel);

		cpu2CodeFile = new EcuFile(CAL_SEQ, 160, CPU2CodeLabel);
		
		persistentDataButton = new JButton("Persistent Data File");
		persistentDataButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				int returnVal = fc.showOpenDialog(persistentDataButton);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					validateAndLoadPersistentDataFile(fc.getSelectedFile());
				}
			}
		});
		filesPanel.add(persistentDataButton);
		
		persistentDataLabel = new JLabel("<Unspecified>");
		persistentDataLabel.setHorizontalAlignment(SwingConstants.CENTER);
		filesPanel.add(persistentDataLabel);
	}
	
	
	private void initializeCanMessages()
	{
		/*
		 * define messages, message contents
		 * different messages are allowed for tpu phase and flashloader phase
		 * use vectors to keep track of each set, vector allows easy initialization of combobox
		 */
		
		// commands for tpu phase
		// tpuPhaseMessages contains those messages that are available to user
				
		tpuPhaseMessages.add(can22_e7_25);
		tpuPhaseMessages.add(can22_e7_26);
		tpuPhaseMessages.add(can22_e7_27);
		tpuPhaseMessages.add(can22_e7_28);
		tpuPhaseMessages.add(can22_e7_29);
		tpuPhaseMessages.add(can22_e7_2a);
		tpuPhaseMessages.add(can27_01);
		//
		// also add later phase messages, to allow determination of which phase cpu1 is in
		//
		String msg22_e7_2b = "Get CPU1 flash volts";
		byte[] data22_e7_2b = new byte[]{(byte)0x22,(byte)0xe7,(byte)0x2b};
		CanbusMessage can22_e7_2b = new CanbusMessage(0x20, data22_e7_2b, msg22_e7_2b);

		tpuPhaseMessages.add(can22_e7_2b);
		
		String msg22_e7_2c = "Get CPU2 flash volts";
		byte[] data22_e7_2c = new byte[]{(byte)0x22,(byte)0xe7,(byte)0x2c};
		CanbusMessage can22_e7_2c = new CanbusMessage(0x20, data22_e7_2c, msg22_e7_2c);

		tpuPhaseMessages.add(can22_e7_2c);
		
		tpuPhaseMessages.add(can3f);
						
		DefaultComboBoxModel<CanbusMessage> tpuPhaseMessageCommands = new DefaultComboBoxModel<CanbusMessage>(tpuPhaseMessages);
		
		//use this model to initialize the commandComboBox
		commandComboBox.setModel(tpuPhaseMessageCommands);
		
		// commands for programmer phase
		// programmerPhaseMessages contains those messages that are available to user
				
		String msg20 = "Ping CPU1 with service 0x20";
		byte[] data20 = new byte[]{(byte)0x20};
		CanbusMessage can20 = new CanbusMessage(0x20, data20, msg20);
		programmerPhaseMessages.add(can20);

		// these two messages also supported in programmer phase
		programmerPhaseMessages.add(can22_e7_29);
		programmerPhaseMessages.add(can22_e7_2a);
		
		programmerPhaseMessages.add(can22_e7_2b);
		
		programmerPhaseMessages.add(can22_e7_2c);
		
		programmerPhaseMessages.add(can3f);
		
	}
	
	private void selectSerialPort()
	{
		SerialPort[] availableSerialPorts = SerialPort.getCommPorts();
		boolean portsAvailable = (availableSerialPorts.length != 0);
		
		JPanel panel = new JPanel();
		
		if (portsAvailable)
		{
	        panel.add(new JLabel("Please make a selection:"));
		}
		else
		{
	        panel.add(new JLabel("No ports currently available"));			
		}
		
        DefaultComboBoxModel<SerialPort> model = new DefaultComboBoxModel<SerialPort>(availableSerialPorts);
               
        JComboBox<SerialPort> comboBox = new JComboBox<SerialPort>(model);
        panel.add(comboBox);

        int result = JOptionPane.showConfirmDialog(null, panel, "Port", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        switch (result) {
            case JOptionPane.OK_OPTION:
            	if (portsAvailable)
            	{
            		arduinoPort = (SerialPort) comboBox.getSelectedItem();
            		serialPortLabel.setText(arduinoPort.toString());
            		configureArduinoPort();
            		setupArduinoAndEcu();
            	}
                break;
        }
		
	}
	
	// datalistener class for Serial port, to receive bytes from arduino
	private final class PacketListener implements SerialPortPacketListener
	{
		@Override
	   public int getListeningEvents() 
		{ return (SerialPort.LISTENING_EVENT_DATA_RECEIVED | SerialPort.LISTENING_EVENT_PORT_DISCONNECTED); }
		
		@Override
		   public int getPacketSize() { return 10; }
		
	   @Override
	   public void serialEvent(SerialPortEvent event)
	   {
		   if ((event.getEventType() & SerialPort.LISTENING_EVENT_PORT_DISCONNECTED) != 0)
		   {
			   //remove listener and close port
			   arduinoPort.removeDataListener();
			   arduinoPort.closePort();
			   arduinoDetected = false;
			   ecuBootMode = false;
			   ecuNormalMode = false;
			   serialPortLabel.setText("<Unknown>");
			   arduinoStatusLabel.setText("Disconnected");
			   commandButton.setEnabled(arduinoDetected);
			   ecuStatusLabel.setText("Disconnected");
			   serialSelectButton.setEnabled(true);
		   }
		   else
		   {
			   // read data from serial receive
				CanbusMessage newMessage = new CanbusMessage(event.getReceivedData());
				
				int id = newMessage.getID();
				 
				//filter out ecu heartbeat, and use to detect if ecu is in normal mode
				//if not, set status to normal mode
				if (!ecuNormalMode && (id == 0x7d0))
				{
					ecuBootMode = false;
					ecuNormalMode = true;
					ecuStatusLabel.setText("Inactive - Normal");
				}
				
				//process messages from ecu boot mode
				if ((id == 0x030) || (id == 0x50)  || (id == 0x7ec))
				{
					receiveQueue.add(newMessage);
					//log message provided it is not a 0x36 download
					//if (newMessage.getData()[1] != (byte) 0x36)
					//{
					//	monitorTextArea.append("Receiving can message " + newMessage.toDetailsString() + "\n");	
					//}
				}
				
		   }
	   }
	}
	
	//configuration of serial port
	private void configureArduinoPort()
	{
		executorService1.execute(new Runnable() {
			@Override
			public void run() {
				//configure serial port: 8 bits, no parity, 1 stop bit
				arduinoPort.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
				// open the port, no checking if successful..
				arduinoPort.openPort();
				//configure serial events - fixed 10 byte receive packet, event based
				PacketListener listener = new PacketListener();
				arduinoPort.addDataListener(listener);
				receiveQueue = new ReceiveQueue();
			}
		});
		
		
	}

	private void setExitBehavior()
	{
		//prevent window automatically closing when user clicks close
		 frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		//set listener to detect close of app, optionally prevent it if procedure is on-going
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		    	if (procedureOngoing)
		    	{
		    		if (JOptionPane.showConfirmDialog(frame, 
		    				"Procedure is on-going, are you sure you want to close this window?", "Close Window?", 
		    				JOptionPane.YES_NO_OPTION,
		    				JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
		    					if (arduinoPort.isOpen())
		    					{
		    						arduinoPort.removeDataListener();
		    						arduinoPort.closePort();
		    						executorService1.shutdown();
		    					}
		    					System.exit(0);
		    				}
		    	}
		    	else
		    	{
					System.exit(0);		    		
		    	}
		    }
		});

	}
	
	
	private void setupArduinoAndEcu()
	{
		//this method is called once a Serial port has been selected
		//   see selectSerialPort()

		monitorTextArea.append("Sending arduino ping\n");		
		CanbusResponses reply = canbusRequestAndResponses(5, pingArduino,
				new byte[] {(byte) 0x02, (byte) 0xaa, (byte) 0xaa}, 0x50, null, 1000L);
		printRequestResult(reply);
		
		if (reply.getResult())
		{
			monitorTextArea.append("Received arduino ping response\n");
			arduinoDetected = true;
			//enable commands
			arduinoStatusLabel.setText("Connected");
			serialSelectButton.setEnabled(false);
		}
		
		//delay to check for ecu heartbeat in normal mode
		long start = System.currentTimeMillis();
		long end = start + 1000;
		
		while (System.currentTimeMillis() < end) {};
						
		//see if ecu is in normal mode
		if (arduinoDetected && (ecuNormalMode == true))
		{
			savePersistentButton.setEnabled(true);
			saveRAMButton.setEnabled(true);
		}
		
		//if not in normal mode, see if ecu is in boot mode
		if (arduinoDetected && !ecuNormalMode)
		{
			monitorTextArea.append("Sending ecu ping\n");			
			reply = canbusRequestAndResponses(5, pingECU,
					new byte[] {(byte) 0x07, (byte) 0x62, (byte) 0xe7, (byte) 0x25}, 0x30, null, 1000L);
			printRequestResult(reply);
			
			if (reply.getResult())
			{
				monitorTextArea.append("Received ecu boot mode ping response\n");
				ecuBootMode = true;
				ecuNormalMode = false;
				//enable commands
				ecuStatusLabel.setText("Active");
				commandButton.setEnabled(true);
				//retrieve eeprom signatures
				retrieveEepromSignatures();
			}
		}

	}
	
	private void validateUploadProgrammersReady()
	{
		if (programmer1.isValid() && programmer2.isValid())
		{
			//enable upload programmers button
			programmerButton.setEnabled(true);
		}
	}
	
	private void validateCodeFilesReady()
	{		
		if (cpu1CodeFile.isValid() && cpu2CodeFile.isValid())
		{
			//enable upload programmers button
			reflashButton.setEnabled(true);
		}
	}
	
	private void unlockExtendedCommands()
	{
		
		monitorTextArea.append("Sending "+can27_01.toDetailsString()+"\n");			
		CanbusResponses reply = canbusRequestAndResponses(1, can27_01,
				new byte[] {(byte) 0x03, (byte) 0x67, (byte) 0x02, (byte) 0x34}, 0x30, null, 1000L);
		printRequestResult(reply);
		
		if (reply.getResult())
		{
			//unlockSuccess = true;
		}

	}
	
	
	private void updateCommandsSelection()
	{
		DefaultComboBoxModel<CanbusMessage> programmerPhaseMessageCommands = new DefaultComboBoxModel<CanbusMessage>(programmerPhaseMessages);
		
		//use this model to initialize the commandComboBox
		commandComboBox.setModel(programmerPhaseMessageCommands);
	}
	
	private CanbusResponses canbusRequestAndResponses(int attempts, CanbusMessage requestMessage, byte[] responseVector, int responseID,
			byte[] suppressVector, long waitTime)
	{
		List<CanbusMessage> responses = new ArrayList<CanbusMessage>();

		//returns expected response as List of Bytes, or null if incorrect response
		int iterations = attempts;
		byte[] msgResponse = new byte[8];
		boolean correctResponse = false;
		boolean successResult = false;
		CanbusMessage targetResponse = null;
				
		//send request message
		//if no response received, exception will be triggered and iterations decremented
		//if response is received, and matches expected reply, true is returned
		while ((iterations > 0) && !correctResponse)
		{
			// set a timeout
			long start = System.currentTimeMillis();
			long end = start + waitTime;
			boolean timeOut = false;
					
			//send message
			requestMessage.sendMessage();
												
			//get correct response or timeout
			while ( !correctResponse && !timeOut)
			{
				if (receiveQueue.size() != 0)
				{
					CanbusMessage rxMessage = receiveQueue.getFirstElement();
					//can remove message from real queue
					receiveQueue.removeFirstElement();
					boolean suppress = true;
					if (rxMessage.getID() == responseID)
					{
						msgResponse = rxMessage.getData();
						int responseCount = responseVector.length;
						correctResponse = true;
						for (int x=0;x<responseCount;x++)
						{
							correctResponse = correctResponse && ( msgResponse[x] == responseVector[x]);
						}
						
						if (correctResponse)
						{
							targetResponse = rxMessage;
						}
						
						if (suppressVector != null)
						{
							int suppressCount = suppressVector.length;
							for (int x=0;x<suppressCount;x++)
							{
								suppress = suppress && ( msgResponse[x] == suppressVector[x]);
							}
						}
						else
						{
							suppress = false;
						}
					}
					if (!suppress)
					{
						responses.add(rxMessage);
					}
				}
				//check for timeout
				if (System.currentTimeMillis() > end)
				{
					timeOut = true;
				}
			}
					
			//if timeout, decrement iterations and try again
			if (!correctResponse)
			{
				iterations--;
			}
			else
			{
				successResult = true;
			}
		}
		
		return new CanbusResponses(successResult, targetResponse, responses);
	}	
	
	private void retrieveEepromSignatures()
	{
		int[] eeprom1Signature = new int[5];
		int[] eeprom2Signature = new int[5];
		int eeprom1EraseCount = 0;
		int eeprom2EraseCount = 0;
		
		/*
		byte[] response = canbusRequestAndResponse(1, can22_e7_25,
			new byte[] {(byte) 0x07, (byte) 0x62, (byte) 0xe7, (byte) 0x25}, 1000L, false);
		
		if (response != null)
		{
			for (int x=0;x<4;x++)
			{
				eeprom1Signature[x] = response[x+4] & 0xff;
			}
		}
		*/
		
		monitorTextArea.append("Sending "+can22_e7_25.toDetailsString()+" , response expected\n");
		CanbusResponses cr = canbusRequestAndResponses(1, can22_e7_25,
				new byte[] {(byte) 0x07, (byte) 0x62, (byte) 0xe7, (byte) 0x25}, 0x30, null, 500L);
		if (cr.getResult())
		{
			byte[] response = cr.getTargetResponse().getData();
			for (int x=0;x<4;x++)
			{
				eeprom1Signature[x] = response[x+4] & 0xff;
			}
		}

		/*
		response = canbusRequestAndResponse(1, can22_e7_26,
				new byte[] {(byte) 0x04, (byte) 0x62, (byte) 0xe7, (byte) 0x26}, 1000L, false);

		if (response != null)
		{
			eeprom1Signature[4] = response[4] & 0xff;
		}
		*/
		
		monitorTextArea.append("Sending "+can22_e7_26.toDetailsString()+" , response expected\n");
		cr = canbusRequestAndResponses(1, can22_e7_26,
				new byte[] {(byte) 0x04, (byte) 0x62, (byte) 0xe7, (byte) 0x26}, 0x30, null, 500L);
		if (cr.getResult())
		{
			eeprom1Signature[4] = cr.getTargetResponse().getData()[4] & 0xff;
		}

		/*
		response = canbusRequestAndResponse(1, can22_e7_27,
				new byte[] {(byte) 0x07, (byte) 0x62, (byte) 0xe7, (byte) 0x27}, 1000L, false);
			
		if (response != null)
		{
			for (int x=0;x<4;x++)
			{
				eeprom2Signature[x] = response[x+4] & 0xff;
			}
		}
		*/
		
		monitorTextArea.append("Sending "+can22_e7_27.toDetailsString()+" , response expected\n");
		cr = canbusRequestAndResponses(1, can22_e7_27,
				new byte[] {(byte) 0x07, (byte) 0x62, (byte) 0xe7, (byte) 0x27}, 0x30, null, 500L);
		if (cr.getResult())
		{
			byte[] response = cr.getTargetResponse().getData();
			for (int x=0;x<4;x++)
			{
				eeprom2Signature[x] = response[x+4] & 0xff;
			}
		}
		
		/*
		response = canbusRequestAndResponse(1, can22_e7_28,
				new byte[] {(byte) 0x04, (byte) 0x62, (byte) 0xe7, (byte) 0x28}, 1000L, false);

		if (response != null)
		{
			eeprom2Signature[4] = response[4] & 0xff;
		}
		*/
		
		monitorTextArea.append("Sending "+can22_e7_28.toDetailsString()+" , response expected\n");
		cr = canbusRequestAndResponses(1, can22_e7_28,
				new byte[] {(byte) 0x04, (byte) 0x62, (byte) 0xe7, (byte) 0x28}, 0x30, null, 500L);
		if (cr.getResult())
		{
			eeprom2Signature[4] = cr.getTargetResponse().getData()[4] & 0xff;
		}

		/*
		response = canbusRequestAndResponse(1, can22_e7_29,
				new byte[] {(byte) 0x04, (byte) 0x62, (byte) 0xe7, (byte) 0x29}, 1000L, false);

		if (response != null)
		{
			eeprom1EraseCount = response[4] & 0xff;
		}
		*/

		monitorTextArea.append("Sending "+can22_e7_29.toDetailsString()+" , response expected\n");
		cr = canbusRequestAndResponses(1, can22_e7_29,
				new byte[] {(byte) 0x04, (byte) 0x62, (byte) 0xe7, (byte) 0x29}, 0x30, null, 500L);
		if (cr.getResult())
		{
			eeprom1EraseCount = cr.getTargetResponse().getData()[4] & 0xff;
		}

		/*
		response = canbusRequestAndResponse(1, can22_e7_2a,
				new byte[] {(byte) 0x04, (byte) 0x62, (byte) 0xe7, (byte) 0x2a}, 1000L, false);

		if (response != null)
		{
			eeprom2EraseCount = response[4] & 0xff;
		}
		*/
		
		monitorTextArea.append("Sending "+can22_e7_2a.toDetailsString()+" , response expected\n");
		cr = canbusRequestAndResponses(1, can22_e7_2a,
				new byte[] {(byte) 0x04, (byte) 0x62, (byte) 0xe7, (byte) 0x2a}, 0x30, null, 500L);
		if (cr.getResult())
		{
			eeprom2EraseCount = cr.getTargetResponse().getData()[4] & 0xff;
		}

		
		String s = String.format("%2x %2x %2x %2x %2x Erase count %2x",
				eeprom1Signature[0], eeprom1Signature[1],eeprom1Signature[2],
				eeprom1Signature[3],eeprom1Signature[4],eeprom1EraseCount);

		eeprom1SigLabel.setText(s);

		s = String.format("%2x %2x %2x %2x %2x Erase count %2x",
				eeprom2Signature[0], eeprom2Signature[1],eeprom2Signature[2],
				eeprom2Signature[3],eeprom2Signature[4],eeprom2EraseCount);

		eeprom2SigLabel.setText(s);

	}

	private void uploadProgrammers()
	{
		// unlock extended commands
		unlockExtendedCommands();
		
		SwingWorker<Integer, String> uploadProgs = new SwingWorker<Integer, String>()
		{
			
			private void printRequestResult(CanbusResponses cr)
			{
				Iterator<CanbusMessage> it = cr.getResponses().iterator();						
				while (it.hasNext())
				{
					CanbusMessage m = (it.next());
					publish( m.toDetailsString());
				}
				String result = "Result is ";
				if (cr.getResult())
				{
					result = result.concat("true");
				}
				else
				{
					result = result.concat("false");
				}
				publish(result);
			}
			
			@Override
			protected Integer doInBackground()
			{
				int resultValue = 0;

				try
				{	
					// *******************
					// upload programmer 1
					// *******************
										
					//partial array for uploading data
					byte[] uploadArray = new byte[]{(byte) 0x02, (byte) 0x00, (byte) 0x07, (byte) 0x36,
							(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
							
					//compute the checksum for programmer1
					int blocks = programmer1.getNumberOfBlocks();
					int checksum = 0;
					
					for (int blockCount=0;blockCount<blocks;blockCount++)
					{
						//get 1k bytes data block
						byte[] data = programmer1.getDataBlock(blockCount);
						//compute checksum over this 512 words
						for (int x=0;x<512;x++)
						{
							int memWord = ((data[x*2] & 0xff) << 8) + ((data[(x*2)+1] & 0xff));
							checksum = (checksum + memWord) & 0xffff;
						}
					}
					
					byte checksumHighByte = (byte) ((checksum & 0xff00) >> 8);
					byte checksumLowByte = (byte) (checksum & 0xff);
					
					// procedure to upload a programmer is:
					// send number of blocks to be uploaded to ecu
					//		31 a0 ... blocks
					// for each 1k block
					//		send target address 34 xx ... address  to ecu
					//		send get ready message to arduino  34 00
					//		send 170 upload packets to arduino  36
					//      send upload to ecu message to arduino  31 a2/b2
					//		arduino will upload data and then send 31 a2/b2 to ecu
					//		send clear target address to ecu
					//		send block upload complete to ecu   32 a2/b2
					// send checksum to ecu  31 a5/b5
					// send validate checksum to ecu 32 a5/b5
					
					//send load 5 RAM blocks commmand to cpu1
					//can31_a0 - 06 31 a0 00 05 00 01
					// 0xa0 is cpu1, 0x00 0x05 is number of blocks (0x0005 word)
					String msg31_a0 = "Load 5 RAM blocks to CPU1";
					byte[] data31_a0 = new byte[]{(byte)0x31,(byte)0xa0,(byte)0x00, (byte)0x05,(byte)0x00, (byte)0x01};
					CanbusMessage can31_a0 = new CanbusMessage(0x20, data31_a0, msg31_a0);

					publish("Sending "+can31_a0.toDetailsString());
					CanbusResponses reply = canbusRequestAndResponses(1, can31_a0, new byte[] {(byte) 0x06, (byte) 0x7f,
							(byte) 0x31, (byte) 0xa0, (byte) 0x00, (byte) 0x05, (byte) 0x00}, 0x30, null, 500L);
					//expect response 06 7f 31 a0 00 05 00
					printRequestResult(reply);
					
					//publish("Received "+byteArraytoString(reply));
					
					//upload the 5 ram blocks
					for (int blockCount = 0;blockCount<5;blockCount++)
					{
						CanbusMessage loadMessage = null;
						byte[] response34 = null;
						//load a 1k block to target RAM address
						switch(blockCount)
						{
							case 0:
								String msg34_80_00 = "Load 1k block to CPU1 b0000";
								byte[] data34_80_00 = new byte[]{(byte)0x34,(byte)0x80,(byte)0x04, (byte)0x00, (byte)0x0b,(byte)0x00, (byte) 0x00};
								response34 = new byte[] {(byte) 0x06, (byte) 0x7f,
										(byte) 0x34, (byte) 0x80, (byte) 0x04, (byte) 0x00, (byte) 0x00};
								loadMessage = new CanbusMessage(0x20, data34_80_00, msg34_80_00);
								break;
								//expect response 06 7f 34 80 04 00 00
							case 1:
								String msg34_80_04 = "Load 1k block to CPU1 b0400";
								byte[] data34_80_04 = new byte[]{(byte)0x34,(byte)0x80,(byte)0x04, (byte)0x00, (byte)0x0b,(byte)0x04, (byte) 0x00};
								response34 = new byte[] {(byte) 0x06, (byte) 0x7f,
										(byte) 0x34, (byte) 0x80, (byte) 0x04, (byte) 0x00, (byte) 0x00};
								loadMessage = new CanbusMessage(0x20, data34_80_04, msg34_80_04);
								//expect response 06 7f 34 80 04 00 00
								break;
							case 2:
								String msg34_80_08 = "Load 1k block to CPU1 b0800";
								byte[] data34_80_08 = new byte[]{(byte)0x34,(byte)0x80,(byte)0x04, (byte)0x00, (byte)0x0b,(byte)0x08, (byte) 0x00};
								response34 = new byte[] {(byte) 0x06, (byte) 0x7f,
										(byte) 0x34, (byte) 0x80, (byte) 0x04, (byte) 0x00, (byte) 0x00};
								loadMessage = new CanbusMessage(0x20, data34_80_08, msg34_80_08);
								//expect response 06 7f 34 80 04 00 00
								break;
							case 3:
								String msg34_80_0c = "Load 1k block to CPU1 b0c00";
								byte[] data34_80_0c = new byte[]{(byte)0x34,(byte)0x80,(byte)0x04, (byte)0x00, (byte)0x0b,(byte)0x0c, (byte) 0x00};
								response34 = new byte[] {(byte) 0x06, (byte) 0x7f,
										(byte) 0x34, (byte) 0x80, (byte) 0x04, (byte) 0x00, (byte) 0x00};
								loadMessage = new CanbusMessage(0x20, data34_80_0c, msg34_80_0c);
								//expect response 06 7f 34 80 04 00 00
								break;
							case 4:
								String msg34_80_10 = "Load 1k block to CPU1 b1000";
								byte[] data34_80_10 = new byte[]{(byte)0x34,(byte)0x80,(byte)0x04, (byte)0x00, (byte)0x0b,(byte)0x10, (byte) 0x00};
								response34 = new byte[] {(byte) 0x06, (byte) 0x7f,
										(byte) 0x34, (byte) 0x80, (byte) 0x04, (byte) 0x00, (byte) 0x00};
								loadMessage = new CanbusMessage(0x20, data34_80_10, msg34_80_10);
								//expect response 06 7f 34 80 04 00 00
								break;	
						}
						
						reply = canbusRequestAndResponses(1, loadMessage, response34, 0x30, null, 500L);
						printRequestResult(reply);
						
						//retrieve 1k data block
						byte[] uploadBlock = programmer1.getDataBlock(blockCount);
						
						//signal to arduino start of upload 1k block  SID 0x10  02 34 00
						publish("Sending "+arduinoUploadStart.toDetailsString());
						reply = canbusRequestAndResponses(1, arduinoUploadStart, new byte[] {(byte) 0x03, (byte) 0x7f,
								(byte) 0x34, (byte) 0x00}, 0x50, null, 500L);
						printRequestResult(reply);
						//enables upload flag, zeros packet count and byte count
												
						//send 170*6 data bytes
						//each send increments packet count and byte count + 6
						//meter the send rate to avoid overloading the serial port
												
						for (int x=0;x<170;x++)
						{								
							//send upload packet
							int offset = x*6;
							System.arraycopy(uploadBlock, offset, uploadArray, 4, 6);
							CanbusMessage data = new CanbusMessage(uploadArray);
							data.sendMessage(); // no print to monitor
							
							try
							{
								Thread.sleep(1); //pause 1 ms to flow control the send rate
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
						}
						
						//send last 4 bytes
						
						int offset = 1020;
						System.arraycopy(uploadBlock, offset, uploadArray, 4, 4);
						uploadArray[8] = (byte) 0x00;
						uploadArray[9] = (byte) 0x00;
						CanbusMessage data = new CanbusMessage(uploadArray);
						data.sendMessage();
						
						
						//validate arduino received all the data SID 0x10 02 31 a2
						publish("Sending "+arduinoUploadEnd1.toDetailsString());
						
						byte[] testVector = new byte[] {(byte) 0x06, (byte) 0x7f, (byte) 0x32, (byte) 0xa2,
								(byte) 0x00, (byte) 0x00, (byte) 0x00};
						reply = canbusRequestAndResponses(1, arduinoUploadEnd1, testVector, 0x30, null, 500L);
						printRequestResult(reply);
						
						//expect acknowledgement 03 31 a2 xx where xx is the packet count (sets of 6 bytes)
						//expect subsequent response after upload is complete of:
						// success - 03 7f 31 a2
						// fail - no response
					
						//validate block sent - 02 31 a2 - is sent by arduino otherwise ecu will timeout
						//expect response 06 7f 31 a2 00 00 00
												
						//update progress bar with value 0 - 40
						setProgress((blockCount)*10);
						 
					}
					
					//upload expected checksum and compute actual uploaded checksum
					//checksum is 16 bit sum of 0x0a00 words from address b0000 - b13fe/i.e. the 5 1k blocks uploaded
					// 04 31 a5 pp qq
					String msg31_a5 = "Upload and compute checksum for programmer1";
					byte[] data31_a5 = new byte[]{(byte)0x31,(byte)0xa5, checksumHighByte, checksumLowByte};
					CanbusMessage can31_a5 = new CanbusMessage(0x20, data31_a5, msg31_a5);
					publish("Sending "+can31_a5.toDetailsString());
					reply = canbusRequestAndResponses(1, can31_a5, new byte[] {(byte) 0x06, (byte) 0x7f,
							(byte) 0x31, (byte) 0xa5, checksumHighByte, checksumLowByte, (byte) 0x00}, 0x30, null, 500L);
					printRequestResult(reply);

					//expect response 06 7f 31 a5 pp qq 00		
					
					//verify checksum and transfer execution to programmer1
					// 02 32 a5 
					publish("Sending "+can32_a5.toDetailsString());

					reply = canbusRequestAndResponses(2, can32_a5, new byte[] {(byte) 0x06, (byte) 0x7f, (byte) 0x32,
							(byte) 0xa5, (byte) 0x00, (byte) 0x00, (byte) 0x00}, 0x30, null, 500L);
					printRequestResult(reply);

					// success response 06 7f 32 a5 00 00 00
					// failure response 06 7f 32 a5 00 5b 63
					
					if (reply.getResult())
					{	
						//programmer1 uploaded successfully
						resultValue = 1;
						//update progress bar with value 50
						setProgress(50);

					}
					else
					{
						resultValue = 0;
						//programmer1 not uploaded
					}
					
					// *******************
					// upload programmer 2
					// *******************
					
					
					//compute the checksum for programmer2
					blocks = programmer2.getNumberOfBlocks();
					checksum = 0;
					
					for (int blockCount=0;blockCount<blocks;blockCount++)
					{
						//get 1k bytes data block
						byte[] data = programmer2.getDataBlock(blockCount);
						//compute checksum over this 512 words
						for (int x=0;x<512;x++)
						{
							int memWord = ((data[x*2] & 0xff) << 8) + ((data[(x*2)+1] & 0xff));
							checksum = (checksum + memWord) & 0xffff;
						}
					}
					
					checksumHighByte = (byte) ((checksum & 0xff00) >> 8);
					checksumLowByte = (byte) (checksum & 0xff);
					
					//send load 4 RAM blocks commmand to cpu2
					//can31_b0 - 06 31 b0 00 04 00 01
					//can31_b0 - 06 31 b0 00 04 00 01
					String msg31_b0 = "Load 4 RAM blocks to CPU2";
					byte[] data31_b0 = new byte[]{(byte)0x31,(byte)0xb0,(byte)0x00, (byte)0x04,(byte)0x00, (byte)0x01};
					CanbusMessage can31_b0 = new CanbusMessage(0x20, data31_b0, msg31_b0);
					
					publish("Sending "+can31_b0.toDetailsString());
					reply = canbusRequestAndResponses(1, can31_b0, new byte[] {(byte) 0x06, (byte) 0x7f,
							(byte) 0x31, (byte) 0xb0, (byte) 0x00, (byte) 0x04, (byte) 0x00}, 0x30, null, 500L);
					//expect response 06 7f 31 b0 00 04 00
					printRequestResult(reply);
					
					//upload the 4 ram blocks
					for (int blockCount = 0;blockCount<4;blockCount++)
					{
						//load a 1k block to target RAM address
						CanbusMessage loadMessage = null;
						byte[] response34 = null;

						switch(blockCount)
						{
							case 0:
								//can34_00_00 - 07 34 00 04 00 0b 00 00
								// 0x00 is CPU2, 0x04 0x00 is 1k bytes (0x0400 word), 0x0b 0x00 0x00 is 20 bit address (0x0b0000) 
								String msg34_00_00 = "Load 1k block to CPU2 b0000";
								byte[] data34_00_00 = new byte[]{(byte)0x34,(byte)0x00,(byte)0x04, (byte)0x00, (byte)0x0b,(byte)0x00, (byte) 0x00};
								response34 = new byte[] {(byte) 0x06, (byte) 0x7f,
										(byte) 0x34, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00};
								loadMessage = new CanbusMessage(0x20, data34_00_00, msg34_00_00);
								
								break;
								//expect response 06 7f 34 00 04 00 00
							case 1:
								//can34_00_04 - 07 34 00 04 00 0b 04 00
								String msg34_00_04 = "Load 1k block to CPU2 b0400";
								byte[] data34_00_04 = new byte[]{(byte)0x34,(byte)0x00,(byte)0x04, (byte)0x00, (byte)0x0b,(byte)0x04, (byte) 0x00};
								response34 = new byte[] {(byte) 0x06, (byte) 0x7f,
										(byte) 0x34, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00};
								loadMessage = new CanbusMessage(0x20, data34_00_04, msg34_00_04);
								//expect response 06 7f 34 00 04 00 00
								break;
							case 2:
								//can34_00_08 - 07 34 00 04 00 0b 08 00
								String msg34_00_08 = "Load 1k block to CPU2 b0800";
								byte[] data34_00_08 = new byte[]{(byte)0x34,(byte)0x00,(byte)0x04, (byte)0x00, (byte)0x0b,(byte)0x08, (byte) 0x00};
								response34 = new byte[] {(byte) 0x06, (byte) 0x7f,
										(byte) 0x34, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00};
								loadMessage = new CanbusMessage(0x20, data34_00_08, msg34_00_08);
								//expect response 06 7f 34 00 04 00 00
								break;
							case 3:
								//can34_00_0c - 07 34 00 04 00 0b 0c 00
								String msg34_00_0c = "Load 1k block to CPU2 b0c00";
								byte[] data34_00_0c = new byte[]{(byte)0x34,(byte)0x00,(byte)0x04, (byte)0x00, (byte)0x0b,(byte)0x0c, (byte) 0x00};
								response34 = new byte[] {(byte) 0x06, (byte) 0x7f,
										(byte) 0x34, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00};
								loadMessage = new CanbusMessage(0x20, data34_00_0c, msg34_00_0c);
								//expect response 06 7f 34 00 04 00 00
								break;
						}
	
						reply = canbusRequestAndResponses(1, loadMessage, response34, 0x30, null, 1000L);
						printRequestResult(reply);

						//retrieve 1k data block
						byte[] uploadBlock = programmer2.getDataBlock(blockCount);
						
						//signal to arduino start of upload 1k block  SID 0x10  02 34 00
						publish("Sending "+arduinoUploadStart.toDetailsString());
						reply = canbusRequestAndResponses(1, arduinoUploadStart, new byte[] {(byte) 0x03, (byte) 0x7f,
								(byte) 0x34, (byte) 0x00}, 0x50, null, 500L);
						printRequestResult(reply);						
						//expect response 03 7f 34 00
						//enables upload flag, zeros packet count and byte count
						
						//send 170*6 data bytes
						//each send increments packet count and byte count + 6
						//meter the send rate to avoid overloading the serial port
												
						for (int x=0;x<170;x++)
						{							
							//send upload packet
							int offset = x*6;
							System.arraycopy(uploadBlock, offset, uploadArray, 4, 6);
							CanbusMessage data = new CanbusMessage(uploadArray);
							data.sendMessage(); // no print to monitor
							
							try
							{
								Thread.sleep(1); //pause 1 ms to flow control the send rate
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}

						}
						
						//send last 4 bytes
						
						int offset = 1020;
						System.arraycopy(uploadBlock, offset, uploadArray, 4, 4);
						uploadArray[8] = (byte) 0x00;
						uploadArray[9] = (byte) 0x00;
						CanbusMessage data = new CanbusMessage(uploadArray);
						data.sendMessage();
						
						
						//validate arduino received all the data SID 0x10 02 31 b2
						publish("Sending "+arduinoUploadEnd2.toDetailsString());
						
						byte[] testVector = new byte[] {(byte) 0x06, (byte) 0x7f, (byte) 0x32, (byte) 0xb2,
								(byte) 0x00, (byte) 0x00, (byte) 0x00};
						reply = canbusRequestAndResponses(1, arduinoUploadEnd2, testVector, 0x30, null, 500L);
						printRequestResult(reply);

						
						//expect acknowledgement 03 31 b2 xx where xx is the packet count (sets of 6 bytes)
						//expect subsequent response after upload is complete of:
						// success - 03 7f 31 b2
						// fail - no response
					
						//validate block sent - 02 31 b2 - is sent by arduino otherwise ecu will timeout
						//expect response 06 7f 31 b2 00 00 00
												
						//update progress bar with value 60 - 90
						setProgress(50 + (blockCount+1)*10);
						 
					}
					
					//upload expected checksum and compute actual uploaded checksum
					//checksum is 16 bit sum of words from address b0000 - b0fff/i.e. the 4 1k blocks uploaded
					// 04 31 b5 pp qq
					String msg31_b5 = "Upload and compute checksum for programmer2";
					byte[] data31_b5 = new byte[]{(byte)0x31,(byte)0xb5, checksumHighByte, checksumLowByte};
					CanbusMessage can31_b5 = new CanbusMessage(0x20, data31_b5, msg31_b5);
					publish("Sending "+can31_b5.toDetailsString());
					reply = canbusRequestAndResponses(1, can31_b5, new byte[] {(byte) 0x06, (byte) 0x7f,
							(byte) 0x31, (byte) 0xb5, checksumHighByte, checksumLowByte, (byte) 0x00}, 0x30, null, 500L);
					printRequestResult(reply);
					//expect response 06 7f 31 b5 pp qq 00		
					
					//verify checksum and transfer execution to programmer2
					// 02 32 b5 
					publish("Sending "+can32_b5.toDetailsString());
					reply = canbusRequestAndResponses(2, can32_b5, new byte[] {(byte) 0x06, (byte) 0x7f, (byte) 0x32,
							(byte) 0xb5, (byte) 0x00, (byte) 0x00, (byte) 0x00}, 0x30, null, 500L);
					printRequestResult(reply);
					// success response 06 7f 32 b5 00 00 00
					// failure response 06 7f 32 b5 00 5b 63
					// pending response 06 7f 32 b5 00 00 21
					
					if (reply.getResult())
					{	
						//programmer 2 successfully uploaded
						resultValue += 2;
						//update progress bar with value 100
						setProgress(100);

					}
					else
					{
						//programmer2 not uploaded
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				};
				
				return Integer.valueOf(resultValue);
			}
			
			@Override
			protected void process(List<String> progressMessages)
			{
				Iterator<String> it = progressMessages.iterator();
				while (it.hasNext())
				{
					monitorTextArea.append(it.next()+"\n");		
				}
			}
			
			@Override
			protected void done()
			{
				int result = 0;
				
				try
				{
					Integer resultInteger = get();
					result = resultInteger.intValue();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				
				switch (result)
				{
				case 0:
					programmersLoaded = false;
					//disable upload programmers button since failed
					programmerButton.setEnabled(false);
					//disable select programmers file buttons since failed
					CPU1LoaderButton.setEnabled(false);
					CPU2LoaderButton.setEnabled(false);
					break;
				case 1:
					//update label to show programmer1 uploaded and active
					mainCPUStatusLabel.setText("Loaded and active");
					programmersLoaded = false;
					programmerButton.setEnabled(false);
					//disable select programmers file buttons since failed
					CPU1LoaderButton.setEnabled(false);
					CPU2LoaderButton.setEnabled(false);
					break;
				case 2:
					//update label to show programmer2 uploaded and active
					secondCPUStatusLabel.setText("Loaded and active");
					programmersLoaded = false;
					programmerButton.setEnabled(false);
					//disable select programmers file buttons since failed
					CPU1LoaderButton.setEnabled(false);
					CPU2LoaderButton.setEnabled(false);
					break;
				case 3:
					//update label to show programmer1 uploaded and active
					mainCPUStatusLabel.setText("Loaded and active");
					//update label to show programmer2 uploaded and active
					secondCPUStatusLabel.setText("Loaded and active");
					programmersLoaded = true;
					// enable eeprom download button since both programmers are loaded
					eepromdownloadButton.setEnabled(true);
					//disable upload programmers button since now loaded
					programmerButton.setEnabled(false);
					//disable select programmers file buttons since now loaded
					CPU1LoaderButton.setEnabled(false);
					CPU2LoaderButton.setEnabled(false);
					break;
				}
				
				monitorTextArea.append("Finished upload attempt\n");		
			}
		};
		
		// add listener to update progress bar
		uploadProgs.addPropertyChangeListener(new PropertyChangeListener() {
	        @Override
	        public void propertyChange(PropertyChangeEvent e) {
	            if ("progress".equals(e.getPropertyName())) {
	                uploadProgressBar.setValue((Integer) e.getNewValue());
	            }
	        }
	    });
		
		//execute upload
		uploadProgs.execute();
	}
	
	
	
	// retrieve 13 eprom3 bytes: 0, 2, 4, 6, 8, a, c, e, 10, 12, 14, 16, 18
	private byte[] retrieveEeprom3Data()
	{
		byte[] allData = new byte[29];
		
		int[] eeprom3Data = new int[13];
		int[] eeprom3aData = new int[16];
		
		CanbusMessage eeprom3_0 = new CanbusMessage(0x7e8, new byte[] {(byte) 0x22, (byte) 0xe7, (byte) 0x23}, "Get eprom3 0x00-05");
		//monitorTextArea.append("Sending "+eeprom3_0.toDetailsString()+" , response expected\n");
		CanbusResponses cr = canbusRequestAndResponses(3, eeprom3_0,
				new byte[] {(byte) 0x06, (byte) 0x62, (byte) 0xe7, (byte) 0x23}, 0x7ec, null, 100L);
		if (cr.getResult())
		{
			byte[] response = cr.getTargetResponse().getData();
			for (int x=0;x<3;x++)
			{
				eeprom3Data[x] = response[x+4] & 0xff;
			}
		}
		//printRequestResult(cr);
				
		CanbusMessage eeprom3_1 = new CanbusMessage(0x7e8, new byte[] {(byte) 0x22, (byte) 0xe7, (byte) 0x24}, "Get eprom3 0x06-0d");
		//monitorTextArea.append("Sending "+eeprom3_1.toDetailsString()+" , response expected\n");
		cr = canbusRequestAndResponses(3, eeprom3_1,
				new byte[] {(byte) 0x07, (byte) 0x62, (byte) 0xe7, (byte) 0x24}, 0x7ec, null, 100L);
		if (cr.getResult())
		{
			byte[] response = cr.getTargetResponse().getData();
			for (int x=0;x<4;x++)
			{
				eeprom3Data[x+3] = response[x+4] & 0xff;
			}
		}
		//printRequestResult(cr);
		
		//display result
		String s = String.format("%2x %2x %2x %2x %2x %2x %2x",
				eeprom3Data[0], eeprom3Data[1], eeprom3Data[2],
				eeprom3Data[3], eeprom3Data[4], eeprom3Data[5], eeprom3Data[6]);
		
		monitorTextArea.append("eeprom3 data 0-0d: " + s + "\n");
				
		CanbusMessage eeprom3_2 = new CanbusMessage(0x7e8, new byte[] {(byte) 0x22, (byte) 0xe7, (byte) 0x18}, "Get eprom3 0x0e-13");
		//monitorTextArea.append("Sending "+eeprom3_2.toDetailsString()+" , response expected\n");
		cr = canbusRequestAndResponses(3, eeprom3_2,
				new byte[] {(byte) 0x06, (byte) 0x62, (byte) 0xe7, (byte) 0x18}, 0x7ec, null, 100L);
		if (cr.getResult())
		{
			byte[] response = cr.getTargetResponse().getData();
			for (int x=0;x<3;x++)
			{
				eeprom3Data[x+7] = response[x+4] & 0xff;
			}
		}
		//printRequestResult(cr);
		
		CanbusMessage eeprom3_3 = new CanbusMessage(0x7e8, new byte[] {(byte) 0x22, (byte) 0x12, (byte) 0x41}, "Get eprom3 0x14-17");
		//monitorTextArea.append("Sending "+eeprom3_3.toDetailsString()+" , response expected\n");
		cr = canbusRequestAndResponses(3, eeprom3_3,
				new byte[] {(byte) 0x05, (byte) 0x62, (byte) 0x12, (byte) 0x41}, 0x7ec, null, 100L);
		if (cr.getResult())
		{
			byte[] response = cr.getTargetResponse().getData();
			for (int x=0;x<2;x++)
			{
				eeprom3Data[x+10] = response[x+4] & 0xff;
			}
		}
		//printRequestResult(cr);
		
		CanbusMessage eeprom3_4 = new CanbusMessage(0x7e8, new byte[] {(byte) 0x22, (byte) 0xe7, (byte) 0x08}, "Get eprom3 0x18-19");
		//monitorTextArea.append("Sending "+eeprom3_4.toDetailsString()+" , response expected\n");
		cr = canbusRequestAndResponses(3, eeprom3_4,
				new byte[] {(byte) 0x04, (byte) 0x62, (byte) 0xe7, (byte) 0x08}, 0x7ec, null, 100L);
		if (cr.getResult())
		{
			byte[] response = cr.getTargetResponse().getData();
			for (int x=0;x<1;x++)
			{
				eeprom3Data[x+12] = response[x+4] & 0xff;
			}
		}
		//printRequestResult(cr);
		
		//display result
		s = String.format("%2x %2x %2x %2x %2x %2x",
				eeprom3Data[7], eeprom3Data[8], eeprom3Data[9],
				eeprom3Data[10], eeprom3Data[11], eeprom3Data[12]);
		
		monitorTextArea.append("eeprom3 data 0e-19: " + s + "\n");
				
		CanbusMessage eeprom3_5 = new CanbusMessage(0x7e8, new byte[] {(byte) 0x22, (byte) 0xe7, (byte) 0x02}, "Get eprom3 0x1a-1d");
		//monitorTextArea.append("Sending "+eeprom3_5.toDetailsString()+" , response expected\n");
		cr = canbusRequestAndResponses(3, eeprom3_5,
				new byte[] {(byte) 0x07, (byte) 0x62, (byte) 0xe7, (byte) 0x02}, 0x7ec, null, 100L);
		if (cr.getResult())
		{
			byte[] response = cr.getTargetResponse().getData();
			for (int x=0;x<4;x++)
			{
				eeprom3aData[x] = response[x+4] & 0xff;
			}
		}
		//printRequestResult(cr);
		
		CanbusMessage eeprom3_6 = new CanbusMessage(0x7e8, new byte[] {(byte) 0x22, (byte) 0xe7, (byte) 0x01}, "Get eprom3 0x1e-21");
		//monitorTextArea.append("Sending "+eeprom3_6.toDetailsString()+" , response expected\n");
		cr = canbusRequestAndResponses(3, eeprom3_6,
				new byte[] {(byte) 0x07, (byte) 0x62, (byte) 0xe7, (byte) 0x01}, 0x7ec, null, 100L);
		if (cr.getResult())
		{
			byte[] response = cr.getTargetResponse().getData();
			for (int x=0;x<4;x++)
			{
				eeprom3aData[x+4] = response[x+4] & 0xff;
			}
		}
		//printRequestResult(cr);
		
		CanbusMessage eeprom3_7 = new CanbusMessage(0x7e8, new byte[] {(byte) 0x22, (byte) 0xe7, (byte) 0x00}, "Get eprom3 0x22-24");
		//monitorTextArea.append("Sending "+eeprom3_7.toDetailsString()+" , response expected\n");
		cr = canbusRequestAndResponses(3, eeprom3_7,
				new byte[] {(byte) 0x06, (byte) 0x62, (byte) 0xe7, (byte) 0x00}, 0x7ec, null, 100L);
		if (cr.getResult())
		{
			byte[] response = cr.getTargetResponse().getData();
			for (int x=0;x<3;x++)
			{
				eeprom3aData[x+8] = response[x+4] & 0xff;
			}
		}
		//printRequestResult(cr);

		CanbusMessage eeprom3_8 = new CanbusMessage(0x7e8, new byte[] {(byte) 0x22, (byte) 0xe7, (byte) 0x06}, "Get eprom3 0x25-27");
		//monitorTextArea.append("Sending "+eeprom3_8.toDetailsString()+" , response expected\n");
		cr = canbusRequestAndResponses(3, eeprom3_8,
				new byte[] {(byte) 0x06, (byte) 0x62, (byte) 0xe7, (byte) 0x06}, 0x7ec, null, 100L);
		if (cr.getResult())
		{
			byte[] response = cr.getTargetResponse().getData();
			for (int x=0;x<3;x++)
			{
				eeprom3aData[x+11] = response[x+4] & 0xff;
			}
		}
		//printRequestResult(cr);

		CanbusMessage eeprom3_9 = new CanbusMessage(0x7e8, new byte[] {(byte) 0x22, (byte) 0xe7, (byte) 0x17}, "Get eprom3 0x28-29");
		//monitorTextArea.append("Sending "+eeprom3_9.toDetailsString()+" , response expected\n");
		cr = canbusRequestAndResponses(3, eeprom3_9,
				new byte[] {(byte) 0x05, (byte) 0x62, (byte) 0xe7, (byte) 0x17}, 0x7ec, null, 100L);
		if (cr.getResult())
		{
			byte[] response = cr.getTargetResponse().getData();
			for (int x=0;x<2;x++)
			{
				eeprom3aData[x+14] = response[x+4] & 0xff;
			}
		}
		//printRequestResult(cr);

		
		//display result
		s = String.format("%2x %2x %2x %2x %2x %2x %2x %2x",
				eeprom3aData[0], eeprom3aData[1], eeprom3aData[2], eeprom3aData[3],
				eeprom3aData[4], eeprom3aData[5], eeprom3aData[6], eeprom3aData[7]);
		
		monitorTextArea.append("eeprom3 data 1a-21: " + s + "\n");
		
		s = String.format("%2x %2x %2x %2x %2x %2x %2x %2x",
				eeprom3aData[8], eeprom3aData[9], eeprom3aData[10], eeprom3aData[11],
				eeprom3aData[12], eeprom3aData[13], eeprom3aData[14], eeprom3aData[15]);
		
		monitorTextArea.append("eeprom3 data 22-29: " + s + "\n");
		
		String vin = "";
		for (int x=0;x<8;x++)
		{
			int charValue = eeprom3aData[x] & 0xff;
			char c = (char) charValue;
			vin = vin + c;
		}
		
		int num = ((eeprom3aData[8] & 0xff) >> 4) + 0x37;
		char c = (char) num;
		vin = vin + c;
		
		num = (eeprom3aData[8] & 0x0f) + 0x30;
		c = (char) num;
		vin = vin + c;	
		
		for (int x=0;x<2;x++)
		{
			num = ((eeprom3aData[9+x] & 0xf0) >> 4) + 0x30;
			c = (char) num;
			vin = vin + c;	
			num = (eeprom3aData[9+x] & 0x0f) + 0x30;
			c = (char) num;
			vin = vin + c;	
		}
		
		monitorTextArea.append("vin: " + "SAJ" + vin + "\n");
		
		for (int x=0;x<13;x++)
		{
			allData[x] = (byte) (eeprom3Data[x] & 0xff );
		}
		for (int x=0;x<16;x++)
		{
			allData[x+13] = (byte) (eeprom3aData[x] & 0xff );
		}
		return allData;

	}
	
	
	private void downloadEeproms()
	{
	
		//get eeprom1 data
		// 160k without TPU
		// 6 + 1029 * 160 = 164646

		byte[][] downloadData = new byte[2][164646];

		
		SwingWorker<Boolean, String> getDownloadData = new SwingWorker<Boolean, String>()
		{
			@Override
			protected Boolean doInBackground()
			{
				
				// total number of 1k blocks to download per eeprom
				// is 160
				final int totalBlocks = 160;

				try
				{
					for (int eepromID=0;eepromID<2;eepromID++)
					{											
						int targetBlockAddressByte0 = 0;
						int targetBlockAddressByte1 = 0;
						
						//write CPU signature
						downloadData[eepromID][0] = 04;
						downloadData[eepromID][1] = 00;
						downloadData[eepromID][2] = 00;
						downloadData[eepromID][3] = (byte) totalBlocks;
						// downloadData[3] = (byte) 160;
						
						downloadData[eepromID][4] = (byte) 0x54;
						downloadData[eepromID][5] = (byte) 0xaa;
						downloadData[eepromID][6] = (byte) 0;
						downloadData[eepromID][7] = (byte) 0;
						downloadData[eepromID][8] = (byte) 0;
						
						//last two byte are 0
						downloadData[eepromID][164644] = (byte) 0;
						downloadData[eepromID][164645] = (byte) 0;
						
						//allow first block to download without waiting for response
						boolean downloadResponse = true;
						
						if (eepromID == 0)
						{
							publish("Download CPU 1 eeprom");
						}
						else
						{
							publish("Download CPU 2 eeprom");
						}
						
						for (int blockCount=0;blockCount<totalBlocks;blockCount++)
						{
							//compute start address of data block
							//first 128k is 0 - 0x20000
							//next 32k is 0xb8000 - bffff
							
							if (blockCount < 128) //first 128k
							{
								targetBlockAddressByte0 = (byte) (blockCount / 64);
								targetBlockAddressByte1 = (byte) (blockCount % 64)*4;
							}
							else if ((blockCount > 127) && (blockCount < totalBlocks)) //32k
							{
								targetBlockAddressByte0 = 0xb;
								targetBlockAddressByte1 = (byte)(128+(blockCount - 128)*4);
							}
								
							byte instructionByte = 0;
							
							//request 1k download block
							//use 0x35 00 for CPU1 and 0x35 80 for CPU2
							if (eepromID == 1)
							{
								instructionByte = (byte) 0x80;
							}

							String eepromMsgName = "Get eeprom1 1k block";
							
							if (eepromID == 1)
							{
								eepromMsgName = "Get eeprom2 1k block";
							}
							
							CanbusMessage blockRequest = new CanbusMessage(0x20, new byte[] {(byte) 0x35, instructionByte,
									(byte) 04, (byte) 00,
									(byte) targetBlockAddressByte0,	(byte) targetBlockAddressByte1, (byte) 00},
									eepromMsgName);
														
							//send download request message
							//this will trigger download messages
							//ack message to download request will be delayed
							// don't send unless previous send was acknowledged, or first send
							if (downloadResponse)
							{
								//publish("Sending "+blockRequest.toDetailsString());
								blockRequest.sendMessage();
								downloadResponse = false;
							}
													
							String s = String.format("%2x%2x ",targetBlockAddressByte0 & 0xff, targetBlockAddressByte1 & 0xff);
							publish("Download block "+s);
							
							boolean foundDownloadMessage = false;
							int blocksDownloaded = 0;
							
							//download first 170 blocks of 6 bytes
							do
							{	
								do
								{
									foundDownloadMessage = false;
									//look for a download message
									if (receiveQueue.size() != 0)
									{
										CanbusMessage rxMessage = receiveQueue.getFirstElement();
	//									String str = String.format("Block Count %2x ",blocksDownloaded);
	//									publish(str+"Received "+rxMessage.toDetailsString());
										receiveQueue.removeFirstElement();
										if ((rxMessage.getID() == 0x30))
										{
											//look for download data message
											byte[] response = rxMessage.getData();
											if ((response[0] == 0x07) && (response[1] == 0x36))
											{
												for (int y=0;y<6;y++)
												{
													downloadData[eepromID][9+(blockCount*1029)+(6*blocksDownloaded)+y] = response[y+2];
												}
												//String str = String.format("Msg %3d ", x);
												//publish("Received "+str+rxMessage.toDetailsString());
												foundDownloadMessage = true;
											}
											
											//look for download command acknowledge
											if ((response[0] == 0x06) && (response[1] == 0x7f) && (response[2] == 0x35))
											{
												//publish("Download response received");
												downloadResponse = true;
											}
	
										}
									}
									else
									{
										//delay to avoid block queue with size() calls
										long timeStamp = System.currentTimeMillis() + 2;
										do {} while (System.currentTimeMillis() < timeStamp);
									}
	
								//continue until a download message is found
								}
								while(!foundDownloadMessage);
								
								blocksDownloaded++;
							}
							while (blocksDownloaded < 170);
							
							//download last 4 bytes
							do
							{
								foundDownloadMessage = false;
								//look for a download message
								if (receiveQueue.size() != 0)
								{
									CanbusMessage rxMessage = receiveQueue.getFirstElement();
									//String str = String.format("Block Count (hex) aa ");
									//publish(str+"Received "+rxMessage.toDetailsString());
									receiveQueue.removeFirstElement();
									if ((rxMessage.getID() == 0x30))
									{
										byte[] response = rxMessage.getData();
										if ((response[0] == 0x07) && (response[1] == 0x36))
										{
											for (int y=0;y<4;y++)
											{
												downloadData[eepromID][9+(blockCount*1029)+(6*170)+y] = response[y+2];
											}
											//String str = String.format("Msg %3d ", 170);
											//publish("Received "+str+rxMessage.toDetailsString());
											foundDownloadMessage = true;
										}
										
										//write header for block, unless first block which is skipped
										if (blockCount != 0)
										{
											downloadData[eepromID][9+(blockCount*1029)-5] = 0x00;
											downloadData[eepromID][9+(blockCount*1029)-4] = 0x00;
											downloadData[eepromID][9+(blockCount*1029)-3] = (byte) targetBlockAddressByte0;
											downloadData[eepromID][9+(blockCount*1029)-2] = (byte) targetBlockAddressByte1;
											downloadData[eepromID][9+(blockCount*1029)-1] = 0x00;
										}
										
										//look for download command acknowledge
										if ((response[0] == 0x06) && (response[1] == 0x7f) && (response[2] == 0x35))
										{
											downloadResponse = true;
										}
	
									}
								}
								else
								{
									//delay to avoid block queue with size() calls
									long timeStamp = System.currentTimeMillis() + 2;
									do {} while (System.currentTimeMillis() < timeStamp);
								}
	
							//continue until a download message is found
							}
							while(!foundDownloadMessage);
							
							setProgress( ( (blockCount*50)/totalBlocks) + (eepromID*50));
							
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				};
				
				setProgress(100);
				return Boolean.TRUE;
			}
			
			@Override
			protected void process(List<String> progressMessages)
			{
				Iterator<String> it = progressMessages.iterator();
				while (it.hasNext())
				{
					monitorTextArea.append(it.next()+"\n");		
				}
			}
			
			@Override
			protected void done()
			{
				monitorTextArea.append("Finished Download\n");
				monitorTextArea.append("Saving eeprom data in files\n");
				saveEepromData(downloadData);
				monitorTextArea.append("Eeprom data saved\n");
			}
		};
		
		// add listener to update progress bar
		getDownloadData.addPropertyChangeListener(new PropertyChangeListener() {
	        @Override
	        public void propertyChange(PropertyChangeEvent e) {
	            if ("progress".equals(e.getPropertyName())) {
	                downloadProgressBar.setValue((Integer) e.getNewValue());
	            }
	        }
	    });

		getDownloadData.execute();
	}
	
	
	private String byteArraytoString(byte[] ba)
	{
		String s = "";
		if (ba == null)
		{
			s = s.concat("Null");
		}
		else
		{
			for (int x=0;x<8;x++)
			{
				s = s.concat(String.format(" %2x", ba[x]));
			}
		}
		return s;
	}
	
	
	private void saveEepromData(byte[][] data)
	{	
		//file is an existing file chooser
		//save the file
		fc.setDialogTitle("Root name for the eeprom files?");
		
		int userSelection = fc.showSaveDialog(eepromdownloadButton);
		
		// get base file name
		// add _C and _D after file name and .b68 extension
		// save the data into the 2 files
		
		if (userSelection == JFileChooser.APPROVE_OPTION)
		{
			File saveFile = fc.getSelectedFile();
			String filePath = saveFile.getPath();				
						
			for (int x=0;x<2;x++)
			{
				if (x==0)
				{
					saveFile = new File(filePath + "_C.b68");
				}
				else
				{
					saveFile = new File(filePath + "_D.b68");;
				}
				
				try (FileOutputStream out = new FileOutputStream(saveFile))
				{
					out.write(data[x]);
				}
				catch (FileNotFoundException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	
	private void reflashEeproms()
	{
		
		SwingWorker<Integer, String> reflash = new SwingWorker<Integer, String>()
		{
			private void printRequestResult(CanbusResponses cr)
			{
				Iterator<CanbusMessage> it = cr.getResponses().iterator();						
				while (it.hasNext())
				{
					CanbusMessage m = (it.next());
					publish( m.toDetailsString());
				}
				String result = "Result is ";
				if (cr.getResult())
				{
					result = result.concat("true");
				}
				else
				{
					result = result.concat("false");
				}
				publish(result);
			}
			
			@Override
			protected Integer doInBackground()
			{	
				int resultValue = 0;

				try
				{	
					// *******************
					// reflash eeprom 1
					// *******************
					
					//partial array for uploading data
					byte[] uploadArray = new byte[]{(byte) 0x02, (byte) 0x00, (byte) 0x07, (byte) 0x36,
							(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
							
					//compute the checksum for programmer1
					int blocks = cpu1CodeFile.getNumberOfBlocks();
					int checksum = 0;
					
					for (int blockCount=0;blockCount<blocks;blockCount++)
					{
						//get 1k bytes data block
						byte[] data = cpu1CodeFile.getDataBlock(blockCount);
						//compute checksum over this 512 words
						for (int x=0;x<512;x++)
						{
							int memWord = ((data[x*2] & 0xff) << 8) + ((data[(x*2)+1] & 0xff));
							checksum = (checksum + memWord) & 0xffff;
						}
					}
					
					byte checksumHighByte = (byte) ((checksum & 0xff00) >> 8);
					byte checksumLowByte = (byte) (checksum & 0xff);
					
					// procedure to reflash is:
					//
					//	reset RTC timeout counter, by
					//		sending read cpu2 flash volts (can22_e7_2c)
					//
					// erase eproms
					//		06 31 a1/b1 00 00 00 00
					//			response ack 06 7f 31 a1/b1 00 00 00
					// validate erase
					//		06 32 a1/b1 00 00 00 00
					//		responses
					//			06 7f 32 a1 00 00 00	sucesss
					//			06 7f 32 a1 00 58 63	fail
					//			06 7f 32 a1 00 00 21	on-going
					//			06 7f 32 a1 00 4a 63	error?
					//
					//	program eproms
					//	 
					// send number of blocks to be uploaded to ecu
					//		06 31 a0/b0 00 a0 00 00  for 160 (0xa0) blocks
					// for each 1k block
					//		send target address 34 80/00 04 00  address1, 2, 3  to ecu
					//			ack 6 7f 34 80/00 04 00 addr1
					//		send get ready message to arduino  34 00
					//		send 170 upload packets to arduino  36
					//      send upload to ecu message to arduino  31 a2/b2
					//		arduino will upload data and then send 31 a2/b2 to ecu
					//		send clear target address to ecu	37
					//		send block upload complete to ecu   32 a2/b2
					//			responses
					//				06 7f 32 a2 00 00 00	success
					//				06 7f 32 a2 00 58 63	fail
					//				06 7f 32 a2 00 00 21	on-going
					// send checksum to ecu  31 a3/b3 pp qq 00 00 00
					//				ack 06 7f 31 a2 00 00 00
					// send validate checksum to ecu 32 a3/b3 00 00 00 00
					//		Responses
					//			06 7f 32 a3 00 00 00 – pass
					//			06 7f 32 a3 00 5b 63 – fail
					//			06 7f 32 a3 00 00 21 – still processing
					
					
					// send read cpu2 flash volts to force reset of RTC timeout counter (30 sec timer)
					String msg22_e7_2c = "Get CPU2 flash volts";
					byte[] data22_e7_2c = new byte[]{(byte)0x22,(byte)0xe7,(byte)0x2c};
					CanbusMessage can22_e7_2c = new CanbusMessage(0x20, data22_e7_2c, msg22_e7_2c);
					can22_e7_2c.sendMessage();
					
					String erase_501_msg = "Erase flash memory of CPU1";
					byte[] erase_501_data = new byte[]{(byte)0x31,(byte)0xa1,(byte)0x00};
					CanbusMessage erase_501 = new CanbusMessage(0x20, erase_501_data, erase_501_msg);

					publish("Sending "+erase_501.toDetailsString());
					CanbusResponses reply = canbusRequestAndResponses(1, erase_501, new byte[] {(byte) 0x06, (byte) 0x7f,
							(byte) 0x31, (byte) 0xa1, (byte) 0x00, (byte) 0x00, (byte) 0x00}, 0x30, null, 5000L);
					// expect response ack 06 7f 31 a1/b1 00 00 00
					printRequestResult(reply);
					
					//validate erase success
					
					String erase_result_501_msg = "Confirm erased flash memory of CPU1";
					byte[] erase_result_501_data = new byte[]{(byte)0x32,(byte)0xa1,(byte)0x00};
					CanbusMessage erase_result_501 = new CanbusMessage(0x20, erase_result_501_data, erase_result_501_msg);	
					
					publish("Sending "+erase_result_501.toDetailsString());
					reply = canbusRequestAndResponses(10, erase_result_501,
							new byte[] {(byte) 0x06, (byte) 0x7f, (byte) 0x32, (byte) 0xa1,
									(byte) 0x00, (byte) 0x00, (byte) 0x00}, 0x30, null, 500L);
					// expect response ack 06 7f 31 a1/b1 00 00 00
					printRequestResult(reply);

					if (!reply.getResult()) {return Integer.valueOf(0);}; //exit if error

					//send load 160 blocks commmand to cpu1
					//	06 31 a0/b0 00 a0 00 00  for 160 (0xa0) blocks

					String program_501_blocks_msg = "Prepare to write 160 blocks to CPU1";
					byte[] program_501_blocks_data = new byte[]{(byte)0x31,(byte)0xa0,(byte)0x00,(byte)0xa0};
					CanbusMessage program_501_blocks = new CanbusMessage(0x20, program_501_blocks_data, program_501_blocks_msg);	
					
					publish("Sending "+program_501_blocks.toDetailsString());
					reply = canbusRequestAndResponses(1, program_501_blocks,
							new byte[] {(byte) 0x06, (byte) 0x7f,
									(byte) 0x31, (byte) 0xa0, (byte) 0x00, (byte) 0xa0}, 0x30, null, 500L);
					//expect response 06 7f 31 a0 00 a0 00
					printRequestResult(reply);
					
					// ***********************
					
					//reflash the 160 blocks
					for (int blockCount = 0;blockCount<160;blockCount++)
					{
						publish("Block "+ blockCount);
						//reset RTC counter
						can22_e7_2c.sendMessage();
						//publish("Sending "+can22_e7_2c.toDetailsString());

						byte targetBlockAddressByte0 = 0;
						byte targetBlockAddressByte1 = 0;
						byte[] uploadBlock;
						
						if (blockCount < 64) //first 128k
						{
							targetBlockAddressByte0 = (byte) (0x00);
							targetBlockAddressByte1 = (byte) (blockCount*4);
							//retrieve 1k data block
							uploadBlock = cpu1CodeFile.getDataBlock(blockCount);
						}
						else if ((blockCount >= 64) && (blockCount < 128))
						{
							targetBlockAddressByte0 = 0x01;
							targetBlockAddressByte1 = (byte)((blockCount - 64)*4);
							//retrieve 1k data block
							uploadBlock = cpu1CodeFile.getDataBlock(blockCount);
						}
						else
						{
							targetBlockAddressByte0 = 0x0b;
							targetBlockAddressByte1 = (byte)(128 + (blockCount - 128)*4);							
							//retrieve 1k data block
							uploadBlock = cpu1CodeFile.getDataBlock(blockCount);
						}
						
						//load a 1k block to target RAM address
						String reflash_501_addr_msg = String.format("Reflash 1k block at address %2x %2x %2x",
								targetBlockAddressByte0, targetBlockAddressByte1, 0);
						byte[] reflash_501_addr_data = new byte[]{(byte)0x34,(byte)0x80,(byte)0x04,(byte)0x00,
								targetBlockAddressByte0,targetBlockAddressByte1, (byte) 0x00};
						CanbusMessage reflash_501_addr = new CanbusMessage(0x20, reflash_501_addr_data, reflash_501_addr_msg);
												
						//publish("Sending "+reflash_501_addr.toDetailsString());
						reply = canbusRequestAndResponses(1, reflash_501_addr,
								new byte[] {(byte) 0x06, (byte) 0x7f,
										(byte) 0x34, (byte) 0x80, (byte) 0x04, (byte) 0x00, 0x00}, 0x30, null, 500L);
						//expect response ack 6 7f 34 80/00 04 00 00
						//printRequestResult(reply);
																
						//publish("Sending "+arduinoUploadStart.toDetailsString());
						reply = canbusRequestAndResponses(1, arduinoUploadStart,
								new byte[] {(byte) 0x03, (byte) 0x7f,
										(byte) 0x34, (byte) 0x00}, 0x50, null, 500L);
						//expect response 03 7f 34 00
						//enables upload flag, zeros packet count and byte count
						//printRequestResult(reply);
										
						//send 170*6 data bytes
						//each send increments packet count and byte count + 6
						//meter the send rate to avoid overloading the serial port
												
						for (int x=0;x<170;x++)
						{
														
							//send upload packet
							int offset = x*6;
							System.arraycopy(uploadBlock, offset, uploadArray, 4, 6);
							CanbusMessage data = new CanbusMessage(uploadArray);
							data.sendMessage(); // no print to monitor
							
							try
							{
								Thread.sleep(1); //pause 1 ms to flow control the send rate
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
						}
						
						//send last 4 bytes
						
						int offset = 1020;
						System.arraycopy(uploadBlock, offset, uploadArray, 4, 4);
						uploadArray[8] = (byte) 0x00;
						uploadArray[9] = (byte) 0x00;
						CanbusMessage data = new CanbusMessage(uploadArray);
						data.sendMessage();
						
						//validate arduino received all the data SID 0x10 02 31 a2
						
						byte[] testVector = new byte[] {(byte) 0x06, (byte) 0x7f, (byte) 0x32, (byte) 0xa2,
								(byte) 0x00, (byte) 0x00, (byte) 0x00};
						byte[] ignoreDisplayVector = new byte[] {(byte) 0x06, (byte) 0x7f, (byte) 0x32, (byte) 0xa2,
								(byte) 0x00, (byte) 0x00, (byte) 0x21};
												
						//publish("Sending "+arduinoUploadEnd1.toDetailsString());
						reply = canbusRequestAndResponses(1, arduinoUploadEnd1,
								testVector, 0x30, ignoreDisplayVector, 5000L);
						//expect response ack 6 7f 34 80/00 04 00 00
						//printRequestResult(reply);

						if (!reply.getResult()) {return Integer.valueOf(0);}; //exit if error
						
						//expect acknowledgement 03 31 a2 xx where xx is the packet count (sets of 6 bytes)
						//expect subsequent response after upload is complete of:
						// success - 03 7f 31 a2
						// fail - no response
											
						//validate block sent - 02 31 a2 - is sent by arduino otherwise ecu will timeout
						//expect response 06 7f 31 a2 00 00 00
																
						//update progress bar with value 0 - 40
						setProgress((blockCount*40)/160);
						 
					}
					
						
					//upload expected checksum and compute actual uploaded checksum
					//checksum is 16 bit sum of words from addresses 0-1ffff, b8000-bffff
					// last 4 bytes are asssumed to be ff ff 00 00
					// 04 31 a3 pp qq
					String msg31_a3 = "Upload and compute checksum for cpu1";
					byte[] data31_a3 = new byte[]{(byte)0x31,(byte)0xa3, checksumHighByte, checksumLowByte};
					CanbusMessage can31_a3 = new CanbusMessage(0x20, data31_a3, msg31_a3);
					byte[] ignoreDisplayVector = new byte[] {(byte) 0x06, (byte) 0x7f, (byte) 0x32, (byte) 0xa3,
							(byte) 0x00, (byte) 0x00, (byte) 0x21};

					
					publish("Sending "+can31_a3.toDetailsString());
					reply = canbusRequestAndResponses(1, can31_a3,
							new byte[] {(byte) 0x06, (byte) 0x7f, (byte) 0x32,
									(byte) 0xa3, (byte) 0x00, (byte) 0x00, (byte) 0x00},
							0x30, ignoreDisplayVector, 10000L);
					//arduino will send 2 32 a3 and ping until valid response
					//expect response 06 7f 32 a3 00 0 00
					printRequestResult(reply);
					
					//		Responses
					//			06 7f 32 a3 00 00 00 – pass
					//			06 7f 32 a3 00 5b 63 – fail
					//			06 7f 32 a3 00 00 21 – still processing
					
					if (reply.getResult())
					{	
						//cpu1 programmed successfully
						resultValue = 1;
						//update progress bar with value 50
						setProgress(50);

					}
					else
					{
						resultValue = 0;
						while (resultValue== 0) { };
						//cpu1 programming failed
					}
					
					if (!reply.getResult()) {return Integer.valueOf(0);}; //exit if error
					
					// *******************
					// reflash cpu 2
					// *******************
						
					//compute the checksum for cpu2
					blocks = cpu2CodeFile.getNumberOfBlocks();
					checksum = 0;
					
					for (int blockCount=0;blockCount<blocks;blockCount++)
					{
						//get 1k bytes data block
						byte[] data = cpu2CodeFile.getDataBlock(blockCount);
						//compute checksum over this 512 words
						for (int x=0;x<512;x++)
						{
							int memWord = ((data[x*2] & 0xff) << 8) + ((data[(x*2)+1] & 0xff));
							checksum = (checksum + memWord) & 0xffff;
						}
					}
					
					checksumHighByte = (byte) ((checksum & 0xff00) >> 8);
					checksumLowByte = (byte) (checksum & 0xff);				
					
					// send read cpu2 flash volts to force reset of RTC timeout counter (30 sec timer)
					can22_e7_2c.sendMessage();
					
					String erase_601_msg = "Erase flash memory of CPU2";
					byte[] erase_601_data = new byte[]{(byte)0x31,(byte)0xb1,(byte)0x00};
					CanbusMessage erase_601 = new CanbusMessage(0x20, erase_601_data, erase_601_msg);
					
					publish("Sending "+erase_601.toDetailsString());
					reply = canbusRequestAndResponses(1, erase_601, new byte[] {(byte) 0x06, (byte) 0x7f,
							(byte) 0x31, (byte) 0xb1, (byte) 0x00, (byte) 0x00, (byte) 0x00}, 0x30, null, 5000L);
					// expect response ack 06 7f 31 a1/b1 00 00 00
					printRequestResult(reply);
					
					//validate erase success
					
					String erase_result_601_msg = "Confirm erased flash memory of CPU2";
					byte[] erase_result_601_data = new byte[]{(byte)0x32,(byte)0xb1,(byte)0x00};
					CanbusMessage erase_result_601 = new CanbusMessage(0x20, erase_result_601_data, erase_result_601_msg);
					byte[] testVector = new byte[] {(byte) 0x06, (byte) 0x7f, (byte) 0x32, (byte) 0xb1, (byte) 0x00, (byte) 0x00, (byte) 0x00};
					
					publish("Sending "+erase_result_601.toDetailsString());
					reply = canbusRequestAndResponses(10, erase_result_601,
							testVector, 0x30, null, 500L);
					// expect response ack 06 7f 31 a1/b1 00 00 00
					printRequestResult(reply);

					if (!reply.getResult()) {return Integer.valueOf(resultValue);}; //exit if error

					//send load 160 blocks commmand to cpu1
					//	06 31 a0/b0 00 a0 00 00  for 160 (0xa0) blocks

					String program_601_blocks_msg = "Prepare to write 160 blocks to CPU2";
					byte[] program_601_blocks_data = new byte[]{(byte)0x31,(byte)0xb0,(byte)0x00,(byte)0xa0};
					CanbusMessage program_601_blocks = new CanbusMessage(0x20, program_601_blocks_data, program_601_blocks_msg);	

					publish("Sending "+program_601_blocks.toDetailsString());
					reply = canbusRequestAndResponses(1, program_601_blocks,
							new byte[] {(byte) 0x06, (byte) 0x7f,
									(byte) 0x31, (byte) 0xb0, (byte) 0x00, (byte) 0xa0}, 0x30, null, 500L);
					//expect response 06 7f 31 b0 00 a0 00
					printRequestResult(reply);

					
					// ***********************
					
					//reflash the 160 blocks
					for (int blockCount = 0;blockCount<160;blockCount++)
					{
						publish("Block "+ blockCount);
						//reset RTC counter
						can22_e7_2c.sendMessage();
						//publish("Sending "+can22_e7_2c.toDetailsString());

						byte targetBlockAddressByte0 = 0;
						byte targetBlockAddressByte1 = 0;
						byte[] uploadBlock;
						
						if (blockCount < 64) //first 128k
						{
							targetBlockAddressByte0 = (byte) (0x00);
							targetBlockAddressByte1 = (byte) (blockCount*4);
							//retrieve 1k data block
							uploadBlock = cpu2CodeFile.getDataBlock(blockCount);
						}
						else if ((blockCount >= 64) && (blockCount < 128))
						{
							targetBlockAddressByte0 = 0x01;
							targetBlockAddressByte1 = (byte)((blockCount - 64)*4);
							//retrieve 1k data block
							uploadBlock = cpu2CodeFile.getDataBlock(blockCount);
						}
						else
						{
							targetBlockAddressByte0 = 0x0b;
							targetBlockAddressByte1 = (byte)(128 + (blockCount - 128)*4);							
							//retrieve 1k data block
							uploadBlock = cpu2CodeFile.getDataBlock(blockCount);
						}						
						//load a 1k block to target RAM address
						String reflash_601_addr_msg = String.format("Reflash 1k block at address %2x %2x %2x",
								targetBlockAddressByte0, targetBlockAddressByte1, 0);
						byte[] reflash_601_addr_data = new byte[]{(byte)0x34,(byte)0x00,(byte)0x04,(byte)0x00,
								targetBlockAddressByte0,targetBlockAddressByte1, (byte) 0x00};
						CanbusMessage reflash_601_addr = new CanbusMessage(0x20, reflash_601_addr_data, reflash_601_addr_msg);
						
						//publish("Sending "+reflash_601_addr.toDetailsString());
						reply = canbusRequestAndResponses(1, reflash_601_addr,
								new byte[] {(byte) 0x06, (byte) 0x7f,
										(byte) 0x34, (byte) 0x00, (byte) 0x04, (byte) 0x00, 0x00}, 0x30, null, 500L);
						//expect response ack 6 7f 34 80/00 04 00 00
						//printRequestResult(reply);
																		
						//signal to arduino start of upload 1k block  SID 0x10  02 34 00
						//publish("Sending "+arduinoUploadStart.toDetailsString());
						reply = canbusRequestAndResponses(1, arduinoUploadStart,
								new byte[] {(byte) 0x03, (byte) 0x7f,
										(byte) 0x34, (byte) 0x00}, 0x50, null, 500L);
						//expect response 03 7f 34 00
						//enables upload flag, zeros packet count and byte count
						//printRequestResult(reply);
												
						//send 170*6 data bytes
						//each send increments packet count and byte count + 6
						//meter the send rate to avoid overloading the serial port
												
						for (int x=0;x<170;x++)
						{
														
							//send upload packet
							int offset = x*6;
							System.arraycopy(uploadBlock, offset, uploadArray, 4, 6);
							CanbusMessage data = new CanbusMessage(uploadArray);
							data.sendMessage(); // no print to monitor
							
							try
							{
								Thread.sleep(1); //pause 1 ms to flow control the send rate
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
						}
						
						//send last 4 bytes
						
						int offset = 1020;
						System.arraycopy(uploadBlock, offset, uploadArray, 4, 4);
						uploadArray[8] = (byte) 0x00;
						uploadArray[9] = (byte) 0x00;
						CanbusMessage data = new CanbusMessage(uploadArray);
						data.sendMessage();
						
						//validate arduino received all the data SID 0x10 02 31 b2
						//publish("Sending "+arduinoUploadEnd2.toDetailsString());
						
						testVector = new byte[] {(byte) 0x06, (byte) 0x7f, (byte) 0x32, (byte) 0xb2,
								(byte) 0x00, (byte) 0x00, (byte) 0x00};
						ignoreDisplayVector = new byte[] {(byte) 0x06, (byte) 0x7f, (byte) 0x32, (byte) 0xb2,
								(byte) 0x00, (byte) 0x00, (byte) 0x21};
						
						//publish("Sending "+arduinoUploadEnd2.toDetailsString());
						reply = canbusRequestAndResponses(1, arduinoUploadEnd2,
								testVector, 0x30, ignoreDisplayVector, 5000L);
						//expect response ack 6 7f 34 80/00 04 00 00
						//printRequestResult(reply);

						if (!reply.getResult()) {return Integer.valueOf(resultValue);}; //exit if error
						
						//expect acknowledgement 03 31 a2 xx where xx is the packet count (sets of 6 bytes)
						//expect subsequent response after upload is complete of:
						// success - 03 7f 31 a2
						// fail - no response
											
						//validate block sent - 02 31 a2 - is sent by arduino otherwise ecu will timeout
						//expect response 06 7f 31 a2 00 00 00
																
						//update progress bar with value 0 - 40
						setProgress(50+(blockCount*40)/160);
					}			
						
					//upload expected checksum and compute actual uploaded checksum
					//checksum is 16 bit sum of words from addresses 0-1ffff, b8000-bffff
					// last 4 bytes are asssumed to be ff ff 00 00
					// 04 31 b3 pp qq
					String msg31_b3 = "Upload and compute checksum for cpu2";
					byte[] data31_b3 = new byte[]{(byte)0x31,(byte)0xb3, checksumHighByte, checksumLowByte};
					ignoreDisplayVector = new byte[] {(byte) 0x06, (byte) 0x7f, (byte) 0x32, (byte) 0xb3,
							(byte) 0x00, (byte) 0x00, (byte) 0x21};

					CanbusMessage can31_b3 = new CanbusMessage(0x20, data31_b3, msg31_b3);
					
					publish("Sending "+can31_b3.toDetailsString());
					reply = canbusRequestAndResponses(1, can31_b3,
							new byte[] {(byte) 0x06, (byte) 0x7f, (byte) 0x32,
									(byte) 0xb3, (byte) 0x00, (byte) 0x00, (byte) 0x00},
							0x30, ignoreDisplayVector, 10000L);
					//arduino will send 2 32 b3 and ping until valid response
					//expect response 06 7f 32 b3 00 0 00
					printRequestResult(reply);

					//		Responses
					//			06 7f 32 b3 00 00 00 – pass
					//			06 7f 32 b3 00 5b 63 – fail
					//			06 7f 32 b3 00 00 21 – still processing
					
					if (reply.getResult())
					{	
						//cpu2 programmed successfully
						resultValue += 2;
						//update progress bar with value 50
						setProgress(100);

					}
					else
					{
						return Integer.valueOf(resultValue); //exit if error

						//cpu2 programming failed
					}
					
				}
				catch (Exception e)
				{
					e.printStackTrace();
				};
				
				return Integer.valueOf(resultValue);
			}
			
			@Override
			protected void process(List<String> progressMessages)
			{
				Iterator<String> it = progressMessages.iterator();
				while (it.hasNext())
				{
					monitorTextArea.append(it.next()+"\n");		
				}
			}
			
			@Override
			protected void done()
			{
				int result = 0;
				
				try
				{
					Integer resultInteger = get();
					result = resultInteger.intValue();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				
				switch (result)
				{
				case 0:
					JOptionPane.showMessageDialog(null, "CPU1 and CPU2 reflash failed", "Reflash Results",
							JOptionPane.INFORMATION_MESSAGE);
					break;
				case 1:
					//cpu1 reflash success, cpu2 fail
					JOptionPane.showMessageDialog(null, "CPU1 reflashed, CPU2 reflash failed", "Reflash Results",
							JOptionPane.INFORMATION_MESSAGE);
					break;
				case 2:
					//cpu1 reflash fail, cpu2 success
					JOptionPane.showMessageDialog(null, "CPU2 reflashed, CPU1 reflash failed", "Reflash Results",
							JOptionPane.INFORMATION_MESSAGE);
					break;
				case 3:
					//cpu1 and cpu2 reflash success
					JOptionPane.showMessageDialog(null, "CPU1 and CPU2 reflashed successfully", "Reflash Results",
							JOptionPane.INFORMATION_MESSAGE);
					break;
				}
				
				monitorTextArea.append("Finished reflash attempt\n");		
			}
		};
		
		// add listener to update progress bar
		reflash.addPropertyChangeListener(new PropertyChangeListener() {
	        @Override
	        public void propertyChange(PropertyChangeEvent e) {
	            if ("progress".equals(e.getPropertyName())) {
	                reflashProgressBar.setValue((Integer) e.getNewValue());
	            }
	        }
	    });
		
		//execute upload
		reflash.execute();
	}
	
	private void printRequestResult(CanbusResponses cr)
	{
		Iterator<CanbusMessage> it = cr.getResponses().iterator();						
		while (it.hasNext())
		{
			CanbusMessage m = (it.next());
			monitorTextArea.append("Received "+m.toDetailsString()+"\n");
		}
		String result = "Result is ";
		if (cr.getResult())
		{
			result = result.concat("true");
		}
		else
		{
			result = result.concat("false");
		}
		monitorTextArea.append(result+"\n");
	}
	
	
	private void validateAndLoadPersistentDataFile(File dataFile)
	{
		//check signature is 0x357e
		//check fileName ends with .p68
		//check length is 31 bytes
		//load data
		//enable program persistent data button
		
		try ( FileInputStream pf1 = new FileInputStream(dataFile) )
		{			
			byte[] START_SEQ = {(byte) 0x35,(byte) 0x7e};
			
			boolean validName =  dataFile.getName().endsWith(".P68") || dataFile.getName().endsWith(".p68");
			
			int fileLength = (int) dataFile.length();

			byte[] targetBytes = new byte[fileLength];
			pf1.read(targetBytes);
			
			byte[] startSeq = new byte[2];
			System.arraycopy(targetBytes, 0, startSeq, 0, 2);
			boolean validStart = Arrays.equals(startSeq, START_SEQ);
						
			boolean validFileLength = (fileLength == 31);
									
			if (validName && validStart && validFileLength)
			{
				persistentDataFile = dataFile;
				persistentFileData = targetBytes;
				persistentDataLabel.setText(dataFile.getName());
				programPersistentButton.setEnabled(true);
				persistentDataLoaded = true;
			}
			else
			{
				//indicate file is invalid
				JPanel panel = new JPanel();
				JOptionPane.showMessageDialog(panel, "Invalid file format - file not loaded");
			}
			
			//display result
			String s = String.format("%2x %2x %2x %2x %2x %2x %2x",
					persistentFileData[2], persistentFileData[3], persistentFileData[4],
					persistentFileData[5], persistentFileData[6], persistentFileData[7], persistentFileData[8]);
			
			monitorTextArea.append("eeprom3 data 0-0d: " + s + "\n");

			//display result
			s = String.format("%2x %2x %2x %2x %2x %2x",
					persistentFileData[9], persistentFileData[10], persistentFileData[11],
					persistentFileData[12], persistentFileData[13], persistentFileData[14]);
			
			monitorTextArea.append("eeprom3 data 0e-19: " + s + "\n");

			s = String.format("%2x %2x %2x %2x %2x %2x %2x %2x",
					persistentFileData[15], persistentFileData[16], persistentFileData[17], persistentFileData[18],
					persistentFileData[19], persistentFileData[20], persistentFileData[21], persistentFileData[22]);
			
			monitorTextArea.append("eeprom3 data 1a-21: " + s + "\n");

			s = String.format("%2x %2x %2x %2x %2x %2x %2x %2x",
					persistentFileData[23], persistentFileData[24], persistentFileData[25], persistentFileData[26],
					persistentFileData[27], persistentFileData[28], persistentFileData[29], persistentFileData[30]);
			
			monitorTextArea.append("eeprom3 data 22-29: " + s + "\n");
			
			String vin = "";
			for (int x=0;x<8;x++)
			{
				int charValue = persistentFileData[x+15] & 0xff;
				char c = (char) charValue;
				vin = vin + c;
			}
			
			int num = ((persistentFileData[23] & 0xff) >> 4) + 0x37;
			char c = (char) num;
			vin = vin + c;
			
			num = (persistentFileData[23] & 0x0f) + 0x30;
			c = (char) num;
			vin = vin + c;	
			
			for (int x=0;x<2;x++)
			{
				num = ((persistentFileData[24+x] & 0xf0) >> 4) + 0x30;
				c = (char) num;
				vin = vin + c;	
				num = (persistentFileData[24+x] & 0x0f) + 0x30;
				c = (char) num;
				vin = vin + c;	
			}
			
			monitorTextArea.append("vin: " + "SAJ" + vin + "\n");
						
			pf1.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	
	private void savePersistentData(byte[] data)
	{	
		byte[] saveData = new byte[31];
		saveData[0] = (byte) 0x35;
		saveData[1] = (byte) 0x7e;
		for (int x=0;x<29;x++)
		{
			saveData[x+2] = data[x];
		}
		
		//file is an existing file chooser
		//save the file
		fc.setDialogTitle("Name for the persistent data file (no extension) ?");
		
		int userSelection = fc.showSaveDialog(savePersistentButton);
		
		if (userSelection == JFileChooser.APPROVE_OPTION)
		{
			File saveFile = fc.getSelectedFile();
			String filePath = saveFile.getPath();				
						
			saveFile = new File(filePath + ".p68");
				
			try (FileOutputStream out = new FileOutputStream(saveFile))
			{
				out.write(saveData);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

		}
		
		monitorTextArea.append("Persistent data saved\n");

	}
	
	
	private void writePersistentData()
	{
		SwingWorker<Boolean, String> writePData = new SwingWorker<Boolean, String>()
		{
			boolean success = true;
			
			private void printRequestResult(CanbusResponses cr)
			{
				Iterator<CanbusMessage> it = cr.getResponses().iterator();						
				while (it.hasNext())
				{
					CanbusMessage m = (it.next());
					publish( m.toDetailsString());
				}
				String result = "Result is ";
				if (cr.getResult())
				{
					result = result.concat("true");
				}
				else
				{
					result = result.concat("false");
				}
				publish(result);
			}

			@Override
			protected Boolean doInBackground()
			{

				//check data file has been loaded
				if (!persistentDataLoaded)
				{
					return Boolean.FALSE;
				}
				
				//unlock extended commands
				
				//write data to eeprom3
				// e7 23 - 3 bytes
				// e7 24 - 4
				// e7 18 - 3
				// 12 41 - 2
				// e7 08 - 1
				// e7 02 - 4
				// e7 01 - 4
				// e7 00 - 3
				// e7 06 - 3
				// e7 17 - 2
				
				try
				{	
					String msg27_01 = "Get CPU1 RTC counter value";
					byte[] data27_01 = new byte[]{(byte)0x27,(byte)0x01};
					CanbusMessage can27_01 = new CanbusMessage(0x7e8, data27_01, msg27_01);

					//publish("Sending "+can27_01.toDetailsString()+"\n");			
					CanbusResponses reply = canbusRequestAndResponses(1, can27_01,
							new byte[] {(byte) 0x03, (byte) 0x67, (byte) 0x02}, 0x7ec, null, 200L);
					//printRequestResult(reply);
					
					
					//erase eprom
					//02 31 04 > 06 7f 31 04 00 00 00
					//03 32 04 00 > 06 7f 32 04 00 00 62
					//03 32 04 01 > 06 7f 32 04 01 00 64
					
					String erase_persistent_data_msg = "Erase all persistent eprom data";
					byte[] erase_persistent_data = new byte[]{(byte)0x31,(byte)0x04};
					CanbusMessage erase_persistent = new CanbusMessage(0x7e8, erase_persistent_data, erase_persistent_data_msg);

					publish("Sending "+erase_persistent.toDetailsString());
					reply = canbusRequestAndResponses(3, erase_persistent, new byte[] {(byte) 0x06, (byte) 0x7f,
							(byte) 0x31, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00}, 0x7ec, null, 5000L);
					// expect response ack 06 7f 31 04 00 00 00
					printRequestResult(reply);
					
					//validate erase success
					
					String erase_persistent_result_msg = "Confirm erased all persistent eprom data step 1";
					byte[] erase_persistent_result_data = new byte[]{(byte)0x32,(byte)0x04,(byte)0x00};
					CanbusMessage erase_persistent_result = new CanbusMessage(0x7e8, erase_persistent_result_data, erase_persistent_result_msg);	
					
					publish("Sending "+erase_persistent_result.toDetailsString());
					reply = canbusRequestAndResponses(10, erase_persistent_result,
							new byte[] {(byte) 0x06, (byte) 0x7f, (byte) 0x32, (byte) 0x04,
									(byte) 0x00, (byte) 0x00, (byte) 0x62}, 0x7ec, null, 500L);
					// expect response ack 06 7f 32 04 00 00 62
					printRequestResult(reply);
					
					
					String msg_e7_23 = "Program eeprom3 00-05, 3 bytes";
					byte[] data_e7_23 = new byte[]{(byte)0x2f,(byte)0xe7,(byte)0x23,
							persistentFileData[2],persistentFileData[3], persistentFileData[4]};
					CanbusMessage can_e7_23 = new CanbusMessage(0x7e8, data_e7_23, msg_e7_23);

					//publish("Sending "+can_e7_23.toDetailsString());
					reply = canbusRequestAndResponses(3, can_e7_23, new byte[] {(byte) 0x06, (byte) 0x6f,
							(byte) 0xe7, (byte) 0x23}, 0x7ec, null, 300L);
					//expect response 06 6f e7 23 pp qq rr
					//printRequestResult(reply);
					success = success && reply.getResult();
					setProgress(10);
					

					String msg_e7_24 = "Program eeprom3 06-0d, 4 bytes";
					byte[] data_e7_24 = new byte[]{(byte)0x2f,(byte)0xe7,(byte)0x24,
							persistentFileData[5],persistentFileData[6], persistentFileData[7], persistentFileData[8]};
					CanbusMessage can_e7_24 = new CanbusMessage(0x7e8, data_e7_24, msg_e7_24);

					//publish("Sending "+can_e7_24.toDetailsString());
					reply = canbusRequestAndResponses(3, can_e7_24, new byte[] {(byte) 0x07, (byte) 0x6f,
							(byte) 0xe7, (byte) 0x24}, 0x7ec, null, 300L);
					//expect response 07 6f e7 24 pp qq rr ss
					//printRequestResult(reply);
					success = success && reply.getResult();
					setProgress(20);

					
					String msg_e7_18 = "Program eeprom3 0e-13, 3 bytes";
					byte[] data_e7_18 = new byte[]{(byte)0x2f,(byte)0xe7,(byte)0x18,
							persistentFileData[9],persistentFileData[10], persistentFileData[11]};
					CanbusMessage can_e7_18 = new CanbusMessage(0x7e8, data_e7_18, msg_e7_18);

					//publish("Sending "+can_e7_18.toDetailsString());
					reply = canbusRequestAndResponses(3, can_e7_18, new byte[] {(byte) 0x06, (byte) 0x6f,
							(byte) 0xe7, (byte) 0x18}, 0x7ec, null, 300L);
					//expect response 07 6f e7 18 pp qq rr
					//printRequestResult(reply);
					success = success && reply.getResult();
					setProgress(30);

					
					String msg_12_41 = "Program eeprom3 14-17, 2 bytes";
					byte[] data_12_41 = new byte[]{(byte)0x2f,(byte)0x12,(byte)0x41,
							persistentFileData[12],persistentFileData[13]};
					CanbusMessage can_12_41 = new CanbusMessage(0x7e8, data_12_41, msg_12_41);

					//publish("Sending "+can_12_41.toDetailsString());
					reply = canbusRequestAndResponses(3, can_12_41, new byte[] {(byte) 0x05, (byte) 0x6f,
							(byte) 0x12, (byte) 0x41}, 0x7ec, null, 300L);
					//expect response 05 6f 12 41 pp qq
					//printRequestResult(reply);
					success = success && reply.getResult();
					setProgress(40);
					

					String msg_e7_08 = "Program eeprom3 18-19, 1 byte";
					byte[] data_e7_08 = new byte[]{(byte)0x2f,(byte)0xe7,(byte)0x08,
							persistentFileData[14]};
					CanbusMessage can_e7_08 = new CanbusMessage(0x7e8, data_e7_08, msg_e7_08);

					//publish("Sending "+can_e7_08.toDetailsString());
					reply = canbusRequestAndResponses(3, can_e7_08, new byte[] {(byte) 0x04, (byte) 0x6f,
							(byte) 0xe7, (byte) 0x08}, 0x7ec, null, 300L);
					//expect response 04 6f e7 08 pp
					//printRequestResult(reply);
					success = success && reply.getResult();
					setProgress(50);
					

					String msg_e7_02 = "Program eeprom3 1a-1d, 4 bytes";
					byte[] data_e7_02 = new byte[]{(byte)0x2f,(byte)0xe7,(byte)0x02,
							persistentFileData[15],persistentFileData[16], persistentFileData[17], persistentFileData[18]};
					CanbusMessage can_e7_02 = new CanbusMessage(0x7e8, data_e7_02, msg_e7_02);

					//publish("Sending "+can_e7_02.toDetailsString());
					reply = canbusRequestAndResponses(3, can_e7_02, new byte[] {(byte) 0x07, (byte) 0x6f,
							(byte) 0xe7, (byte) 0x02}, 0x7ec, null, 300L);
					//expect response 07 6f e7 02 pp qq rr ss
					//printRequestResult(reply);
					success = success && reply.getResult();
					setProgress(60);

					
					String msg_e7_01 = "Program eeprom3 1e-21, 4 bytes";
					byte[] data_e7_01 = new byte[]{(byte)0x2f,(byte)0xe7,(byte)0x01,
							persistentFileData[19],persistentFileData[20], persistentFileData[21], persistentFileData[22]};
					CanbusMessage can_e7_01 = new CanbusMessage(0x7e8, data_e7_01, msg_e7_01);

					//publish("Sending "+can_e7_01.toDetailsString());
					reply = canbusRequestAndResponses(3, can_e7_01, new byte[] {(byte) 0x07, (byte) 0x6f,
							(byte) 0xe7, (byte) 0x01}, 0x7ec, null, 300L);
					//expect response 07 6f e7 01 pp qq rr ss
					//printRequestResult(reply);
					success = success && reply.getResult();
					setProgress(70);

					
					String msg_e7_00 = "Program eeprom3 22-24, 3 bytes";
					byte[] data_e7_00 = new byte[]{(byte)0x2f,(byte)0xe7,(byte)0x00,
							persistentFileData[23],persistentFileData[24], persistentFileData[25]};
					CanbusMessage can_e7_00 = new CanbusMessage(0x7e8, data_e7_00, msg_e7_00);

					//publish("Sending "+can_e7_00.toDetailsString());
					reply = canbusRequestAndResponses(3, can_e7_00, new byte[] {(byte) 0x06, (byte) 0x6f,
							(byte) 0xe7, (byte) 0x00}, 0x7ec, null, 300L);
					//expect response 06 6f e7 00 pp qq rr
					//printRequestResult(reply);
					success = success && reply.getResult();
					setProgress(80);
					
					
					String msg_e7_06 = "Program eeprom3 25-27, 3 bytes";
					byte[] data_e7_06 = new byte[]{(byte)0x2f,(byte)0xe7,(byte)0x06,
							persistentFileData[26],persistentFileData[27], persistentFileData[28]};
					CanbusMessage can_e7_06 = new CanbusMessage(0x7e8, data_e7_06, msg_e7_06);

					//publish("Sending "+can_e7_06.toDetailsString());
					reply = canbusRequestAndResponses(3, can_e7_06, new byte[] {(byte) 0x06, (byte) 0x6f,
							(byte) 0xe7, (byte) 0x06}, 0x7ec, null, 300L);
					//expect response 06 6f e7 06 pp qq rr
					//printRequestResult(reply);
					success = success && reply.getResult();
					setProgress(90);
					
					
					String msg_e7_17 = "Program eeprom3 28-29, 2 bytes";
					byte[] data_e7_17 = new byte[]{(byte)0x2f,(byte)0xe7,(byte)0x17,
							persistentFileData[29],persistentFileData[30]};
					CanbusMessage can_e7_17 = new CanbusMessage(0x7e8, data_e7_17, msg_e7_17);

					//publish("Sending "+can_e7_17.toDetailsString());
					reply = canbusRequestAndResponses(3, can_e7_17, new byte[] {(byte) 0x05, (byte) 0x6f,
							(byte) 0xe7, (byte) 0x17}, 0x7ec, null, 300L);
					//expect response 05 6f e7 17 pp qq
					//printRequestResult(reply);
					success = success && reply.getResult();
					setProgress(100);
					
					//clear extended commands
					byte[] data_20 = new byte[]{(byte)0x20};
					CanbusMessage can_20 = new CanbusMessage(0x7e8, data_20, "Clear security enable");
					//publish("Sending "+can_20.toDetailsString());
					reply = canbusRequestAndResponses(1, can_20, new byte[] {(byte) 0x06, (byte) 0x7f,
							(byte) 0x20}, 0x7ec, null, 200L);
					//printRequestResult(reply);
					
				}
				catch (Exception e)
				{
					e.printStackTrace();
				};
				
				if (success)
				{
					return Boolean.TRUE;
				}
				else
				{
					return Boolean.FALSE;
				}
			}
			
			@Override
			protected void process(List<String> progressMessages)
			{
				Iterator<String> it = progressMessages.iterator();
				while (it.hasNext())
				{
					monitorTextArea.append(it.next()+"\n");		
				}
			}
			
			@Override
			protected void done()
			{
				if (success)
				{
					monitorTextArea.append("Eeprom3 data written\n");
				}
				else
				{
					monitorTextArea.append("Eeprom3 data write failed\n");					
				}
			}
			
		};
		
		// add listener to update progress bar
		writePData.addPropertyChangeListener(new PropertyChangeListener() {
	        @Override
	        public void propertyChange(PropertyChangeEvent e) {
	            if ("progress".equals(e.getPropertyName())) {
	                persistentProgressBar.setValue((Integer) e.getNewValue());
	            }
	        }
	    });

		writePData.execute();
	}
	
	
	// retrieve all RAM data
	//from address 0x0b0000-b1bff, which is 7168 bytes
	private void retrieveRamData()
	{
		
		SwingWorker<ArrayList<Byte>, String> getRamData = new SwingWorker<ArrayList<Byte>, String>()
		{
			@Override
			protected ArrayList<Byte> doInBackground()
			{
				ArrayList<Byte> ramData = new ArrayList<Byte>();
				byte last_mid = 0;
				int progressCount = 0;
				
				try
				{
					for (int x=0;x<1792;x++)
					{
						byte addr_low = (byte)((x << 2) & 0xff);
						byte addr_mid = (byte) (x >> 6);
						CanbusMessage ramReq = new CanbusMessage(0x7e8, new byte[] {(byte) 0x23, (byte) 0x0b, addr_mid, addr_low}, "Get memory bytes");
						
						CanbusResponses cr = canbusRequestAndResponses(3, ramReq,
								new byte[] {(byte) 0x07, (byte) 0x63, addr_mid, addr_low}, 0x7ec, null, 100L);
						String s = String.format("%02x %02x", addr_mid, addr_low);
						//publish("Requesting address 0x0b "+s);
						if (cr.getResult())
						{
							byte[] response = cr.getTargetResponse().getData();
							for (int y=0;y<3;y++)
							{
								ramData.add(Byte.valueOf(response[y+4]));
							}
						}
						else
						{
							throw new Exception("incorrect response to canbus request 0x23");
						}
						
						if (addr_mid != last_mid)
						{
							s = String.format("%02x", last_mid);
							publish("Downloaded 256 ram bytes at 0x0b" + s + "00");
							progressCount += 3;
							setProgress(progressCount);
						}
						
						last_mid = addr_mid;
					}
			
					String s = String.format("%2x", last_mid);
					publish("Downloaded 256 ram bytes at 0x0b" + s + "00\n");				
			
				}
				catch (Exception e)
				{
					e.printStackTrace();
				};
				
				setProgress(100);
				return ramData;
			}
		
			@Override
			protected void process(List<String> progressMessages)
			{
				Iterator<String> it = progressMessages.iterator();
				while (it.hasNext())
				{
					monitorTextArea.append(it.next()+"\n");		
				}
			}
			
			@Override
			protected void done()
			{
				monitorTextArea.append("Finished downloading\n");
				try {
					ArrayList<Byte> retrievedRamData = get();
					monitorTextArea.append("calling saveRamData\n");
					saveRamData(retrievedRamData);
				}
				catch (Exception e) { 
                    e.printStackTrace(); 
                } 
			}
		};
	
		// add listener to update progress bar
		getRamData.addPropertyChangeListener(new PropertyChangeListener() {
	        @Override
	        public void propertyChange(PropertyChangeEvent e) {
	            if ("progress".equals(e.getPropertyName())) {
	                ramDownloadProgressBar.setValue((Integer) e.getNewValue());
	            }
	        }

	    });

		getRamData.execute();

	}
	
	
	// save ram data using b68 format
	//number of bytes is 6 + 1029*7 = 7209
	//prompt for filename to use for data, automatically add .b68 extension
	private void saveRamData(ArrayList<Byte> data)
	{	
		monitorTextArea.append("entering saveRamData\n");

		byte[] saveData = new byte[7029];
		
		//write CPU signature
		saveData[0] = 04;
		saveData[1] = 00;
		saveData[2] = 00;
		saveData[3] = 7;		
		saveData[4] = (byte) 0x54;
		saveData[5] = (byte) 0xaa;
		saveData[6] = (byte) 0x0b;
		saveData[7] = (byte) 0;
		saveData[8] = (byte) 0;
		
		
		for (int x=0;x<7;x++)
		{			
			//write header for block, unless first block which is skipped
			if (x != 0)
			{
				saveData[9+(x*1029)-5] = 0x00;
				saveData[9+(x*1029)-4] = 0x00;
				saveData[9+(x*1029)-3] = (byte) 0x0b;
				saveData[9+(x*1029)-2] = (byte) (x*4);
				saveData[9+(x*1029)-1] = 0x00;
			}

			for (int y=0;y<1024;y++)
			{
				//saveData[9+(x*1029)+y] = data[(x*1024)+y];
				saveData[9+(x*1029)+y] = data.get((x*1024)+y).byteValue();
			}
		}
				
		monitorTextArea.append("Preparing to save ram data\n");
		
		//file is an existing file chooser
		//save the file
		fc.setDialogTitle("Name for the ram data file (no extension) ?");
		
		int userSelection = fc.showSaveDialog(saveRAMButton);
		
		if (userSelection == JFileChooser.APPROVE_OPTION)
		{
			File saveFile = fc.getSelectedFile();
			String filePath = saveFile.getPath();				
						
			saveFile = new File(filePath + ".b68");
				
			try (FileOutputStream out = new FileOutputStream(saveFile))
			{
				out.write(saveData);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

		}
		
		monitorTextArea.append("Ram data saved\n");
		
	}
	
}

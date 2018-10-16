package chatproj.client;

import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.*;
import javax.swing.event.*;

import chatproj.MessagePack;
import chatproj.User;

public class ClientUI extends JFrame {
	private static final long serialVersionUID = -9175621375633179585L;

	static Color unSelectedColor = new Color(238, 238, 238);
	static Color selectedColor = Color.LIGHT_GRAY;
	static Color messageComeColor = Color.ORANGE;
	static int maxFriendNum = 200;
	
	private String defaultRightBanner;

	private final int userID;
	private final String userName;
	private final User user;

	final ReentrantLock lock = new ReentrantLock();
	final Condition cond = lock.newCondition();
	
	private final BlockingQueue<MessagePack> sendQueue;
	private final BlockingQueue<MessagePack> getQueue;

	private JButton[] friendButtons;
	private StringBuffer[] conversations;
	private int[] friendIDs;
	private String[] friendNames;
	private User[] userFriends;
	private JPanel friendList;
	private GridLayout listLayout;
	private JLabel rightBanner;
	private JTextArea dialogue;
	private JTextField sendText;
	private JButton send;
	private JScrollPane scrollDialogue;
	private JScrollBar dialogueScrollBar;
	private FriendListener friendListener;
	private EnterKeyListener keyListener;
	private int friendCount;
	private volatile int selectedFriend;
	
	{
		this.setIconImage(Toolkit.getDefaultToolkit().getImage("app_icon.png"));
	}

	ClientUI(String title, User user, BlockingQueue<MessagePack> sendQueue, BlockingQueue<MessagePack> getQueue, String APP_NAME) {
		super(title);
		userID = user.ID;
		userName = new String(user.Name);
		this.sendQueue = sendQueue;
		this.getQueue = getQueue;
		this.user = user;
		this.defaultRightBanner = APP_NAME;
	}

	public static ClientUI getClient(User user, User[] userFriends, BlockingQueue<MessagePack> sendQueue, BlockingQueue<MessagePack> getQueue, String APP_NAME) {
		if (user == null || sendQueue == null || getQueue == null) {
			return null;
		}
		ClientUI ui = new ClientUI(APP_NAME + " - " + user.Name, user, sendQueue, getQueue, APP_NAME);
		ui.initial(userFriends);
		new Thread(ui.new GetMessage()).start();
		return ui;
	}

	private void initial(User[] userFriends) {
		this.setSize(500, 400);
		this.setLocation(150, 250);
		this.addWindowListener(new MyWindowListener());
		Container con = this.getContentPane();
		con.setLayout(new BorderLayout());

		friendButtons = new JButton[maxFriendNum];
		conversations = new StringBuffer[maxFriendNum];
		friendIDs = new int[maxFriendNum];
		friendNames = new String[maxFriendNum];
		this.userFriends = new User[maxFriendNum];
		friendListener = new FriendListener();
		selectedFriend = -1;

		JPanel left = new JPanel(new BorderLayout());
		left.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));
		JLabel leftBanner = new JLabel("Contact");
		leftBanner.setPreferredSize(new Dimension(90, 30));
		leftBanner.setHorizontalAlignment(SwingConstants.CENTER);
		left.add(leftBanner, BorderLayout.NORTH);
		listLayout = new GridLayout(0, 1);
		friendList = new JPanel(listLayout);

		JPanel scrollInner = new JPanel(new BorderLayout());
		JPanel blank = new JPanel();
		scrollInner.add(friendList, BorderLayout.NORTH);
		scrollInner.add(blank);
		JScrollPane scrollList = new JScrollPane(scrollInner);
		scrollList.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		left.add(scrollList, BorderLayout.CENTER);

		JPanel right = new JPanel(new BorderLayout());
		rightBanner = new JLabel(defaultRightBanner);
		rightBanner.setPreferredSize(new Dimension(90, 30));
		Font font = new Font(null, 0, 20);
		rightBanner.setFont(font);
		rightBanner.setHorizontalAlignment(SwingConstants.CENTER);
		right.add(rightBanner, BorderLayout.NORTH);
		dialogue = new JTextArea();
		dialogue.setEditable(false);
		dialogue.setLineWrap(true);
		dialogue.setVisible(false);
		dialogue.setMargin(new Insets(3, 3, 3, 3));
		scrollDialogue = new JScrollPane(dialogue);
		dialogueScrollBar =  scrollDialogue.getVerticalScrollBar();
		right.add(scrollDialogue, BorderLayout.CENTER);
		JPanel rightBottom = new JPanel(new BorderLayout());
		sendText = new JTextField();
		sendText.getDocument().addDocumentListener(new SendTextListener());
		keyListener = new EnterKeyListener();
		sendText.addKeyListener(keyListener);

		rightBottom.add(sendText, BorderLayout.CENTER);
		send = new JButton("Send");
		send.addActionListener(new SendListener());
		send.setFocusable(false);
		send.setEnabled(false);
		rightBottom.add(send, BorderLayout.EAST);
		right.add(rightBottom, BorderLayout.SOUTH);

		if (userFriends != null) {
			for (int i = 0; i < userFriends.length; i++) {
				addFriendButton(userFriends[i]);
			}
			listLayout.setRows(friendCount);
		} else {
			friendCount = 0;
		}

		con.add(left, BorderLayout.WEST);
		con.add(right, BorderLayout.CENTER);
	}

	private boolean addFriendButton(User userFriend) {
		friendButtons[friendCount] = new JButton(userFriend.Name);
		friendButtons[friendCount].setPreferredSize(new Dimension(90, 30));
		friendButtons[friendCount].setActionCommand(friendCount + " " + userFriend.ID);
		friendButtons[friendCount].addActionListener(friendListener);
		friendButtons[friendCount].setFocusable(false);
		friendButtons[friendCount].setBackground(unSelectedColor);
		friendList.add(friendButtons[friendCount]);
		friendIDs[friendCount] = userFriend.ID;
		friendNames[friendCount] = userFriend.Name;
		userFriends[friendCount] = userFriend;
		conversations[friendCount++] = new StringBuffer();
		return true;
	}

	private class MyWindowListener implements WindowListener {
		@Override
		public void windowClosing(WindowEvent e) {
			ClientMain.clientExit();
		}

		@Override
		public void windowClosed(WindowEvent e) {}
		@Override
		public void windowIconified(WindowEvent e) {}
		@Override
		public void windowDeiconified(WindowEvent e) {}
		@Override
		public void windowActivated(WindowEvent e) {}
		@Override
		public void windowDeactivated(WindowEvent e) {}
		@Override
		public void windowOpened(WindowEvent e) {}
	}

	class SendListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			int friendNum = Integer.parseInt(e.getActionCommand());
			sendMessage(friendNum);
		}
	}

	private class FriendListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			String[] strs = e.getActionCommand().split(" ");
			int friendNum = Integer.parseInt(strs[0]);
			friendButtons[friendNum].setBackground(selectedColor);
			lock.lock();
			send.setActionCommand(String.valueOf(friendNum));
			if (selectedFriend == friendNum) {
				selectedFriend = -1;
				friendButtons[friendNum].setBackground(unSelectedColor);
				dialogue.setText("");
				dialogue.setVisible(false);
				rightBanner.setText(defaultRightBanner);
				send.setEnabled(false);
				keyListener.selectedF = -1;
			} else {
				if (selectedFriend == -1) {
					dialogue.setVisible(true);
				} else {
					friendButtons[selectedFriend].setBackground(unSelectedColor);
				}
				if (!sendText.getText().equals("")) {
					send.setEnabled(true);
				}
				keyListener.selectedF = friendNum;
				selectedFriend = friendNum;
				rightBanner.setText(friendButtons[friendNum].getText());
				if (conversations[friendNum].length() > 0) {
					dialogue.setText(conversations[friendNum].substring(0, conversations[friendNum].length() - 1));
				} else {
					dialogue.setText("");
				}
				dialogue.validate();
				scrollDialogue.validate();
				dialogueScrollBar.setValue(dialogueScrollBar.getMaximum());
				
			}
			lock.unlock();
		}
	}

	private class SendTextListener implements DocumentListener {
		@Override
		public void insertUpdate(DocumentEvent e) {
			if (!send.isEnabled() && selectedFriend != -1) {
				send.setEnabled(true);
			}
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			if (send.isEnabled() && sendText.getText().equals("")) {
				send.setEnabled(false);
			}
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
		}
	}

	private class EnterKeyListener implements KeyListener {
		int selectedF = -1;
		@Override
		public void keyTyped(KeyEvent e) {
			if (e.getKeyChar() == '\n') {
				if (selectedF != -1 && !sendText.getText().equals("")) {
					sendMessage(selectedF);
				}
			}
		}

		@Override
		public void keyPressed(KeyEvent e) {
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}

	}

	private void newMessage(MessagePack pack) {
		if (pack.to.ID != this.userID) {
			return;
		}
		int fromID = pack.from.ID;
		for (int i = 0; i < friendCount; i++) {
			if (fromID == friendIDs[i]) {
				String str = formatMessage(friendNames[i], pack.messageText);
				conversations[i].append(str);
				lock.lock();
				if (selectedFriend != -1 && fromID == friendIDs[selectedFriend]) {
					if (dialogue.getText().equals("")) {
						dialogue.append(str.substring(0, str.length() - 1));
					} else {
						dialogue.append("\n" + str.substring(0, str.length() - 1));
					}
					dialogue.validate();
					scrollDialogue.validate();
					dialogueScrollBar.setValue(dialogueScrollBar.getMaximum());
				} else {
					friendButtons[i].setBackground(messageComeColor);
				}
				lock.unlock();
				if (!this.isActive()) {
					this.setVisible(true);
				}
				break;
			}
		}
	}
	
	private void sendMessage(int friendNum) {
		lock.lock();
		String str = formatMessage(userName, sendText.getText());
		conversations[friendNum].append(str);
		if (dialogue.getText().equals("")) {
			dialogue.append(str.substring(0, str.length() - 1));
		} else {
			dialogue.append("\n" + str.substring(0, str.length() - 1));
		}
		dialogue.validate();
		scrollDialogue.validate();
		dialogueScrollBar.setValue(dialogueScrollBar.getMaximum());
		str = sendText.getText();
		sendText.setText("");
		lock.unlock();
		try {
			sendQueue.put(MessagePack.getMessagePack(user, userFriends[friendNum], str));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static String formatMessage(String userName, String message) {
		return "[" + userName  + "] :\n" + message + "\n";
	}
	
	private class GetMessage implements Runnable {
		@Override
		public void run() {
			while(true) {
				try {
					newMessage(getQueue.take());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}		
		}
		
	}
}

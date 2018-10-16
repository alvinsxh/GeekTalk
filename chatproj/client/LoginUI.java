package chatproj.client;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.concurrent.BlockingQueue;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class LoginUI extends JFrame {
	private static final long serialVersionUID = 1L;
	private JTextField userID;
	private JTextField password;
	private JTextField serverIPPort;
	private JButton loginButton;
	private BlockingQueue<String> getQueue;
	private BlockingQueue<String> sendQueue;
	private String APP_NAME;

	{
		this.setIconImage(Toolkit.getDefaultToolkit().getImage("app_icon.png"));
		
		int width = 300;
		int height = 210;
		this.setSize(width, height);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
		int winWidth = (int) screensize.getWidth();
		int winHeight = (int) screensize.getHeight();
		this.setLocation((winWidth - width) / 2, (winHeight - height) / 2);

		Container con = this.getContentPane();
		con.setLayout(new FlowLayout());

		userID = new JTextField();
		password = new JTextField();
		serverIPPort = new JTextField();
		loginButton = new JButton("Login");
		loginButton.addActionListener(new LoginListener());

		JPanel topPanel = new JPanel();
		topPanel.setPreferredSize(new Dimension(350, 5));
		con.add(topPanel);

		JPanel panel = new JPanel(new GridLayout(3, 1, 4, 4));
		JPanel p1 = new JPanel(new FlowLayout());
		JLabel l1 = new JLabel("ID:", JLabel.CENTER);
		l1.setPreferredSize(new Dimension(80, 28));
		p1.add(l1);
		userID.setPreferredSize(new Dimension(180, 28));
		p1.add(userID);
		panel.add(p1);

		JPanel p2 = new JPanel(new FlowLayout());
		JLabel l2 = new JLabel("Password:", JLabel.CENTER);
		l2.setPreferredSize(new Dimension(80, 28));
		p2.add(l2);
		password.setPreferredSize(new Dimension(180, 28));
		p2.add(password);
		panel.add(p2);

		JPanel p3 = new JPanel(new FlowLayout());
		JLabel l3 = new JLabel("Server:", JLabel.CENTER);
		l3.setPreferredSize(new Dimension(80, 28));
		p3.add(l3);
		serverIPPort.setPreferredSize(new Dimension(180, 28));
		p3.add(serverIPPort);
		panel.add(p3);
		panel.setPreferredSize(new Dimension(350, 100));
		con.add(panel);

		JPanel bottomPanel = new JPanel();
		loginButton.setFocusable(false);
		bottomPanel.add(loginButton);
		bottomPanel.setPreferredSize(new Dimension(350, 50));
		con.add(bottomPanel);
		
		userID.addKeyListener(new EnterKeyListener());
		password.addKeyListener(new EnterKeyListener());
		serverIPPort.addKeyListener(new EnterKeyListener());
	}

	public LoginUI(BlockingQueue<String> sendQueue, BlockingQueue<String> getQueue, String SERVER_IP, int SERVER_PORT, String APP_NAME) {
		super(APP_NAME);
		this.APP_NAME = APP_NAME;
		this.sendQueue = sendQueue;
		this.getQueue = getQueue;
		serverIPPort.setText(SERVER_IP + ":" + SERVER_PORT);
	}

	private class LoginListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			loginButton.validate();
			String str = null;
			try {
				sendQueue.put(userID.getText());
				sendQueue.put(password.getText());
				sendQueue.put(serverIPPort.getText());
				str = getQueue.take();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			if (!str.equals("true")) {
				JOptionPane.showMessageDialog(null, str, APP_NAME, 0, null);
			}
		}
	}
	
	private class EnterKeyListener implements KeyListener {
		@Override
		public void keyTyped(KeyEvent e) {
			if (e.getKeyChar() == '\n') {
				loginButton.validate();
				String str = null;
				try {
					sendQueue.put(userID.getText());
					sendQueue.put(password.getText());
					sendQueue.put(serverIPPort.getText());
					str = getQueue.take();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				if (!str.equals("true")) {
					JOptionPane.showMessageDialog(null, str, APP_NAME, 0, null);
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
}

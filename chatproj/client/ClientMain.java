package chatproj.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

import javax.swing.JOptionPane;

import chatproj.EncryptedPack;
import chatproj.MessagePack;
import chatproj.User;
import static chatproj.RequestHeader.*;

public class ClientMain {
	private static String userIDStr;

	private static User user;
	private static User[] userFriends;

	private static volatile int conTimeCount;
	private static final int maxTimeCount = 15;
	private static final int conCheckInterval = 2000;
	private static String ServerIP;
	private static int ServerPort;

	static Socket clientSocket;

	static BlockingQueue<MessagePack> getQueue = new ArrayBlockingQueue<>(100);
	static BlockingQueue<MessagePack> sendQueue = new ArrayBlockingQueue<>(100);
	
	private static int SERVER_PORT;
	private static String SERVER_IP;
	private static String APP_NAME;
	
	static {
		Properties prop = new Properties(); 
        try {   
    		prop.load(new FileInputStream("client.properties"));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Error: Component lost", APP_NAME, 0, null);
			e.printStackTrace();
		}
        SERVER_PORT = Integer.parseInt(prop.getProperty("serverport"));
        SERVER_IP = prop.getProperty("serverip");
		APP_NAME = prop.getProperty("appname");
	}

	public static void main(String[] args) {
		BlockingQueue<String> loginGetQueue = new SynchronousQueue<String>();
		BlockingQueue<String> loginSendQueue = new SynchronousQueue<String>();
		LoginUI loginUI = new LoginUI(loginGetQueue, loginSendQueue, SERVER_IP, SERVER_PORT, APP_NAME);
		loginUI.setVisible(true);
		String userid = null, password = null, ipport = null;

		while (true) {
			try {
				userid = loginGetQueue.take();
				password = loginGetQueue.take();
				ipport = loginGetQueue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (!userid.matches("\\d{" + ID_LEN + "}") || userid.matches("0.*")) {
				try {
					loginSendQueue.put("Wrong ID format");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			userIDStr = userid;
			
			if (!password.matches("\\w{6,20}")) {
				try {
					loginSendQueue.put("Wrong password format");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}

			if (ipport.equals("local")) {
				ServerIP = "127.0.0.1";
				ServerPort = SERVER_PORT;
			} else if (!ipport.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{1,5}")) {
				try {
					loginSendQueue.put("Wrong IP format");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			} else {
				String[] ipportarray = ipport.split(":");
				String[] ips = ipportarray[0].split("\\.");
				boolean flag = false;
				for (String ipsingle : ips) {
					if (Integer.valueOf(ipsingle) > 255) {
						try {
							loginSendQueue.put("Wrong IP format");
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						flag = true;
						continue;
					}
					if (flag) {
						continue;
					}
				}
				if (Integer.parseInt(ipportarray[1]) > 65535) {
					try {
						loginSendQueue.put("Wrong IP format");
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					continue;
				}
				ServerIP = ipportarray[0];
				ServerPort = Integer.parseInt(ipportarray[1]);
			}

			try {
				MessageDigest md = MessageDigest.getInstance("MD5");
				md.update(password.getBytes());
				password = new BigInteger(1, md.digest()).toString(16);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
			
			String str = initial(password);
			if (str.equals("true")) {
				try {
					loginSendQueue.put("true");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				loginUI.setVisible(false);
				break;
			}
			try {
				loginSendQueue.put(str);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		ClientUI c = ClientUI.getClient(user, userFriends, sendQueue, getQueue, APP_NAME);
		new Thread(new GetMessage()).start();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			showErrorDialogue("Error: Internal error");
			clientExit();
		}
		new Thread(new InformReady()).start();
		new Thread(new SendMessage()).start();
		c.setVisible(true);
	}

	private static String initial(String password) {
		try {
			clientSocket = new Socket();
			clientSocket.connect(new InetSocketAddress(ServerIP, ServerPort), 20000);
			InputStream is = clientSocket.getInputStream();
			clientSocket.getOutputStream().write(new String(CON_SERVER + "" + userIDStr + password).getBytes());
			if (is.read() != USER_INFO) {
				throw new IOException("Error: Server error");
			}
			int status = is.read();
			if (status == USER_INFO_ERROR) {
				throw new IOException("Error: Server error");
			} else if (status == USER_INFO_REPEAT) {
				throw new IOException("Already logged in");
			} else if (status == USER_INFO_FAIL) {
				throw new IOException("Wrong ID or password");
			}
			ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
			user = (User) ois.readObject();
			userFriends = (User[]) ois.readObject();
		} catch (IOException e) {
			String str;
			if (e.getMessage().equals("Connection refused: connect") || e.getMessage().equals("connect timed out")) {
				str = "Error: Cannot conncet to server";
			} else {
				str = e.getMessage();
			}
			return str;
		} catch (ClassNotFoundException e) {
			return "Error: Component lost";
		}
		return "true";
	}

	private static class GetMessage implements Runnable {
		@Override
		public void run() {
			while (true) {
				try {
					InputStream is = clientSocket.getInputStream();
					int head = is.read();
					if (head != MESSAGE) {
						throw new IOException();
					}
					ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
					getQueue.put(EncryptedPack.decrypt((EncryptedPack) ois.readObject()));
				} catch (ClassNotFoundException e) {
					showErrorDialogue("Error: Component lost");
					clientExit();
				} catch (UnsupportedEncodingException e) {
					showErrorDialogue("Error: GBK charset not supported");
					clientExit();
				} catch (IOException | InterruptedException e) {
				}
			}
		}
	}

	private static class SendMessage implements Runnable {
		@Override
		public void run() {
			MessagePack pack = null;
			while (true) {
				try {
					pack = sendQueue.take();
				} catch (InterruptedException e) {
				}
				try {
					clientSocket.getOutputStream().write(MESSAGE);
					ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
					oos.writeObject(EncryptedPack.encrypt(pack));
				} catch (UnsupportedEncodingException e) {
					showErrorDialogue("Error: GBK charset not supported");
					clientExit();
				} catch (IOException e) {
					showErrorDialogue("Error: Cannot connect to server");
					clientExit();
				}
			}
		}
	}

	private static class InformReady implements Runnable {
		@Override
		public void run() {
			while (true) {
				if (--conTimeCount <= 0) {
					try {
						clientSocket.getOutputStream().write(CLIENT_ONLINE);
						clientSocket.getOutputStream().write(userIDStr.getBytes());
						conTimeCount = maxTimeCount;
					} catch (IOException e1) {
						showErrorDialogue("Error: Cannot connect to server");
						clientExit();
					}
				}
				try {
					Thread.sleep(conCheckInterval);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void showErrorDialogue(String msg) {
		JOptionPane.showMessageDialog(null, msg, APP_NAME, 0, null);
	}

	public static void clientExit() {
		try {
			clientSocket.getOutputStream().write(DISCON_SERVER);
			clientSocket.getOutputStream().write(userIDStr.getBytes());
			clientSocket.close();
		} catch (IOException e) {
		}
		System.exit(0);
	}
}

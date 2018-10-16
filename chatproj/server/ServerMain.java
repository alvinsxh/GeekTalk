package chatproj.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import chatproj.EncryptedPack;

import static chatproj.RequestHeader.*;

public class ServerMain {
	private static ServerSocket serverGet;
	private static Map<Integer, OnlineUser> onlineUserCollection;
	private static ExecutorService pool;
	private static Map<Integer, LinkedList<EncryptedPack>> unsendPack;

	private static ReentrantLock lock = new ReentrantLock(true);
	
	private static SqlQuery sqlQuery;

	private static long maxNoResponseMillis = 1000 * 60 * 2;
	private static long checkOnlineInteval = 1000 * 60 * 5;
	private static final int READ_WAIT = 100;
	private static final int LISTEN_INTERVAL = 10;
	
	private static final int SERVER_PORT;
	private static final String DB_NAME;
	private static final String UNSER_INFO_TABLE;
	private static final String RELATION_INFO_TABLE;
	private static final String SQL_TYPE;
	
	static {
		Properties prop = new Properties(); 
        try {   
    		prop.load(new FileInputStream("server.properties"));
		} catch (IOException e) {
			System.out.println("Property file lost");
			e.printStackTrace();
		}
		SERVER_PORT = Integer.parseInt(prop.getProperty("serverport"));
		SQL_TYPE = prop.getProperty("sqltype");
		DB_NAME = prop.getProperty("dbname");
		UNSER_INFO_TABLE = prop.getProperty("userinfotable");
		RELATION_INFO_TABLE = prop.getProperty("relationinfotable");
	}

	public static void main(String[] args) throws IOException {
		sqlQuery = new SqlQuery(DB_NAME, UNSER_INFO_TABLE, RELATION_INFO_TABLE, SQL_TYPE);
		serverGet = new ServerSocket(SERVER_PORT);
		onlineUserCollection = new HashMap<>();
		unsendPack = new Hashtable<>();
		pool = Executors.newWorkStealingPool();
		new Thread(new CheckOnline()).start();
		new Thread(new LoopListen()).start();
		try {
			while (true) {
				Socket s = serverGet.accept();
				char c = (char) s.getInputStream().read();
				switch (c) {
				case CON_SERVER: {
					pool.submit(new ResponseCon(new OnlineUser(s)));
					break;
				} default: {
					s.close();
					break;
				}
				}
			}
		} finally {
			pool.shutdown();
			serverGet.close();
		}
	}

	private static class ResponseSend extends Response {
		ResponseSend(OnlineUser onlineUser) {
			super(onlineUser);
		}

		@Override
		public void run() {
			EncryptedPack pack = null;
			try {
				ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
				pack = (EncryptedPack) ois.readObject();
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}

			if (pack != null) {
				if (!sendPack(pack)) {
					addUnSend(pack);
					onlineStatusRemove(pack.to.ID);
				}
			}
			onlineUser.taskFinish = true;
		}
	}

	private static void addUnSend(EncryptedPack pack) {
		if (!unsendPack.containsKey(pack.to.ID)) {
			unsendPack.put(pack.to.ID, new LinkedList<EncryptedPack>());
		}
		unsendPack.get(pack.to.ID).add(pack);
	}

	private static boolean sendPack(EncryptedPack pack) {
		boolean sent = false;
		if (onlineUserCollection.containsKey(pack.to.ID)) {
			try {
				Socket s = onlineUserCollection.get(pack.to.ID).s;
				s.getOutputStream().write(MESSAGE);
				ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
				oos.writeObject(pack);
				sent = true;
			} catch (IOException e) {
			}
		}
		return sent;
	}

	private static class ResponseReady extends Response {
		ResponseReady(OnlineUser onlineUser) {
			super(onlineUser);
		}

		@Override
		public void run() {
			int userID = 0;
			try {
				char[] IDChar = new char[ID_LEN];
				Reader r = new InputStreamReader(s.getInputStream());
				r.read(IDChar);
				userID = Integer.valueOf(new String(IDChar));
				onlineStatusUpdate(userID);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (userID != 0 && unsendPack.containsKey(userID)) {
				EncryptedPack pack;
				LinkedList<EncryptedPack> packList = unsendPack.get(userID);
				while ((pack = packList.peek()) != null) {
					if (sendPack(pack)) {
						packList.poll();
					} else {
						break;
					}
				}
				if (packList.size() == 0) {
					unsendPack.remove(userID);
				}
			}
			onlineUser.taskFinish = true;
		}
	}

	private static void onlineStatusUpdate(int userID) {
		lock.lock();
		long now = System.currentTimeMillis();
		OnlineUser onlineUser = onlineUserCollection.get(userID);
		if (onlineUser != null && onlineUser.lastReportTime < now) {
			onlineUser.lastReportTime = now;
		}
		lock.unlock();
	}

	private static void onlineStatusRemove(int userID) {
		lock.lock();
		if (onlineUserCollection.containsKey(userID)) {
			tryCloseSocket(onlineUserCollection.get(userID).s);
		}
		onlineUserCollection.remove(userID);
		lock.unlock();
	}
	
	private static void tryCloseSocket(Socket s) {
		try {
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static class ResponseDiscon extends Response {
		ResponseDiscon(OnlineUser onlineUser) {
			super(onlineUser);
		}

		@Override
		public void run() {
			try {
				char[] IDChar = new char[ID_LEN];
				Reader r = new InputStreamReader(s.getInputStream());
				r.read(IDChar);
				int userID = Integer.valueOf(new String(IDChar));
				onlineStatusRemove(userID);
				System.out.println("User: " + userID + " offline");
			} catch (IOException e) {
				e.printStackTrace();
			}
			onlineUser.taskFinish = true;
		}

	}

	private static class ResponseCon extends Response {
		ResponseCon(OnlineUser onlineUser) {
			super(onlineUser);
		}

		@Override
		public void run() {
			Object[] o = null;
			int userID = 0;
			try {
				char[] IDChar = new char[ID_LEN];
				char[] passwordChar = new char[32];
				Reader r = new InputStreamReader(s.getInputStream());
				r.read(IDChar);
				r.read(passwordChar);
				userID = Integer.valueOf(new String(IDChar));				
				o = sqlQuery.Query(userID, new String(passwordChar));
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				PrintStream ps = new PrintStream(s.getOutputStream());
				ps.print(USER_INFO);
				if (o == null) {
					ps.print(USER_INFO_ERROR);
				} else if (o[0] == null) {
					ps.print(USER_INFO_FAIL);
				} else {
					if (onlineUserCollection.containsKey(userID)) {
						ps.print(USER_INFO_REPEAT);
						return;
					}
					ps.print(USER_INFO_SUCCESS);
					ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
					oos.writeObject(o[0]);
					oos.writeObject(o[1]);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			onlineUser.taskFinish = true;
			if (o != null && o[0] != null) {
				lock.lock();
				onlineUser.lastReportTime = System.currentTimeMillis();
				onlineUserCollection.put(userID, onlineUser);
				lock.unlock();
				System.out.println("New client log in");
			}
		}
	}

	private static abstract class Response implements Runnable {
		protected Socket s;
		protected OnlineUser onlineUser;

		Response(OnlineUser onlineUser) {
			this.s = onlineUser.s;
			this.onlineUser = onlineUser;
		}
	}

	private static class CheckOnline implements Runnable {
		@Override
		public void run() {
			while (true) {
				lock.lock();
				long now = System.currentTimeMillis();
				
				Iterator<HashMap.Entry<Integer, OnlineUser>> it = onlineUserCollection.entrySet().iterator();
				HashMap.Entry<Integer, OnlineUser> entry;
				while(it.hasNext()) {
					entry = it.next();
					if (now - entry.getValue().lastReportTime > maxNoResponseMillis) {
						tryCloseSocket(onlineUserCollection.get(entry.getKey()).s);
						it.remove();
					}
				}
				
				/*
				LinkedList<Integer> removeList = new LinkedList<Integer>();
				for (int userID : onlineUserTime.keySet()) {
					if (now - onlineUserTime.get(userID) > maxNoResponseMillis) {
						removeList.add(userID);
					}
				}
				for (int userID : removeList) {
					onlineStatusRemove(userID);
				}
				*/
				
				lock.unlock();
				try {
					Thread.sleep(checkOnlineInteval);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static class LoopListen implements Runnable {
		@Override
		public void run() {
			OnlineUser onlineUser;
			Socket s;
			int head;
			Object[] userIDs = new Object[0];
			int userID, i;
			while (true) {
				lock.lock();
				if (!onlineUserCollection.isEmpty()) {
					userIDs = onlineUserCollection.keySet().toArray();
				}
				lock.unlock();

				for (i = 0; i < userIDs.length; i++) {
					head = -2;
					userID = (int) userIDs[i];
					if ((onlineUser = onlineUserCollection.get(userID)) == null) {
						continue;
					}
					if (!onlineUser.taskFinish) {
						continue;
					}
					s = onlineUser.s;
					try {
						s.setSoTimeout(READ_WAIT);
						head = s.getInputStream().read();
						s.setSoTimeout(Integer.MAX_VALUE);
					} catch (IOException e) {
						if (!(e instanceof SocketTimeoutException)) {
							head = 0;
							e.printStackTrace();
						}
					}
					if (head == 0 || head == -1) {
						onlineStatusRemove(userID);
					} else if (head == -2) {
						continue;
					} else {
						switch ((char) head) {
						case CON_SERVER: {
							onlineUser.taskFinish = false;
							pool.submit(new ResponseCon(onlineUser));
							break;
						}
						case DISCON_SERVER: {
							onlineUser.taskFinish = false;
							pool.submit(new ResponseDiscon(onlineUser));
							break;
						}
						case CLIENT_ONLINE: {
							onlineUser.taskFinish = false;
							pool.submit(new ResponseReady(onlineUser));
							break;
						}
						case MESSAGE: {
							onlineUser.taskFinish = false;
							pool.submit(new ResponseSend(onlineUser));
							break;
						}
						}
					}
				}
				try {
					Thread.sleep(LISTEN_INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private static class OnlineUser {
		Socket s;
		long lastReportTime;
		volatile boolean taskFinish = true;

		OnlineUser(Socket s) {
			this.s = s;
		}
	}
}
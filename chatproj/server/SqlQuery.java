package chatproj.server;

import java.sql.SQLException;

import javax.sql.rowset.CachedRowSet;

import chatproj.User;
import chatproj.server.SqlIO;

public class SqlQuery {
	private final String UNSER_INFO_TABLE;
	private final String RELATION_INFO_TABLE;
	private final SqlIO SQL_CONN;
	
	public SqlQuery(String DB_NAME, String UNSER_INFO_TABLE, String RELATION_INFO_TABLE, String SQL_TYPE) {
		this.UNSER_INFO_TABLE = UNSER_INFO_TABLE;
		this.RELATION_INFO_TABLE = RELATION_INFO_TABLE;
		this.SQL_CONN = new SqlIO(DB_NAME, SQL_TYPE);
	}
	
	private User queryUser(int userID, String password) throws SQLException {
		String sql = "select user_name from " + UNSER_INFO_TABLE + " where user_id = " + userID + " and user_password = " + "'" +password + "'";
		CachedRowSet userInfo = SQL_CONN.DB_Cached_Select(sql);
		if (userInfo == null || userInfo.size() == 0) {
			return null;
		} else {
			userInfo.next();
			String userName = userInfo.getString(1);
			return new User(userID, userName);
		}
	}
	
	private User[] queryFriends(int userID) throws SQLException {
		String sql ="select u.user_id, u.user_name " +
					"from " + UNSER_INFO_TABLE + " u, " +
					"(" +
					"select case " +
					"when relation_user1 = " + userID + " then relation_user2 " +
					"when relation_user2 = " + userID + " then relation_user1 " +
					"end fid " +
					"from " + RELATION_INFO_TABLE + " " +
					"where relation_user1 = " + userID + " or relation_user2 = " + userID +
					") r " +
					"where u.user_id = r.fid";
		CachedRowSet relationInfo = SQL_CONN.DB_Cached_Select(sql);
		User[] friends;
		if (relationInfo == null || relationInfo.size() < 1) {
			friends =  new User[0];
		} else {
			friends = new User[relationInfo.size()];
			while (relationInfo.next()) {
				friends[relationInfo.getRow() - 1] = new User(relationInfo.getInt(1), relationInfo.getString(2));
			}
			SQL_CONN.DBClose();
		}
		return friends;
	}
	
	public Object[] Query(int userID, String password) {
		User user = null;
		User[] friends = null;
		try {
			user = queryUser(userID, password);
		} catch (SQLException e) {
			return null;
		}
		if (user == null) {
			return new Object[]{null};
		}
		try {
			friends = queryFriends(userID);
		} catch (SQLException e) {
			return null;
		}
		return new Object[]{user, friends};
	}
}

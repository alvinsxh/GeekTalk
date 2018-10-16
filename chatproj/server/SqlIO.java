package chatproj.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;

public class SqlIO {
	/**
	 * 数据库连接Connection变量，初始值为null。
	 */
	private Connection conn = null;
	/**
	 * RP-AEC数据库表名，初始值为tazpga。
	 */
	private String dbName = "oop";

	/**
	 * 带参数构造函数。
	 * 
	 * @param dbName
	 *            要连接的数据库的名称。
	 */
	
	private String SQL_TYPE;
	
	public SqlIO(String dbName, String SQL_TYPE) {
		this.dbName = dbName;
		this.SQL_TYPE = SQL_TYPE;
	}

	/**
	 * Getter函数，返回dbName的值。
	 * 
	 * @return String: 返回dbName的值。
	 */
	public String GetDBName() {
		return this.dbName;
	}

	/**
	 * Setter函数，设置dbName的值。
	 * 
	 * @param dbName
	 *            String类型的数据库表名。
	 */
	public void SetDBName(String dbName) {
		this.dbName = dbName;
	}

	/**
	 * 设置数据库连接。driver = "com.mysql.jdbc.Driver";url =
	 * "jdbc:mysql://localhost:3306/"+DBName; user = "root";password = "1234";
	 */
	private void dbConn() {
		String url = null;
		String user = null;
		String password = null;

		switch (SQL_TYPE) {
		case "sqlserver":
			url = "jdbc:sqlserver://localhost:1433;DatabaseName=" + dbName + ";";
			user = "sa";
			password = "123456";
			break;
		case "mysql":
			url = "jdbc:mysql://localhost:3306/"+dbName+"?useSSL=false&serverTimezone=UTC";
			user = "root";
			password = "123456";
			break;
		}

		try {
			this.conn = DriverManager.getConnection(url, user, password);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 关闭数据库。
	 */
	public void DBClose() {
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 执行Update语句。
	 * 
	 * @param sql
	 *            String类型的Update语句。
	 * @return int: 影响的行数。
	 */
	public int DB_Update(String sql) {
		int rtn = 0;
		this.dbConn();
		try {
			Statement statement = conn.createStatement();
			rtn = statement.executeUpdate(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rtn;
	}

	/**
	 * 执行Select语句。
	 * 
	 * @param sql
	 *            String类型的Select语句。
	 * @return ResultSet: Select语句返回的行的结果集。
	 */
	public ResultSet DB_Select(String sql) {
		ResultSet rtn = null;
		this.dbConn();
		try {
			Statement statement = conn.createStatement();
			rtn = statement.executeQuery(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rtn;
	}

	/**
	 * 执行Insert语句。
	 * 
	 * @param sql
	 *            String类型的Insert语句。
	 * @return int: 影响的行数。
	 */
	public int DB_Insert(String sql) {
		int rtn = 0;
		this.dbConn();
		Statement statement;
		try {
			statement = conn.createStatement();
			rtn = statement.executeUpdate(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rtn;
	}

	/**
	 * 执行其他指定的语句。
	 * 
	 * @param sql
	 *            String类型的其他语句。
	 * @return int: 影响的行数。
	 */
	public int DB_Other(String sql) {
		int rtn = 0;
		this.dbConn();
		Statement statement;
		try {
			statement = conn.createStatement();
			rtn = statement.executeUpdate(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rtn;
	}

	public CachedRowSet DB_Cached_Select(String sql) {
		this.dbConn();
		try {
			Statement s = conn.createStatement();
			ResultSet rs = s.executeQuery(sql);
			RowSetFactory rsf = RowSetProvider.newFactory();
			CachedRowSet crs = rsf.createCachedRowSet();
			crs.populate(rs);
			this.DBClose();
			return crs;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}

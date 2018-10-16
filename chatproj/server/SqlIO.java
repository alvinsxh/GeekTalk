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
	 * ���ݿ�����Connection��������ʼֵΪnull��
	 */
	private Connection conn = null;
	/**
	 * RP-AEC���ݿ��������ʼֵΪtazpga��
	 */
	private String dbName = "oop";

	/**
	 * ���������캯����
	 * 
	 * @param dbName
	 *            Ҫ���ӵ����ݿ�����ơ�
	 */
	
	private String SQL_TYPE;
	
	public SqlIO(String dbName, String SQL_TYPE) {
		this.dbName = dbName;
		this.SQL_TYPE = SQL_TYPE;
	}

	/**
	 * Getter����������dbName��ֵ��
	 * 
	 * @return String: ����dbName��ֵ��
	 */
	public String GetDBName() {
		return this.dbName;
	}

	/**
	 * Setter����������dbName��ֵ��
	 * 
	 * @param dbName
	 *            String���͵����ݿ������
	 */
	public void SetDBName(String dbName) {
		this.dbName = dbName;
	}

	/**
	 * �������ݿ����ӡ�driver = "com.mysql.jdbc.Driver";url =
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
	 * �ر����ݿ⡣
	 */
	public void DBClose() {
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * ִ��Update��䡣
	 * 
	 * @param sql
	 *            String���͵�Update��䡣
	 * @return int: Ӱ���������
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
	 * ִ��Select��䡣
	 * 
	 * @param sql
	 *            String���͵�Select��䡣
	 * @return ResultSet: Select��䷵�ص��еĽ������
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
	 * ִ��Insert��䡣
	 * 
	 * @param sql
	 *            String���͵�Insert��䡣
	 * @return int: Ӱ���������
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
	 * ִ������ָ������䡣
	 * 
	 * @param sql
	 *            String���͵�������䡣
	 * @return int: Ӱ���������
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

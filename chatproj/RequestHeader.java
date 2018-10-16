package chatproj;

public class RequestHeader {
	public static final char CON_SERVER = 17;
	public static final char DISCON_SERVER = 18;
	public static final char CLIENT_ONLINE = 19;
	public static final char USER_INFO = 20;
	public static final char MESSAGE = 21;
	
	public static final int ID_LEN = 6;
	
	public static final char USER_INFO_ERROR = '0';
	public static final char USER_INFO_REPEAT = '1';
	public static final char USER_INFO_FAIL = '2';
	public static final char USER_INFO_SUCCESS = '3';
	public static final String CHAR_SET = "GBK";
}

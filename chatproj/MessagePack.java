package chatproj;

public class MessagePack implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
	public final User from;
	public final User to;
	public final String messageText;
	
	public static MessagePack getMessagePack(User from, User to, String messageText) {
		if (from == null || to == null || messageText == null) {
			return null;
		}
		return new MessagePack(from, to, messageText);
	}
	
	private MessagePack(User from, User to, String messageText) {
		this.from = from;
		this.to = to;
		this.messageText = messageText;
	}
}

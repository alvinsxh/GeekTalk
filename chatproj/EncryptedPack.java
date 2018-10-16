package chatproj;

import static chatproj.RequestHeader.CHAR_SET;

import java.io.UnsupportedEncodingException;

public class EncryptedPack implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	public User from;
	public User to;
	private byte[] message;

	private EncryptedPack(User from, User to, byte[] message) {
		this.from = from;
		this.to = to;
		this.message = message;
	}

	public static EncryptedPack encrypt(MessagePack unencryptedPack) throws UnsupportedEncodingException {
		if (unencryptedPack == null) {
			return null;
		}
		byte[] originMessage = null;
		originMessage = unencryptedPack.messageText.getBytes(CHAR_SET);
		byte[] modifiedMessage = new byte[originMessage.length];
		int low = 0, high = originMessage.length - 1;
		while (low <= high) {
			if (low == high) {
				modifiedMessage[low] = (byte) -(originMessage[low] - 30);
			} else if (low % 2 == 0) {
				modifiedMessage[low] = (byte) -(originMessage[low] - 10);
				modifiedMessage[high] = (byte) -(originMessage[high] - 20);
			} else {
				modifiedMessage[low] = (byte) -(originMessage[high] - 5);
				modifiedMessage[high] = (byte) -(originMessage[low] - 15);
			}
			low++;
			high--;
		}
		return new EncryptedPack(unencryptedPack.from, unencryptedPack.to, modifiedMessage);
	}

	public static MessagePack decrypt(EncryptedPack encryptedPack) throws UnsupportedEncodingException {
		if (encryptedPack == null) {
			return null;
		}
		byte[] modifiedMessage = encryptedPack.message;
		byte[] originMessage = new byte[modifiedMessage.length];
		int low = 0, high = modifiedMessage.length - 1;
		while (low <= high) {
			if (low == high) {
				originMessage[low] = (byte) (-modifiedMessage[low] + 30);
			} else if (low % 2 == 0) {
				originMessage[low] = (byte) (-modifiedMessage[low] + 10);
				originMessage[high] = (byte) (-modifiedMessage[high] + 20);
			} else {
				originMessage[low] = (byte) (-modifiedMessage[high] + 15);
				originMessage[high] = (byte) (-modifiedMessage[low] + 5);
			}
			low++;
			high--;
		}
		return MessagePack.getMessagePack(encryptedPack.from, encryptedPack.to, new String(originMessage, CHAR_SET));
	}
}

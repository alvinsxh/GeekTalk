package chatproj;

public class User implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
	public final int ID;
	public final String Name;
	
	public User(int ID, String Name) {
		this.ID = ID;
		this.Name = new String(Name);
	}
	
	@Override
	public boolean equals(Object o) {
		if (! (o instanceof User)) {
			return false;
		}
		if (((User)o).ID == this.ID) {
			return true;
		} else {
			return false;
		}
	}
}

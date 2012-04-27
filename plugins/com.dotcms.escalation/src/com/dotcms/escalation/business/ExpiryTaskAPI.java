package	com.dotcms.escalation.business;


public abstract class ExpiryTaskAPI {

	private static ExpiryTaskAPI ExpiryTaskAPI = null;

	public static ExpiryTaskAPI getInstance() {
		if (ExpiryTaskAPI == null) {
			ExpiryTaskAPI = ExpiryTaskAPIImpl.getInstance();
		}
		return ExpiryTaskAPI;
	}

	public abstract void escaleTask(String taskId, String roleId)
			throws Exception;

}

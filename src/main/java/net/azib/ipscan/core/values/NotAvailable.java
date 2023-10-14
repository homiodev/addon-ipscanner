package net.azib.ipscan.core.values;

/**
 * The value for displaying in the result list, meaning that the actual value is unknown,
 * because it wasn't resolved successfully.
 *
 * @author Anton Keks
 */
public class NotAvailable {
	public static final NotAvailable VALUE = new NotAvailable();

	private NotAvailable() {}

	public String toString() {
		return "N/A";
	}
}

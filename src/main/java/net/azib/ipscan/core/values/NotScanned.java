package net.azib.ipscan.core.values;

/**
 * The value for displaying in the result list, meaning that the actual value is unknown,
 * because it was not scanned.
 *
 * @author Anton Keks
 */
public class NotScanned {
	public static final NotScanned VALUE = new NotScanned();

	private NotScanned() {}

	/**
	 * Displays a user-friendly text string :-)
	 */
	public String toString() {
		return "N/S";
	}
}

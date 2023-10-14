/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
 */
package net.azib.ipscan.fetchers;

import net.azib.ipscan.IPScannerService;
import net.azib.ipscan.core.ScanningSubject;
import net.azib.ipscan.core.values.NotAvailable;
import net.azib.ipscan.core.values.NotScanned;
import org.jetbrains.annotations.NotNull;

public interface Fetcher extends Cloneable {

	@NotNull IPScannerService.Fetcher getFetcherID();

	default String getId() {
		return getFetcherID().name();
	}

	default String getName() {
		return getId();
	}

	/**
	 * @return full name to be displayed in the result table column.
	 * It may contain a suffix useful to inform users about the fetcher's preferences.
	 */
	default String getFullName() {
		return getName();
	}

	/**
	 * Does the actual fetching.
	 * @param subject the scanning subject, containing an IP address
	 * @return the fetched data (a String in most cases), null in case of any error.
	 * Special values may also be returned, such as {@link NotAvailable} or {@link NotScanned}
	 */
	Object scan(ScanningSubject subject);

	/**
	 * Called before scanning has started to do any intialization stuff
	 */
	default void init() {
	}

	/**
	 * Called after the scanning has been completed to do any cleanup needed
	 */
	default void cleanup() {
	}
}

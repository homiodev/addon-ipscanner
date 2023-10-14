/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */
package net.azib.ipscan.core;

import lombok.Getter;
import net.azib.ipscan.fetchers.Fetcher;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * The holder of scanning result for a single IP address.
 *
 * @author Anton Keks
 */
public class ScanningResult {

	public enum ResultType {
		UNKNOWN, DEAD, ALIVE, WITH_PORTS
	}

	/** The scanned IP address */
	@Getter private final InetAddress address;
	/** Scanning results, result of each Fetcher is an element */
	private Object[] values;

	@Getter private ResultType type;

	/** reference to the containing list */
	ScanningResultList resultList;

	public ScanningResult(InetAddress address, int numberOfFetchers) {
		this.address = address;
		values = new Object[numberOfFetchers];
		values[0] = address.getHostAddress();
		type = ResultType.UNKNOWN;
	}

	/**
	 * @return the scanning results as an unmodifiable List, result of each Fetcher is an element
	 */
	public List<Object> getValues() {
		return Arrays.asList(values);
	}

	public void setValues(Object[] values) {
		this.values = values;
	}

	/**
	 * Sets scanning result type
	 */
	public void setType(ResultType type) {
		this.type = type;
	}

	public void setValue(int fetcherIndex, Object value) {
		values[fetcherIndex] = value;
	}

	/**
	 * Returns all results for this IP address as a String.
	 * This is used in showing the IP Details dialog box.
	 * @return human-friendly text representation of results
	 */
	public String toString() {
		// cross-platform newline :-)
		String newLine = System.getProperty("line.separator");

		StringBuilder details = new StringBuilder(1024);
		Iterator<?> iterator = getValues().iterator();
		List<Fetcher> fetchers = resultList.getFetchers();
		for (int i = 0; iterator.hasNext(); i++) {
			String fetcherName = fetchers.get(i).getName();
			details.append(fetcherName).append(":\t");
			Object value = iterator.next();
			details.append(value != null ? value : "");
			details.append(newLine);
		}
		return details.toString();
	}
}

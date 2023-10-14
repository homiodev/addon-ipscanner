/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */
package net.azib.ipscan.core;

import static java.util.Collections.emptyList;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import net.azib.ipscan.IPScannerService;
import net.azib.ipscan.fetchers.Fetcher;
import net.azib.ipscan.fetchers.FetcherRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * The holder of scanning results.
 *
 * @author Anton Keks
 */
public class ScanningResultList {
	private static final int RESULT_LIST_INITIAL_SIZE = 1024;

	private final FetcherRegistry fetcherRegistry;
	// selected fetchers are cached here, because they may be changed in the registry already
	private List<Fetcher> selectedFetchers = emptyList();

	private final @Getter List<ScanningResult> resultList = new ArrayList<>(RESULT_LIST_INITIAL_SIZE);
	private final Map<InetAddress, Integer> resultIndexes = new HashMap<>(RESULT_LIST_INITIAL_SIZE);

	public ScanningResultList(FetcherRegistry fetcherRegistry) {
		this.fetcherRegistry = fetcherRegistry;
	}

	/**
	 * @return selected fetchers that were used for the last scan
	 * Note: they may be different from {@link FetcherRegistry#getSelectedFetchers()}
	 */
	public List<Fetcher> getFetchers() {
		return selectedFetchers;
	}

	/**
	 * Creates the new results holder for particular address or returns an existing one.
	 * @return pre-initialized empty ScanningResult
	 */
	public synchronized ScanningResult createResult(InetAddress address) {
		Integer index = resultIndexes.get(address);
		if (index == null) {
			return new ScanningResult(address, fetcherRegistry.getSelectedFetchers().size());
		}
		return resultList.get(index);
	}

	/**
	 * Registers the provided results holder at the specified index in this list.
	 * This index will later be used to retrieve the result when redrawing items.
	 * TODO: index parameter is not really needed here - add method with index will not work with sparse lists anyway
	 */
	public synchronized void registerAtIndex(int index, ScanningResult result) {
		if (resultIndexes.put(result.getAddress(), index) != null)
			throw new IllegalStateException(result.getAddress() + " is already registered in the list");

		result.resultList = this;
		resultList.add(index, result);
	}

	/**
	 * @return true if the provided result holder exists in the list.
	 */
	public synchronized boolean isRegistered(ScanningResult result) {
		return resultIndexes.containsKey(result.getAddress());
	}

	/**
	 * Updates statistics.
	 * @return the index of the result in the list, if it is registered.
	 */
	public synchronized int update(ScanningResult result) {
		return resultIndexes.get(result.getAddress());
	}

	/**
	 * Clears previous scanning results, prepares for a new scan.
	 */
	public synchronized void clear() {
		// clear the results
		resultList.clear();
		resultIndexes.clear();
	}

	public synchronized void initNewScan() {
		// reload currently selected fetchers
		selectedFetchers = new ArrayList<>(fetcherRegistry.getSelectedFetchers());
	}

	/**
	 * @return an Iterator of scanning results
	 *
	 * Note: the returned Iterator is not synchronized
	 */
	public synchronized @NotNull Iterator<ScanningResult> iterator() {
		return resultList.iterator();
	}

	public int getFetcherIndex(IPScannerService.Fetcher fetcher) {
		int index = 0;
		for (Fetcher item : getFetchers()) {
			if (fetcher == item.getFetcherID()) return index;
			index++;
		}
		return -1;
	}
}

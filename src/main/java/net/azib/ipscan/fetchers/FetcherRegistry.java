/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */
package net.azib.ipscan.fetchers;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.azib.ipscan.IPScannerService;

/**
 * Fetcher registry singleton class.
 * It registers both plugins and builtins.
 *
 * @author Anton Keks
 */
public class FetcherRegistry {

	/** All available Fetcher implementations, List of Fetcher instances */
	private final Map<IPScannerService.Fetcher, Fetcher> registeredFetchers;

	/** Selected for scanning Fetcher implementations, keys are fetcher labels, values are Fetcher instances */
	private Map<IPScannerService.Fetcher, Fetcher> selectedFetchers;

	public FetcherRegistry(List<Fetcher> fetchers) {
		registeredFetchers = fetchers.stream().collect(Collectors.toMap(Fetcher::getFetcherID, s -> s));
	}

	public void setFetchers(Set<IPScannerService.Fetcher> fetchers) {
		selectedFetchers = new LinkedHashMap<>();
		for (IPScannerService.Fetcher fetcher : fetchers) {
			selectedFetchers.put(fetcher, registeredFetchers.get(fetcher));
		}
	}

	public Collection<Fetcher> getSelectedFetchers() {
		return selectedFetchers.values();
	}
}

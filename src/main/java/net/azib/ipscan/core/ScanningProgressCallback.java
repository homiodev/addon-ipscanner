/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */
package net.azib.ipscan.core;

import org.jetbrains.annotations.Nullable;

/**
 * This callback is called on scanning state updates.
 *
 * @author Anton Keks
 */
public interface ScanningProgressCallback {

	/**
	 * This method is called on scanning progress updates.
	 * There are no guarantees that this method is called on every
	 * scanning iteration.
	 *
	 * @param message currently scanned IP address
	 * @param runningThreads number of currently running threads
	 * @param percentageComplete value from 0 to 100, showing how much work
	 * 		is already done.
	 */
	void updateProgress(@Nullable String message, int runningThreads, double percentageComplete);
}

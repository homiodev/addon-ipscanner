/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */
package net.azib.ipscan;

import org.apache.commons.lang3.SystemUtils;
import org.homio.hquery.ProgressBar;

/**
 * Scanner configuration holder.
 *
 * @author Anton Keks
 */
public class ScannerConfig {

	public String portString = "80,443,8080";
	public int maxThreads = 30;
	public int threadDelay = 20;
	public boolean scanDeadHosts = false;
	public String selectedPinger = SystemUtils.IS_OS_WINDOWS ? "pinger.windows" : "pinger.icmp";
	public int pingTimeout = 2000;
	public int pingCount = 3;
	public boolean skipBroadcastAddresses = true;
	public int portTimeout = 2000;
	public boolean adaptPortTimeout = true;
	public int minPortTimeout = 100;
	public boolean useRequestedPorts = true;
	public ProgressBar progressBar;
}

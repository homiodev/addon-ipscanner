/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */
package net.azib.ipscan.feeders;

import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.Getter;
import net.azib.ipscan.ScannerConfig;
import net.azib.ipscan.core.ScanningSubject;
import net.azib.ipscan.util.InetAddressUtils;
import org.savarese.vserv.tcpip.OctetConverter;

/**
 * IP Range Feeder.
 * It contains the starting and ending values, which
 * are then iterated sequentially.
 *
 * @author Anton Keks
 */
public class RangeFeeder {

	private final ScannerConfig scannerConfig;
	private InetAddress startIP;
	private InetAddress endIP;
	private InetAddress originalEndIP;
	private InetAddress currentIP;
	boolean isReverse;

	@Getter double percentageComplete;
	double percentageIncrement;

	public String getName() {
		return getId(); // Labels.getLabel(getId());
	}

	@Override
	public String toString() {
		return getName() + ": " + getInfo();
	}

	public String getId() {
		return "feeder.range";
	}

	public RangeFeeder(String startIP, String endIP, ScannerConfig scannerConfig) {
		this.scannerConfig = scannerConfig;
		try {
			this.startIP = this.currentIP = InetAddress.getByName(startIP);
			this.endIP = this.originalEndIP = InetAddress.getByName(endIP);
			this.isReverse = false;
		}
		catch (UnknownHostException e) {
			throw new FeederException("malformedIP");
		}
		if (this.startIP.getClass() != this.endIP.getClass()) {
			throw new FeederException("differentProtocols");
		}
		if (InetAddressUtils.greaterThan(this.startIP, this.endIP)) {
			this.isReverse = true;
			this.endIP = InetAddressUtils.decrement(InetAddressUtils.decrement(this.endIP));
		}
		initPercentageIncrement();
		this.endIP = InetAddressUtils.increment(this.endIP);
	}

	/**
	 * Initalizes fields, used for computation of percentage of completion.
	 */
	private void initPercentageIncrement() {
		byte[] endAddress = this.endIP.getAddress();
		long rawEndIP = OctetConverter.octetsToInt(endAddress, endAddress.length - 4);
		long rawStartIP = OctetConverter.octetsToInt(this.startIP.getAddress(), endAddress.length - 4);
		// make 32-bit unsigned values
		rawEndIP = rawEndIP >= 0 ? rawEndIP : rawEndIP + Integer.MAX_VALUE;
		rawStartIP = rawStartIP >= 0 ? rawStartIP : rawStartIP + Integer.MAX_VALUE;
		// compute 1% of the whole range
		percentageIncrement = Math.abs(100.0 / (rawEndIP - rawStartIP + 1));
		percentageComplete = 0;
	}

	public boolean hasNext() {
		// equals() is faster than greaterThan()
		return !currentIP.equals(endIP);
	}

	public ScanningSubject next() {
		percentageComplete += percentageIncrement;
		InetAddress prevIP = this.currentIP;
		if (this.isReverse) {
			this.currentIP = InetAddressUtils.decrement(prevIP);
		} else {
			this.currentIP = InetAddressUtils.increment(prevIP);
		}
		return new ScanningSubject(prevIP, scannerConfig);
	}

	public String getInfo() {
		// let's return the range
		return startIP.getHostAddress() + " - " + originalEndIP.getHostAddress();
	}
}

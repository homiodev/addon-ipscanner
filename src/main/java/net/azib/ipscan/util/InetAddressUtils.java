/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */
package net.azib.ipscan.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.extern.log4j.Log4j2;

/**
 * This class provides various utility static methods,
 * useful for transforming InetAddress objects.
 *
 * @author Anton Keks
 */
@Log4j2
public class InetAddressUtils {

	/**
	 * Compares two IP addresses.
	 * @return true in case inetAddress1 is greater than inetAddress2
	 */
	public static boolean greaterThan(InetAddress inetAddress1, InetAddress inetAddress2) {
		byte[] address1 = inetAddress1.getAddress();
		byte[] address2 = inetAddress2.getAddress();
		for (int i = 0; i < address1.length; i++) {
			if ((address1[i] & 0xFF) > (address2[i] & 0xFF))
				return true;
			else
			if ((address1[i] & 0xFF) < (address2[i] & 0xFF))
				break;
		}
		return false;
	}

	public static InetAddress increment(InetAddress address) {
		return modifyInetAddress(address, true);
	}

	public static InetAddress decrement(InetAddress address) {
		return modifyInetAddress(address, false);
	}

	/**
	 * Increments or decrements an IP address by 1.
	 * @return incremented/decremented IP address
	 */
	private static InetAddress modifyInetAddress(InetAddress address, boolean isIncrement) {
		try {
			byte[] newAddress = address.getAddress();
			for (int i = newAddress.length-1; i >= 0; i--) {
				if (isIncrement) {
					if (++newAddress[i] != 0x00) {
						break;
					}
				} else {
					if (--newAddress[i] != 0x00) {
						break;
					}
				}

			}
			return InetAddress.getByAddress(newAddress);
		}
		catch (UnknownHostException e) {
			// this exception is unexpected here
			assert false : e;
			return null;
		}
	}

	/**
	 * Checks whether the passed address is likely either a broadcast or network address
	 */
	public static boolean isLikelyBroadcast(InetAddress address) {
		byte[] bytes = address.getAddress();
		return bytes[bytes.length-1] == 0 || bytes[bytes.length-1] == (byte)0xFF;
	}
}

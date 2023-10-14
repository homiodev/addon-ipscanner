/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */

package net.azib.ipscan.fetchers;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import lombok.extern.log4j.Log4j2;
import net.azib.ipscan.IPScannerService;
import net.azib.ipscan.core.ScanningSubject;
import net.azib.ipscan.util.NetBIOSResolver;
import org.jetbrains.annotations.NotNull;

/**
 * NetBIOSInfoFetcher - gathers NetBIOS info about Windows machines.
 * Provided for feature-compatibility with version 2.x
 *
 * @author Anton Keks
 */
@Log4j2
public class NetBIOSInfoFetcher implements Fetcher {
	public NetBIOSInfoFetcher() {}

    @Override
    public @NotNull IPScannerService.Fetcher getFetcherID() {
        return IPScannerService.Fetcher.NetBIOSInfo;
    }

	public Object scan(ScanningSubject subject) {
		try (NetBIOSResolver netbios = new NetBIOSResolver(subject.getAdaptedPortTimeout())) {
			String[] names = netbios.resolve(subject.getAddress());
			if (names == null) return null;

			String computerName = names[0];
			String userName = names[1];
			String groupName = names[2];
			String macAddress = names[3];

			return (groupName != null ? groupName + "\\" : "") +
					(userName != null ? userName + "@" : "") +
					(computerName != null ? computerName + ' ' : "") + '[' + macAddress + ']';
		}
		catch (SocketTimeoutException e) {
			// this is not a derivative of SocketException
			return null;
		}
		catch (SocketException e) {
			// this includes PortUnreachableException
			return null;
		}
		catch (Exception e) {
			// bugs?
			log.warn(e);
			return null;
		}
	}
}

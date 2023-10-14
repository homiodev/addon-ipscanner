/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */
package net.azib.ipscan.core.values;

/**
 * IntegerWithUnit - an Integer value together with a unit, e.g. "10 ms".
 * TODO: IntegerWithUnitTest
 *
 * @author Anton Keks
 */
public class IntegerWithUnit {

	private final int value;
	private final String unitLabel;

	public IntegerWithUnit(int value, String unitLabel) {
		this.value = value;
		this.unitLabel = unitLabel;
	}

	public String toString() {
		return value + unitLabel;/*Labels.getLabel("unit." + unitLabel);*/
	}

	public int hashCode() {
		return value;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof IntegerWithUnit)
			return value == ((IntegerWithUnit) obj).value;
		return false;
	}
}

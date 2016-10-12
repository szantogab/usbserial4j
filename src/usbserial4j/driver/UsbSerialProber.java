package usbserial4j.driver;

/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 * Copyright 2016 Gabor Szanto <szantogab@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbHub;

/**
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class UsbSerialProber {

	private final ProbeTable mProbeTable;

	public UsbSerialProber(ProbeTable probeTable) {
		mProbeTable = probeTable;
	}

	public static UsbSerialProber getDefaultProber() {
		return new UsbSerialProber(getDefaultProbeTable());
	}

	public static ProbeTable getDefaultProbeTable() {
		final ProbeTable probeTable = new ProbeTable();
		//TODO add these drivers 
		//probeTable.addDriver(CdcAcmSerialDriver.class);
		//probeTable.addDriver(Cp21xxSerialDriver.class);
		//probeTable.addDriver(FtdiSerialDriver.class);
		probeTable.addDriver(ProlificSerialDriver.class);
		//probeTable.addDriver(Ch34xSerialDriver.class);
		return probeTable;
	}

	/**
	 * Finds and builds all possible {@link UsbSerialDriver UsbSerialDrivers}
	 * from the currently-attached {@link UsbDevice} hierarchy. This method does
	 * not require permission from the Android USB system, since it does not
	 * open any of the devices.
	 *
	 * @param usbManager
	 * @return a list, possibly empty, of all compatible drivers
	 */
	public List<UsbSerialDriver> findAllDrivers(final UsbHub usbHub) {
		final List<UsbSerialDriver> result = new ArrayList<UsbSerialDriver>();

		for (final UsbDevice usbDevice : getDevices(usbHub)) {
			final UsbSerialDriver driver = probeDevice(usbDevice);
			if (driver != null) {
				result.add(driver);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public List<UsbDevice> getDevices(UsbHub hub) {
		List<UsbDevice> usbDevices = new ArrayList<>();

		for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices()) {
			usbDevices.add(device);
			
			if (device.isUsbHub()) {
				List<UsbDevice> devices = getDevices((UsbHub) device);
				if (devices.size() > 0)
					usbDevices.addAll(devices);
			}
		}
		return usbDevices;
	}

	@SuppressWarnings("unchecked")
	public List<UsbDevice> findDevice(UsbHub hub, short vendorId, short productId) {
		List<UsbDevice> usbDevices = new ArrayList<>();

		for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices()) {
			UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
			if (desc.idVendor() == vendorId && desc.idProduct() == productId)
				usbDevices.add(device);
			if (device.isUsbHub()) {
				List<UsbDevice> devices = findDevice((UsbHub) device, vendorId, productId);
				if (devices.size() > 0)
					usbDevices.addAll(devices);
			}
		}
		return usbDevices;
	}

	/**
	 * Probes a single device for a compatible driver.
	 * 
	 * @param usbDevice
	 *            the usb device to probe
	 * @return a new {@link UsbSerialDriver} compatible with this device, or
	 *         {@code null} if none available.
	 */
	public UsbSerialDriver probeDevice(final UsbDevice usbDevice) {
		final int vendorId = usbDevice.getUsbDeviceDescriptor().idVendor();
		final int productId = usbDevice.getUsbDeviceDescriptor().idProduct();

		final Class<? extends UsbSerialDriver> driverClass = mProbeTable.findDriver(vendorId, productId);
		if (driverClass != null) {
			final UsbSerialDriver driver;
			try {
				final Constructor<? extends UsbSerialDriver> ctor = driverClass.getConstructor(UsbDevice.class);
				driver = ctor.newInstance(usbDevice);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			} catch (IllegalArgumentException e) {
				throw new RuntimeException(e);
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
			return driver;
		}
		return null;
	}

}

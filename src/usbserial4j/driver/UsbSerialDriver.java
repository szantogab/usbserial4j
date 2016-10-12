package usbserial4j.driver;

import java.util.List;

import javax.usb.UsbDevice;

/**
 *
 * @author Gabor Szanto (szantogab@gmail.com)
 */
public interface UsbSerialDriver {

	/**
	 * Returns the raw {@link UsbDevice} backing this port.
	 *
	 * @return the device
	 */
	public UsbDevice getDevice();

	/**
	 * Returns all available ports for this device. This list must have at least
	 * one entry.
	 *
	 * @return the ports
	 */
	public List<UsbSerialPort> getPorts();
}
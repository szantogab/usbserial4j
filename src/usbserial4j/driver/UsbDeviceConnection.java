package usbserial4j.driver;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.usb.UsbConfiguration;
import javax.usb.UsbControlIrp;
import javax.usb.UsbDevice;
import javax.usb.UsbDisconnectedException;
import javax.usb.UsbEndpoint;
import javax.usb.UsbException;
import javax.usb.UsbInterface;
import javax.usb.UsbInterfacePolicy;
import javax.usb.UsbIrp;
import javax.usb.UsbNotActiveException;
import javax.usb.UsbNotClaimedException;
import javax.usb.UsbNotOpenException;
import javax.usb.UsbPipe;

public class UsbDeviceConnection {
	private final UsbDevice mDevice;
	private List<UsbEndpoint> openedEndpoints = new ArrayList<>();

	public UsbDeviceConnection(UsbDevice device) {
		mDevice = device;
	}

	/**
	 * Releases all system resources related to the device. Once the object is
	 * closed it cannot be used again. The client must call
	 * {@link UsbManager#openDevice} again to retrieve a new instance to
	 * reestablish communication with the device.
	 */
	@SuppressWarnings("unchecked")
	public void close() {
		try {
			for (UsbEndpoint endpoint : openedEndpoints) {
				// Close all opened endpoints
				UsbPipe usbPipe = endpoint.getUsbPipe();
				if (usbPipe.isOpen())
					usbPipe.close();
			}

			UsbConfiguration usbConfiguration = mDevice.getActiveUsbConfiguration();
			if (usbConfiguration != null) {
				List<UsbInterface> ifaces = ((List<UsbInterface>) usbConfiguration.getUsbInterfaces());
				for (UsbInterface usbInterface : ifaces) {
					if (usbInterface.isClaimed())
						usbInterface.release();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Claims exclusive access to a {@link android.hardware.usb.UsbInterface}.
	 * This must be done before sending or receiving data on any
	 * {@link android.hardware.usb.UsbEndpoint}s belonging to the interface.
	 *
	 * @param intf
	 *            the interface to claim
	 * @param force
	 *            true to disconnect kernel driver if necessary
	 * @return true if the interface was successfully claimed
	 */
	public boolean claimInterface(UsbInterface intf, final boolean force) {
		try {
			intf.claim(new UsbInterfacePolicy() {

				@Override
				public boolean forceClaim(UsbInterface usbInterface) {
					return force;
				}
			});
			return true;
		} catch (UsbException | UsbNotActiveException | UsbDisconnectedException e) {
			return false;
		}
	}

	/**
	 * Returns the raw USB descriptors for the device. This can be used to
	 * access descriptors not supported directly via the higher level APIs.
	 *
	 * @return raw USB descriptors
	 */
	public byte[] getRawDescriptors() {
		byte[] descriptor = new byte[18];
		descriptor[0] = (byte) descriptor.length;
		descriptor[1] = mDevice.getUsbDeviceDescriptor().bDescriptorType();
		descriptor[2] = (byte) mDevice.getUsbDeviceDescriptor().bcdUSB();
		descriptor[4] = mDevice.getUsbDeviceDescriptor().bDeviceClass();
		descriptor[5] = mDevice.getUsbDeviceDescriptor().bDeviceSubClass();
		descriptor[6] = mDevice.getUsbDeviceDescriptor().bDeviceProtocol();
		descriptor[7] = mDevice.getUsbDeviceDescriptor().bMaxPacketSize0();
		descriptor[8] = (byte) mDevice.getUsbDeviceDescriptor().idVendor();
		descriptor[10] = (byte) mDevice.getUsbDeviceDescriptor().idProduct();
		descriptor[12] = (byte) mDevice.getUsbDeviceDescriptor().bcdDevice();
		descriptor[14] = (byte) mDevice.getUsbDeviceDescriptor().iManufacturer();
		descriptor[15] = (byte) mDevice.getUsbDeviceDescriptor().iProduct();
		descriptor[16] = (byte) mDevice.getUsbDeviceDescriptor().iSerialNumber();
		descriptor[17] = (byte) mDevice.getUsbDeviceDescriptor().bNumConfigurations();
		return descriptor;
	}

	/**
	 * Releases exclusive access to a {@link android.hardware.usb.UsbInterface}.
	 *
	 * @return true if the interface was successfully released
	 */
	public boolean releaseInterface(UsbInterface intf) {
		try {
			intf.release();
			return true;
		} catch (UsbException | UsbNotActiveException | UsbDisconnectedException e) {
			return false;
		}
	}

	/**
	 * Performs a control transaction on endpoint zero for this device. The
	 * direction of the transfer is determined by the request type. If
	 * requestType & {@link UsbConstants#USB_ENDPOINT_DIR_MASK} is
	 * {@link UsbConstants#USB_DIR_OUT}, then the transfer is a write, and if it
	 * is {@link UsbConstants#USB_DIR_IN}, then the transfer is a read.
	 * <p>
	 * This method transfers data starting from index 0 in the buffer. To
	 * specify a different offset, use
	 * {@link #controlTransfer(int, int, int, int, byte[], int, int, int)}.
	 * </p>
	 *
	 * @param requestType
	 *            request type for this transaction
	 * @param request
	 *            request ID for this transaction
	 * @param value
	 *            value field for this transaction
	 * @param index
	 *            index field for this transaction
	 * @param buffer
	 *            buffer for data portion of transaction, or null if no data
	 *            needs to be sent or received
	 * @param length
	 *            the length of the data to send or receive
	 * @param timeout
	 *            in milliseconds
	 * @return length of data transferred (or zero) for success, or negative
	 *         value for failure
	 */
	public int controlTransfer(int requestType, int request, int value, int index, byte[] buffer, int length,
			int timeout) {
		return controlTransfer(requestType, request, value, index, buffer, 0, length, timeout);
	}

	/**
	 * Performs a control transaction on endpoint zero for this device. The
	 * direction of the transfer is determined by the request type. If
	 * requestType & {@link UsbConstants#USB_ENDPOINT_DIR_MASK} is
	 * {@link UsbConstants#USB_DIR_OUT}, then the transfer is a write, and if it
	 * is {@link UsbConstants#USB_DIR_IN}, then the transfer is a read.
	 *
	 * @param requestType
	 *            request type for this transaction
	 * @param request
	 *            request ID for this transaction
	 * @param value
	 *            value field for this transaction
	 * @param index
	 *            index field for this transaction
	 * @param buffer
	 *            buffer for data portion of transaction, or null if no data
	 *            needs to be sent or received
	 * @param offset
	 *            the index of the first byte in the buffer to send or receive
	 * @param length
	 *            the length of the data to send or receive
	 * @param timeout
	 *            in milliseconds
	 * @return length of data transferred (or zero) for success, or negative
	 *         value for failure
	 */
	public int controlTransfer(int requestType, int request, int value, int index, byte[] buffer, int offset,
			int length, int timeout) {
		checkBounds(buffer, offset, length);

		UsbControlIrp irp = mDevice.createUsbControlIrp((byte) requestType, (byte) request, (short) value,
				(short) index);

		if (buffer != null)
			irp.setData(buffer);

		irp.setOffset(offset);
		irp.setLength(length);
		try {
			mDevice.asyncSubmit(irp);
			irp.waitUntilComplete(timeout);
		} catch (IllegalArgumentException | UsbDisconnectedException | UsbException e) {
			return -1;
		}

		return (irp.isUsbException() || !irp.isComplete() ? -1 : irp.getActualLength());
	}

	/**
	 * Performs a bulk transaction on the given endpoint. The direction of the
	 * transfer is determined by the direction of the endpoint.
	 * <p>
	 * This method transfers data starting from index 0 in the buffer. To
	 * specify a different offset, use
	 * {@link #bulkTransfer(UsbEndpoint, byte[], int, int, int)}.
	 * </p>
	 *
	 * @param endpoint
	 *            the endpoint for this transaction
	 * @param buffer
	 *            buffer for data to send or receive
	 * @param length
	 *            the length of the data to send or receive
	 * @param timeout
	 *            in milliseconds
	 * @return length of data transferred (or zero) for success, or negative
	 *         value for failure
	 */
	public int bulkTransfer(UsbEndpoint endpoint, byte[] buffer, int length, int timeout) {
		return bulkTransfer(endpoint, buffer, 0, length, timeout);
	}

	/**
	 * Performs a bulk transaction on the given endpoint. The direction of the
	 * transfer is determined by the direction of the endpoint.
	 *
	 * @param endpoint
	 *            the endpoint for this transaction
	 * @param buffer
	 *            buffer for data to send or receive
	 * @param offset
	 *            the index of the first byte in the buffer to send or receive
	 * @param length
	 *            the length of the data to send or receive
	 * @param timeout
	 *            in milliseconds
	 * @return length of data transferred (or zero) for success, or negative
	 *         value for failure
	 */
	public int bulkTransfer(UsbEndpoint endpoint, byte[] buffer, int offset, int length, int timeout) {
		checkBounds(buffer, offset, length);

		UsbPipe usbPipe = endpoint.getUsbPipe();
		if (!usbPipe.isOpen()) {
			try {
				usbPipe.open();
				
				if (!openedEndpoints.contains(endpoint))
					openedEndpoints.add(endpoint);
			} catch (UsbNotActiveException | UsbNotClaimedException | UsbDisconnectedException | UsbException e) {
				// Failed to open pipe
				return -1;
			}
		}

		UsbIrp irp = usbPipe.createUsbIrp();
		irp.setData(buffer, offset, length);

		try {
			usbPipe.asyncSubmit(irp);
			irp.waitUntilComplete(timeout);
		} catch (UsbNotActiveException | UsbNotOpenException | UsbDisconnectedException | UsbException e) {
			return -1;
		}

		return (irp.isUsbException() || !irp.isComplete() ? -1 : irp.getActualLength());
	}

	/**
	 * Waits for the result of a {@link android.hardware.usb.UsbRequest#queue}
	 * operation Note that this may return requests queued on multiple
	 * {@link android.hardware.usb.UsbEndpoint}s. When multiple endpoints are in
	 * use, {@link android.hardware.usb.UsbRequest#getEndpoint} and
	 * {@link android.hardware.usb.UsbRequest#getClientData} can be useful in
	 * determining how to process the result of this function.
	 *
	 * @return a completed USB request, or null if an error occurred
	 */
	// public UsbRequest requestWait() {
	// UsbRequest request = native_request_wait();
	// if (request != null) {
	// request.dequeue();
	// }
	// return request;
	// }

	/**
	 * Returns the serial number for the device. This will return null if the
	 * device has not been opened.
	 *
	 * @return the device serial number
	 */
	public String getSerial() {
		try {
			return mDevice.getSerialNumberString();
		} catch (UnsupportedEncodingException | UsbDisconnectedException | UsbException e) {
			return null;
		}
	}

	private static void checkBounds(byte[] buffer, int start, int length) {
		final int bufferLength = (buffer != null ? buffer.length : 0);
		if (start < 0 || start + length > bufferLength) {
			throw new IllegalArgumentException("Buffer start or length out of bounds.");
		}
	}
}
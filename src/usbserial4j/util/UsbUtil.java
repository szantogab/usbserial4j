package usbserial4j.util;

import java.util.List;

import javax.usb.UsbConfiguration;
import javax.usb.UsbDevice;
import javax.usb.UsbInterface;

public class UsbUtil {
	@SuppressWarnings("unchecked")
	public static UsbInterface findInterface(UsbDevice usbDevice, byte number)
	{
		for (UsbConfiguration usbConfiguration : (List<UsbConfiguration>)usbDevice.getUsbConfigurations())
		{
			if (usbConfiguration.containsUsbInterface(number))
				return usbConfiguration.getUsbInterface(number);
		}
		
		return null;
	}
}

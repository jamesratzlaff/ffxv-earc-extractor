/**
 * 
 */
package com.ratzlaff.james.util.io;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * @author James Ratzlaff
 *
 */
public class HexPrinter {

	public static void printAsHex(ByteBuffer bb) {
		printAsHex(bb, 0);
	}

	public static void printAsHex(ByteBuffer bb, int offset) {
		printAsHex(bb, 40, offset);
	}

	// works best with size 12 'Terminal' font
	private static void createIndex(int width, int offset) {

		StringBuilder sb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		StringBuilder top = new StringBuilder();
		StringBuilder side_top = new StringBuilder();
		StringBuilder side_value = new StringBuilder();
		StringBuilder sideBottom = new StringBuilder();
		sb.append('Ã');
		sb2.append('³');
		top.append('Ú');
		sideBottom.append('Ì');
		side_top.append('É');
		side_value.append('º');
		for (int i = 0; i < width; i++) {
			sb2.append(fmt((byte) i).toUpperCase());
			sb2.append('³');
			side_value.append(Integer.toHexString(i%16).toUpperCase());
			if (i != 0) {
				sb.append('Å');
				top.append('Â');
				side_top.append('Ñ');
				sideBottom.append('Ø');
			}
//			side_value.append('³');
			top.append("ÄÄ");
			sb.append("ÄÄ");

		}
		
		top.append('¿');
		sb.append('´');
		side_value.append('º');
		sideBottom.append('Ø');
		sideBottom.append('¹');
//		sb2.append(' ');
		side_top.append('Ñ');
		side_top.append('»');
		
		System.out.println(top.append(side_top));
		System.out.println(sb2.append(side_value));
		System.out.println(sb.append(sideBottom));
	}

	public static void printAsHex(ByteBuffer bb, int width, int offset) {
		byte[] bytes = new byte[width];
		int zeros = offset % width;
		Arrays.fill(bytes, 0, zeros, (byte) 0);
		boolean notPadded = zeros == 0 ? false : true;
		createIndex(width, offset);
		while (bb.remaining() > 0) {
			if (bb.remaining() >= width) {
				if (notPadded) {
					bb.get(bytes, zeros, bytes.length - zeros);
					notPadded = false;
				} else {
					bb.get(bytes);
				}
			} else {
				int remaining = bb.remaining();
				bb.get(bytes, 0, remaining);
				for (int i = remaining; i < bytes.length; i++) {
					bytes[i] = 0;
				}
			}
			printHexLine(bytes);
		}
	}

	private static void printHexLine(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length << 2);
		for (int i = 0; i < bytes.length; i++) {

			sb.append(i == 0 ? '³' :i%4==0?'³':'|');

			byte b = bytes[i];
			
			String asStr = fmt(b).toUpperCase();
			sb.append(asStr);
			if(bytes[i]<0x20) {
				bytes[i]=0x20;
			}
		}
		
		String asText = new String(bytes, Charset.forName("ASCII"));

		sb.append("³º").append(asText).append('º');
		System.out.println(sb);
	}

	private static String fmt(byte b) {
		String asHex = Integer.toHexString((b & 0xFF));
		if (asHex.length() < 2) {
			asHex = '0' + asHex;
		}
		if (asHex.length() < 1) {
			asHex = '0' + asHex;
		}

		return asHex;
	}

}

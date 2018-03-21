/**
 * 
 */
package com.ratzlaff.james.arc.earc.obfus;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.ratzlaff.james.arc.earc.EArcHeader;

/**
 * @author James Ratzlaff
 *
 */
public class KeyGen extends AbstractKeyGen {


	public static EntryUnlockKeys createDefaultEntryKeys() {
		return new EntryUnlockKeysImpl();
	}
	
	public KeyGen(long minorKey, long majorKey, long parentKey) {
		super(minorKey, majorKey, parentKey);
//		setMinorKey( minorKey);
//		setMajorKey( majorKey);
//		setParentKey(parentKey);;
		
	}
	public KeyGen(long minorKey, long majorKey, long parentKey, long transientKey) {
		super(minorKey,majorKey,parentKey,transientKey);
	}
	
	
	public KeyGen(long parentKey) {
		this(AbstractKeyContainer.DEFAULT_MINOR_KEY,AbstractKeyContainer.DEFAULT_MAJOR_KEY, parentKey);
	}
	
	public KeyGen(long parentKey, long transientKey) {
		this(AbstractKeyContainer.DEFAULT_MINOR_KEY,AbstractKeyContainer.DEFAULT_MAJOR_KEY, parentKey,  transientKey);
	}

	/* (non-Javadoc)
	 * @see com.ratzlaff.james.arc.earc.obfus.KeyContainer#getParentKey()
	 */
	public long getParentKey() {
		return parentKey;
	}

	/* (non-Javadoc)
	 * @see com.ratzlaff.james.arc.earc.obfus.AbstractKeyGen#setParentKey_(long)
	 */
	protected void setParentKey_(long parentKey) {
		this.parentKey = parentKey;

	}
	
	public long getMinorKey() {
		return minorKey;
	}
	
	public long getMajorKey() {
		return majorKey;
	}

	public void reset() {
		setParentKey(this.parentKey);
	}


	public long getOffsetKey() {
		return offsetKey;
	}

	public long getLengthKey() {
		return lengthKey;
	}

	

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Please specify the path to a .earc file.");
		} else {
			FileChannel fc = null;
			try {
				fc = FileChannel.open(Paths.get(args[0]), StandardOpenOption.READ);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			if (fc != null) {
				FileChannel toUse = fc;
				EArcHeader header = new EArcHeader(() -> toUse);
				System.out.println(header.isObfuscated());
				System.out.println(Long.toHexString(Long.reverseBytes(header.getObfuscationKey())) + " little endian");
				
				EventTrackingKeyGen kg = new EventTrackingKeyGen(header.getObfuscationKey());// 259c8f5dc3b9f8f5
				
				System.out.println(kg);
				kg.setTransientKey(2710198708648671477l);
				System.out.println(kg);
				System.out.println(
						Long.toHexString(kg.getDeobfiscatedLengthInArchiveAndSizeOnDisk(-1462552410,-26329965)));
				kg.setTransientKey(2710188724502312339l);
				System.out.println(
						Long.toHexString(kg.getDeobfiscatedLengthInArchiveAndSizeOnDisk(-545312353, -1096827210)));
				kg.writeEventsTo(System.out);

			}
		}
	}


}

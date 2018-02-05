package com.ratzlaff.james.arc.earc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * 
 * @author James Ratzlaff
 *
 *         The container for header data for .earc files
 *
 */
public class EArcHeader {
	// header
	// 0x00 magic int (always has the value 0x41465243, 'CRAF' or 'FARC')
	// 0x04 version(?) int?(always has the value 196628)
	// 0x08 file_count int
	// 0x0C min_data_block_size? int (always has the value of 512)
	// 0x10 metadata_start? int (always has the value of 64)
	// 0x14 url_tbl_loc int (data://blah/bloh/blee.file)
	// 0x18 path_tbl_loc int (blah/bloh/blee.file)
	// 0x1C data_tbl_loc int (should be power of 2)
	// 0x20 contains_raw? int (appears to be a flag...all files seem to have either
	// 1 or 0 --possibly indicating if it contains uncompressed file data anywhere)
	// 0x24 ? int (always has the value 0x80)
	private static final FileMetadataPointers[] EMPTY_FILEPOINTERS = new FileMetadataPointers[0];

	private final transient Supplier<FileChannel> fileChannelSupplier;
	private final int magic;
	private final int version;
	private final int fileCount;
	private final int minDataBlockSize;
	private final int metadataLocation;
	private final int urlTableLocation;
	private final int pathTableLocation;
	private final int dataTableLocation;
	private final boolean unknownBoolean;
	private final long fileSize;
	private FileMetadataPointers[] metadataPointers;

	public EArcHeader(Supplier<FileChannel> fileChannelSupplier) {
		this.fileChannelSupplier = fileChannelSupplier;

		int magicToUse = -1;
		int versionToUse = -1;
		int fileCountToUse = -1;
		int minDataBlockSizeToUse = -1;
		int metaDataStartToUse = -1;
		int urlTableLocationToUse = -1;
		int pathTableLocationToUse = -1;
		int dataTableLocationToUse = -1;
		long fileSizeToUse = -1;
		boolean unknownBooleanToUse = false;
		FileChannel fc = getFileChannel();
		if (fc != null) {
			ByteBuffer bb = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
			try {
				fc.position(0);
				fileSizeToUse = fc.size();
				if (fileSizeToUse > 40) {
					fc.read(bb);
					bb.flip();
					magicToUse = bb.getInt();
					versionToUse = bb.getInt();
					fileCountToUse = bb.getInt();
					minDataBlockSizeToUse = bb.getInt();
					metaDataStartToUse = bb.getInt();
					urlTableLocationToUse = bb.getInt();
					pathTableLocationToUse = bb.getInt();
					dataTableLocationToUse = bb.getInt();
					unknownBooleanToUse = bb.getInt() != 0;
				}

			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

		this.magic = magicToUse;
		this.version = versionToUse;
		this.fileCount = fileCountToUse;
		this.minDataBlockSize = minDataBlockSizeToUse;
		this.metadataLocation = metaDataStartToUse;
		this.urlTableLocation = urlTableLocationToUse;
		this.pathTableLocation = pathTableLocationToUse;
		this.dataTableLocation = dataTableLocationToUse;
		this.unknownBoolean = unknownBooleanToUse;
		this.fileSize = fileSizeToUse;

	}

	public int getVersion() {
		return version;
	}

	public int getMagic() {
		return magic;
	}

	protected FileChannel getFileChannel() {
		FileChannel result = this.fileChannelSupplier.get();
		if (this.fileChannelSupplier != null) {
			result = this.fileChannelSupplier.get();
		}
		return result;
	}

	/**
	 * 
	 * @return the number of files in this archive
	 */
	public int getFileCount() {
		return fileCount;
	}

	/**
	 * 
	 * @return the minimum partition (in bytes) for data table entries as well as
	 *         the header+metadata
	 */
	public int getMinDataBlockSize() {
		return minDataBlockSize;
	}

	/**
	 * 
	 * @return the offset (from 0x00) in which the metadata entries are located
	 */
	public int getMetadataLocation() {
		return metadataLocation;
	}
	
	public int getMetadataSize() {
		return getUrlTableLocation()-getMetadataLocation();
	}

	/**
	 * 
	 * @return the offset (from 0x00) in which the url (data://) table is located
	 */
	public int getUrlTableLocation() {
		return urlTableLocation;
	}
	
	public int getUrlTableSize() {
		return getPathTableLocation()-getUrlTableLocation();
	}

	/**
	 * 
	 * @return the offset (from 0x00) in which the file path table is located
	 */
	public int getPathTableLocation() {
		return pathTableLocation;
	}
	
	public int getPathTableSize() {
		return getDataTableLocation()-getPathTableLocation();
	}

	/**
	 * 
	 * @return the flag at offset 0x20 (possibly whether or not this archive
	 *         contains raw (uncompressed) data entries???)
	 */
	public boolean getUnknownBoolean() {
		return unknownBoolean;
	}

	/**
	 * 
	 * @return the offset (from 0x00) in which the data table is located
	 */
	public int getDataTableLocation() {
		return dataTableLocation;
	}

	public int getDataTableSize() {
		return getSizeAsInt()-getDataTableLocation();
	}

	public int getSizeAsInt() {
		long size = size();
		int intSize = (int)size; 
		if (size > Integer.MAX_VALUE) {
			System.err.println(String.format(
					"The size of this file (%d bytes) is too large to represent as an integer.%n\tReturning the maximum integer value of %d",
					size, Integer.MAX_VALUE));
			intSize=Integer.MAX_VALUE;
		}
		return intSize;
	}

	public long size() {
		return fileSize;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + dataTableLocation;
		result = prime * result + fileCount;
		result = prime * result + magic;
		result = prime * result + metadataLocation;
		result = prime * result + Arrays.hashCode(metadataPointers);
		result = prime * result + minDataBlockSize;
		result = prime * result + pathTableLocation;
		result = prime * result + (unknownBoolean ? 1231 : 1237);
		result = prime * result + urlTableLocation;
		result = prime * result + version;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EArcHeader other = (EArcHeader) obj;
		if (dataTableLocation != other.dataTableLocation)
			return false;
		if (fileCount != other.fileCount)
			return false;
		if (magic != other.magic)
			return false;
		if (metadataLocation != other.metadataLocation)
			return false;
		if (!Arrays.equals(metadataPointers, other.metadataPointers))
			return false;
		if (minDataBlockSize != other.minDataBlockSize)
			return false;
		if (pathTableLocation != other.pathTableLocation)
			return false;
		if (unknownBoolean != other.unknownBoolean)
			return false;
		if (urlTableLocation != other.urlTableLocation)
			return false;
		if (version != other.version)
			return false;
		return true;
	}

	private String generateFileMetadataList() {
		String result = null;
		if (this.metadataPointers != null) {
			String[] asArray = Arrays.stream(this.metadataPointers).filter(fmp -> fmp != null)
					.map(fmp -> fmp.toString()).toArray(size -> new String[size]);
			String joined = String.join("\n\t", asArray);
			result = "\n\t" + joined;
		}
		return result;

	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("EArcHeader [fileCount=");
		builder.append(fileCount);
		builder.append(", minDataBlockSize=");
		builder.append(minDataBlockSize);
		builder.append(", metadataLocation=");
		builder.append(metadataLocation);
		builder.append(", urlTableLocation=");
		builder.append(urlTableLocation);
		builder.append(", pathTableLocation=");
		builder.append(pathTableLocation);
		builder.append(", dataTableLocation=");
		builder.append(dataTableLocation);
		builder.append(", uknownBoolean=");
		builder.append(unknownBoolean);
		String fileList = generateFileMetadataList();
		if (fileList != null) {
			builder.append(", filemetadata=");
			builder.append(fileList);
			builder.append("\n");
		}
		builder.append("]");
		return builder.toString();
	}

	// private void populateFileMetadataPointers() {
	// FileChannel fc = FileChannel.open(getPath(), StandardOpenOption.READ);
	// fc.map(MapMode.READ_ONLY, getMetadataLocation(), size)
	// Files.newByteChannel(Paths.get(""), StandardOpenOption.READ).
	// }

	private void initializeFilePointerArray() {
		if (metadataPointers == null) {
			int numberOfEntries = getFileCount();
			if (numberOfEntries > -1) {
				metadataPointers = new FileMetadataPointers[numberOfEntries];
			}
		}
	}

	public FileMetadataPointers getFilePointersAt(int index) {
		FileMetadataPointers result = getFilePointersAt(index, null);
		return result;
	}

	public FileMetadataPointers getFilePointersAt(int index, ByteBuffer bb) {
		initializeFilePointerArray();
		FileMetadataPointers result = null;
		if (metadataPointers != null) {
			result = metadataPointers[index];
			if (result == null) {
				try {
					fileChannelSupplier.get().position(getMetadataLocation() + (40 * index));
				} catch (IOException e) {
					e.printStackTrace();
				}
				result = new FileMetadataPointers(fileChannelSupplier, bb);
				metadataPointers[index] = result;
			}
		}
		return result;
	}

	public FileMetadataPointers[] getMetadataPointers() {
		FileMetadataPointers[] result = EMPTY_FILEPOINTERS;
		if (metadataPointers == null) {
			initializeFilePointerArray();
		}
		if (metadataPointers != null) {
			ByteBuffer bb = ByteBuffer.allocateDirect(40).order(ByteOrder.nativeOrder());
			for (int i = 0; i < getFileCount(); i++) {
				getFilePointersAt(i, bb);
			}
			result = metadataPointers;
		}
		return result;

	}

	public void setMetadataPointers(FileMetadataPointers[] metadataPointers) {
		this.metadataPointers = metadataPointers;
	}

}

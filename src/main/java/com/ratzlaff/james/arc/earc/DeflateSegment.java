/**
 * 
 */
package com.ratzlaff.james.arc.earc;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Objects;

/**
 * 
 * 
 * Reflects the structure of a Deflate section within a an earc file.  
 * 
 * @author James Ratzlaff
 *
 */
public class DeflateSegment {
	private static final transient Logger LOG = Logging.getLogger(DeflateSegment.class);
	private static final int INITIALIZATION_BUFFER_SIZE = Integer.BYTES << 1;

	private static class Messages {
		private static final String REL_OFFSET = "has a relative offset of {0} and an absolute offset of {1}";
		private static final String INFLATE_START = "deflate data starts at relative offset {0}";
		private static final String ALIGNMENT_PADDING = "has {0} bytes of padding to align every {1} bytes";
		private static final String COMPRESSED_SIZE = "compressed size of {0} bytes";
		private static final String BUFFER_SIZE = "buffer size of {0} bytes";
		private static final String BUFFER_SUCCESS = String.format("Successfully read %d bytes apply to entities",
				INITIALIZATION_BUFFER_SIZE);
		private static final String NULL_PARAM_MESSAGE = String.format(
				"An DeflateSegmant {0} MUST have a non-null {0} object passed into the constructor.",
				DeflateSegment.class.getSimpleName(), FileMetadataPointers.class.getSimpleName());
	}
	private final FileMetadataPointers parentPointer;
	private final int entryOffset;
	private final int compressedSize;
	private final int bufferSize;
	

	public DeflateSegment(FileMetadataPointers parent) {
		Objects.requireNonNull(parent, Messages.NULL_PARAM_MESSAGE);
		parentPointer = parent;
		
		FileChannel fc = getFileChannel();
		int entryOffsetToUse = (int)(getPosition(fc)-getParentPointer().getDataLocation());
		this.entryOffset=entryOffsetToUse;
		LOG.log(Level.INFO, Messages.REL_OFFSET, getEntryOffset(), getEntryOffset()+getParentPointer().getDataLocation());
		
		ByteBuffer bb = getHeaderByteBuffer(fc);
		
		int compressedSizeToUse = 0;		
		compressedSizeToUse = bb.getInt();
		this.compressedSize = compressedSizeToUse;
		LOG.log(Level.INFO, Messages.COMPRESSED_SIZE, getCompressedSize(),getEntryOffset());
		
		int bufferSizeToUse = 0;
		bufferSizeToUse = bb.getInt();
		this.bufferSize = bufferSizeToUse;
		LOG.log(Level.INFO, Messages.BUFFER_SIZE, getBufferSize());
		incrementPosition(fc,getCompressedSize());
		incrementPosition(fc, getEndPadding());

		LOG.log(Level.INFO, Messages.INFLATE_START, getDeflateDataOffset());
		LOG.log(Level.INFO, Messages.ALIGNMENT_PADDING,getEndPadding(),Integer.BYTES);
		

	}
	
	

	private static FileChannel incrementPosition(FileChannel fc, int delta) {
		return setPosition(fc, delta+(fc != null ? getPosition(fc): 0));
	}
	public int getDeflateDataOffset() {
		return getEntryOffset()+getHeaderSize();
	}
	private static long getPosition(FileChannel fc) {
		long result = -1;
		if (fc != null) {
			try {
				result = fc.position();
			} catch (IOException e) {
				LOG.log(Level.ERROR, e.getLocalizedMessage(), e);
			}
		}
		return result;
	}
	
	public int getEntryOffset() {
		return entryOffset;
	}

	private static FileChannel setPosition(FileChannel fc, long absolutePosition) {
		FileChannel fileChannel = fc;
		if (fileChannel != null) {
			try {
				fileChannel = fileChannel.position(absolutePosition);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return fileChannel;
	}

	/**
	 * 
	 * @return the total amounts of byte this segment has <i>including</i> the alignment padding after the deflate data
	 */
	public int length() {
		return getHeaderSize()+getCompressedSize()+getEndPadding();
	}
	
	
	protected int getHeaderSize() {
		return Integer.BYTES<<1;
	}

	private static final int MOD_MASK=Integer.BYTES-1;
	private static int getAlignedPadding(int offset, int len) {
		int end = offset + len;
		int mod = end & MOD_MASK;
		if (mod != 0) {
			mod = Integer.BYTES - mod;
		}
		return mod;
	}


	private int getEndPadding() {
		return getAlignedPadding(getEntryOffset(), getCompressedSize());
	}

	private static ByteBuffer getHeaderByteBuffer(FileChannel fileChannel) {
		Objects.requireNonNull(fileChannel);
		ByteBuffer bb = ByteBuffer.allocateDirect(INITIALIZATION_BUFFER_SIZE).order(ByteOrder.nativeOrder());
		if (fileChannel != null) {
			try {
				int read = fileChannel.read(bb);
				bb.flip();
				if (read != INITIALIZATION_BUFFER_SIZE) {
					LOG.log(Level.WARNING,
							"The amount of bytes read was %d bytes but expected to read %d bytes.  Unexpected behavior may occur",
							read, INITIALIZATION_BUFFER_SIZE);
				} else {
					LOG.log(Level.DEBUG, Messages.BUFFER_SUCCESS);
				}
			} catch (IOException e) {
				LOG.log(Level.ERROR, e.getLocalizedMessage(), e);
			}
		}
		return bb;
	}

	public int getCompressedSize() {
		return compressedSize;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	protected FileMetadataPointers getParentPointer() {
		return this.parentPointer;
	}

	FileChannel getFileChannel() {
		FileChannel fileChannel = null;
		FileMetadataPointers parentPointer = getParentPointer();
		if (parentPointer != null) {
			fileChannel = parentPointer.getFileChannel();
		}
		return fileChannel;
	}



	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + bufferSize;
		result = prime * result + compressedSize;
		result = prime * result + entryOffset;
		result = prime * result + ((parentPointer == null) ? 0 : parentPointer.hashCode());
		return result;
	}



	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DeflateSegment other = (DeflateSegment) obj;
		if (bufferSize != other.bufferSize)
			return false;
		if (compressedSize != other.compressedSize)
			return false;
		if (entryOffset != other.entryOffset)
			return false;
		if (parentPointer == null) {
			if (other.parentPointer != null)
				return false;
		} else if (!parentPointer.equals(other.parentPointer))
			return false;
		return true;
	}



	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("DeflateSegment[");
		sb.append("entryOffset=").append(getEntryOffset());
		sb.append(", compressedSize=").append(getCompressedSize());
		sb.append(", bufferSize=").append(getBufferSize()).append(']');
		return sb.toString();
	}

}

/**
 * 
 */
package com.ratzlaff.james.arc.earc;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Objects;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.InflaterOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ratzlaff.james.arc.earc.obfus.DeflateDeobfuscator;

/**
 * 
 * 
 * Reflects the structure of a Deflate section within a an earc file.
 * 
 * @author James Ratzlaff
 *
 */
public class DeflateSegment {
	private static final transient Logger LOG = LoggerFactory.getLogger(DeflateSegment.class);
	private static final int INITIALIZATION_BUFFER_SIZE = Integer.BYTES << 1;

	private static class Messages {
		private static final String REL_OFFSET = "has a relative offset of {} and an absolute offset of {}";
		private static final String INFLATE_START = "deflate data starts at relative offset {}";
		private static final String ALIGNMENT_PADDING = "has {} bytes of padding to align every {} bytes";
		private static final String COMPRESSED_SIZE = "compressed size of {} bytes";
		private static final String BUFFER_SIZE = "buffer size of {} bytes";
		private static final String BUFFER_SUCCESS = String.format("Successfully read %d bytes apply to entities",
				INITIALIZATION_BUFFER_SIZE);
		private static final String NULL_PARAM_MESSAGE = String.format(
				"An DeflateSegmant {0} MUST have a non-null {0} object passed into the constructor.",
				DeflateSegment.class.getSimpleName(), EArcEntry.class.getSimpleName());
	}

	private final EArcEntry parentPointer;
	private final int entryOffset;
	private final int compressedSize;
	private final int bufferSize;
	private Short deflateKey;

	public DeflateSegment(EArcEntry parent) {
		Objects.requireNonNull(parent, Messages.NULL_PARAM_MESSAGE);
		parentPointer = parent;
		Short keyToUse = null;
		if(parentPointer!=null&&parentPointer.isObfuscated()&&parentPointer.getDeflateSegments().isEmpty()) {
			keyToUse=parentPointer.getDeflateKey();
		}
		this.deflateKey=keyToUse;
		FileChannel fc = getFileChannel();
		int entryOffsetToUse = (int) (getPosition(fc) - getParentPointer().getDataLocation());
		this.entryOffset = entryOffsetToUse;
		LOG.info(Messages.REL_OFFSET, getEntryOffset(), getEntryOffset() + getParentPointer().getDataLocation());

		ByteBuffer bb = getHeaderByteBuffer(fc);

		int compressedSizeToUse = 0;
		compressedSizeToUse = bb.getInt();//LEFT-SIDE Value
		this.compressedSize = compressedSizeToUse;
		LOG.info(Messages.COMPRESSED_SIZE, getCompressedSize(), getEntryOffset());

		int bufferSizeToUse = 0;
		bufferSizeToUse = bb.getInt();//RIGHT-SIDE Value
		this.bufferSize = bufferSizeToUse;
		LOG.info(Messages.BUFFER_SIZE, getBufferSize());
		incrementPosition(fc, getCompressedSize());
		incrementPosition(fc, getEndPadding());

		LOG.info(Messages.INFLATE_START, getDeflateDataOffset());
		LOG.info(Messages.ALIGNMENT_PADDING, getEndPadding(), Integer.BYTES);
		
	}

	private static FileChannel incrementPosition(FileChannel fc, int delta) {
		return setPosition(fc, delta + (fc != null ? getPosition(fc) : 0));
	}

	public int getDeflateDataOffset() {
		return getEntryOffset() + getHeaderSize();
	}

	private static long getPosition(FileChannel fc) {
		long result = -1;
		if (fc != null) {
			try {
				result = fc.position();
			} catch (IOException e) {
				LOG.error(e.getLocalizedMessage(), e);
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
	 * @return the total amounts of byte this segment has <i>including</i> the
	 *         alignment padding after the deflate data
	 */
	public int length() {
		return getHeaderSize() + getCompressedSize() + getEndPadding();
	}

	protected int getHeaderSize() {
		return Integer.BYTES << 1;
	}
	 InputStream readIntoInputStream(InputStream is) {
		Inflater inflater = new Inflater();
		FileChannel fc = getParentPointer().getFileChannel();
		ByteBuffer bb = ByteBuffer.allocateDirect(getCompressedSize());
		InflaterInputStream iis = new InflaterInputStream(is, inflater, getBufferSize());
		return iis;
	
	}
	public void writeToOutputStream(OutputStream os) {
		Inflater inflater = new Inflater();
		FileChannel fc = getParentPointer().getFileChannel();
		ByteBuffer bb = ByteBuffer.allocateDirect(getCompressedSize());

		InflaterOutputStream ios = new InflaterOutputStream(os, inflater, getBufferSize());
		try {
			ios.write(getCompressedDataAsByteArray());
			
		} catch (IOException e) {
			LOG.error(e.getLocalizedMessage(),e);
		} finally {
//			close(ios);
		}
	
	}
	
	private static void close(Closeable c) {
		if(c!=null) {
			try {
				c.close();
			} catch (IOException e) {
				LOG.error(e.getLocalizedMessage(),e);
			}
		}
	}
	
	
	public long getAbsoluteDeflateDataOffset() {
		return getParentPointer().getDataLocation()+getDeflateDataOffset();
	}
	
	public byte[] getCompressedDataAsByteArray() {
		byte[] bytes = getCompressedDataAsByteArray(null);
		return bytes;
	}

	public byte[] getCompressedDataAsByteArray(byte[] dst) {
		if(dst==null) {
			dst=new byte[getCompressedSize()];
		}
		ByteBuffer wrapped = ByteBuffer.wrap(dst);
		getCompressedDataAsByteBuffer(wrapped);
		return dst;
	}
	
	public ByteBuffer getCompressedDataAsByteBuffer(ByteBuffer dst) {
		
		if (dst == null) {
			dst = ByteBuffer.allocateDirect(getCompressedSize());
		}
		
		FileChannel fc = getFileChannel();
		try {
			long absOffset = getAbsoluteDeflateDataOffset();
			fc.position(absOffset);
			int read = fc.read(dst);
			dst.flip();
		} catch (IOException ioe) {
			LOG.error(ioe.getLocalizedMessage(), ioe);
		}
		return dst;

	}

	private static final int MOD_MASK = Integer.BYTES - 1;

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
					LOG.warn(
							"The amount of bytes read was %d bytes but expected to read %d bytes.  Unexpected behavior may occur",
							read, INITIALIZATION_BUFFER_SIZE);
				} else {
					LOG.debug(Messages.BUFFER_SUCCESS);
				}
			} catch (IOException e) {
				LOG.error(e.getLocalizedMessage(), e);
			}
		}
		return bb;
	}
	
	public int getRawCompressedSizeValue() {
		return compressedSize;
	}
	
	public int getRawBufferSizeValue() {
		return bufferSize;
	}

	public int getCompressedSize() {
		return deflateKey!=null?DeflateDeobfuscator.DEFAULT_INSTANCE.getToggledObfuscationForLeftValue(getDeflateKey().shortValue(), getRawCompressedSizeValue()):getRawCompressedSizeValue();
	}

	public int getBufferSize() {
		return deflateKey!=null?DeflateDeobfuscator.DEFAULT_INSTANCE.getToggledObfuscationForRightValue(getDeflateKey().shortValue(), getRawBufferSizeValue()):getRawBufferSizeValue();
	}

	protected EArcEntry getParentPointer() {
		return this.parentPointer;
	}

	FileChannel getFileChannel() {
		FileChannel fileChannel = null;
		EArcEntry parentPointer = getParentPointer();
		if (parentPointer != null) {
			fileChannel = parentPointer.getFileChannel();
		}
		return fileChannel;
	}
	
	public Short getDeflateKey() {
		return deflateKey;
	}

	/*
	 * (non-Javadoc)
	 * 
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

	/*
	 * (non-Javadoc)
	 * 
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

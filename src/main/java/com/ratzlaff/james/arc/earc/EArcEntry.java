package com.ratzlaff.james.arc.earc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.InflaterInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author James Ratzlaff
 *
 */
public class EArcEntry implements Comparable<EArcEntry>{
	private static final transient Logger LOG = LoggerFactory.getLogger(EArcEntry.class);
	// meta_data
	// file_offset meta_data_offset name type desc
	// 0x30 0x00 unknown? long (unknown)
	// 0x38 0x08 extracted_size int (basically the uncompressed size, if it is
	// indeed uncompressed)
	// 0x4C 0x0C data_length? int (seems to need to be divisible by 8. The needed
	// data to unzip it may be slightly smaller (basically if the file ends with
	// nulls [0x00], read backward until a non-null limit is hit. That is the end of
	// the file [maybe]. My guess is so the data can be read in as longs)?
	// 0x50 0x10 type? int (appears to be a values 0,2,3, or 5, 0 appears to
	// indicate raw data)
	// 0x54 0x14 data_url_loc int (location of data_url)
	// 0x58 0x18 data_loc long (location of the actual file)
	// 0x5C 0x1C path_loc long (location of the file path string)
	public static final Charset DEFAULT_EARC_CHARSET = Charset.forName("UTF-8");
	private long unknown;
	private int extractedSize;
	private int length;
	private int type;
	private int dataUrlLocation;
	private long dataLocation;
	private long pathLocation;
	private short obfuscationKey;
	private List<DeflateSegment> deflateSegments;
	private String dataUrl;
	private String path;

	private final Supplier<FileChannel> fileChannelSupplier;

	public EArcEntry(Supplier<FileChannel> fileChannelSupplier) {
		this(fileChannelSupplier, null);
	}

	public EArcEntry(Supplier<FileChannel> fileChannelSupplier, ByteBuffer bb) {
		this.fileChannelSupplier = fileChannelSupplier;
		long checksumToUse = -1;
		int extractedSizeToUse = -1;
		int lengthToUse = -1;
		int typeToUse = -1;
		int dataUrlLocationToUse = -1;
		long dataLocationToUse = -1;
		long pathLocationToUse = -1;
		short obfuscationKey = 0;
		if (this.fileChannelSupplier != null) {
			if (bb == null) {
				bb = ByteBuffer.allocateDirect(40).order(ByteOrder.nativeOrder());
			}
			FileChannel fc = this.fileChannelSupplier.get();
			try {
				int bytesRead = fc.read(bb);
				bb.flip();
				checksumToUse = bb.getLong();
				extractedSizeToUse = bb.getInt();
				lengthToUse = bb.getInt();
				typeToUse = bb.getInt();
				dataUrlLocationToUse = bb.getInt();
				dataLocationToUse = bb.getLong();
				pathLocationToUse = bb.getLong();
				bb.flip();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		unknown = checksumToUse;
		extractedSize = extractedSizeToUse;
		length = lengthToUse;
		type = typeToUse;
		dataUrlLocation = dataUrlLocationToUse;
		dataLocation = dataLocationToUse;
		pathLocation = pathLocationToUse;

	}

	public static String readString(FileChannel fc, int offset, ByteBuffer dst) {
		String result = null;
		// if(offset%8!=0) {
		// throw new RuntimeException("offset must be divisible by 8");
		// }
		if (dst == null) {
			dst = ByteBuffer.allocateDirect(1024);
		}
		int bytesRead = 0;
		try {
			ByteBuffer littleBuffer = ByteBuffer.allocateDirect(8);
			fc.position(offset);
			while (bytesRead != -1 && dst.limit() != 0 && dst.remaining() > 0) {
				bytesRead = fc.read(littleBuffer);
				if (bytesRead < 0) {
					break;
				}
				int lastByte = bytesRead > 0 ? littleBuffer.get((int) bytesRead - 1) : Integer.MAX_VALUE;

				littleBuffer.flip();
				dst.put(littleBuffer);
				if (lastByte == 0) {
					break;
				}
				littleBuffer.rewind();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		dst.flip();
		result = DEFAULT_EARC_CHARSET.decode(dst).toString().trim();
		return result;
	}

	public static String readString(Path p, int offset, ByteBuffer dst) {
		String result = null;
		try {
			result = readString(FileChannel.open(p, StandardOpenOption.READ), offset, dst);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return result;
	}

	public static String readString(String str, int offset, ByteBuffer dst) {
		String result = readString(Paths.get(str), offset, dst);
		return result;
	}

	public static String readString(String str, int offset) {
		ByteBuffer bb = ByteBuffer.allocateDirect(1024);
		String result = readString(str, offset, bb);
		return result;
	}

	FileChannel getFileChannel() {
		FileChannel result = null;
		if (fileChannelSupplier != null) {
			result = fileChannelSupplier.get();
		}
		return result;
	}



	/**
	 * 
	 * @return the unknown data field, it looks like it's for ordering, the
	 *         {@link #getType() type} definitely seems to have something to do with
	 *         this number
	 */
	public long getUnknown() {
		return unknown;
	}

	public void setChecksum(long checksum) {
		this.unknown = checksum;
	}

	/**
	 * 
	 * @return the extracted size (in bytes) of the file
	 */
	public int getExtractedSize() {
		return extractedSize;
	}

	public void setExtractedSize(int extractedSize) {
		this.extractedSize = extractedSize;
	}

	/**
	 * 
	 * @return the length of the data entry in the archive
	 */
	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	/**
	 * file types?
	 * 
	 * <pre>
	 *	0x00		.lnkani,.listb,.clsn	(appears to always be uncompressed, extracted_size and data_length always seem to be the same)
	 *	0x01		.lnkani,.elx,.autoext	(appears to always be uncompressed, extracted_size and data_length always seem to be the same)
	 *	0x02		.btex,.swf,.swfb,.anmgph,.pka,.bnm (compressed)
	 *	0x03		.ebex,.sax	(compressed, includes extracted_size and data_length)
	 *	0x04		.gpubin,.htpk (compressed, does not include extracted_size or data_length in metadata)
	 *	0x05		.ebex@,.dds (not compressed. maybe it's a file inside of another archive? ie a substream, see weird types, does not include exrtacted_size and length)
	 * </pre>
	 * 
	 * From the table above, it appears that the 0x02 bit indicates a file is
	 * compressed. There was a 0x4 type I found with no
	 * 
	 * @return the metatype of the entry
	 */
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getDataUrlLocation() {
		return dataUrlLocation;
	}

	public void setDataUrlLocation(int dataUrlLocation) {
		this.dataUrlLocation = dataUrlLocation;
	}

	public String getDataUrl(ByteBuffer dst) {
		if (dataUrl == null && dataUrlLocation != -1) {
			FileChannel fc = getFileChannel();
			if (fc != null) {
				this.dataUrl = readString(fc, (int) dataUrlLocation, dst);
			}
		}
		return dataUrl;
	}

	public String getDataUrl() {
		String result = getDataUrl(null);
		return result;
	}

	public String getFilePath(ByteBuffer dst) {
		if (path == null && dataUrlLocation != -1) {
			FileChannel fc = getFileChannel();
			if (fc != null) {
				path = readString(fc, (int) pathLocation, dst);
			}
		}
		return path;
	}

	public String getFilePath() {
		String result = getFilePath(null);
		return result;
	}

	public long getDataLocation() {
		return dataLocation;
	}

	public void setDataLocation(long dataLocation) {
		this.dataLocation = dataLocation;
	}

	public long getPathLocation() {
		return pathLocation;
	}

	public void setPathLocation(long pathLocation) {
		this.pathLocation = pathLocation;
	}

	public File extractTo(Path dir) {
		if (dir == null) {
			dir = Paths.get("./");
		}
		Path myPath = getPath();
		Path resolved = dir.resolve(myPath);
		try {
			Files.createDirectories(resolved.getParent());
			Files.createFile(resolved);
			FileChannel outChannel = FileChannel.open(resolved, StandardOpenOption.WRITE);
			if (!EArchDataExtractor.isZipped(this)) {
				getFileChannel().transferTo(getDataLocation(), getLength(), outChannel);
			} else {
				ByteBuffer bb = ByteBuffer.wrap(getExtractData());
				int dataWritten = outChannel.write(bb);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return resolved.toFile();

	}

	public Path getPath() {
		String pathAsString = getFilePath();
		Path asPath = Paths.get(pathAsString);
		return asPath;
	}

	public byte[] getExtractData() {
		return EArchDataExtractor.extract(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (unknown ^ (unknown >>> 32));
		result = prime * result + (int) (dataLocation ^ (dataLocation >>> 32));
		result = prime * result + dataUrlLocation;
		result = prime * result + extractedSize;
		result = prime * result + length;
		result = prime * result + (int) (pathLocation ^ (pathLocation >>> 32));
		result = prime * result + type;
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
		EArcEntry other = (EArcEntry) obj;
		if (unknown != other.unknown)
			return false;
		if (dataLocation != other.dataLocation)
			return false;
		if (dataUrlLocation != other.dataUrlLocation)
			return false;
		if (extractedSize != other.extractedSize)
			return false;
		if (length != other.length)
			return false;
		if (pathLocation != other.pathLocation)
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	private static String toHexArray(long value) {
		String asHex = Long.toHexString(value);
		int idealLength = Long.BYTES << 1;
		int leftovers = idealLength - asHex.length();
		char[] filler = new char[leftovers];
		Arrays.fill(filler, '0');
		return new String(filler) + asHex;
	}

	private static final String[] EMPTY = new String[0];

	private static String[] chop(String str, int interval) {
		if (str == null || interval < 1) {
			return EMPTY;
		}
		int wholeSegments = (str.length() / interval);
		int leftOvers = str.length() % interval;
		String[] result = new String[wholeSegments + (leftOvers != 0 ? 1 : 0)];

		for (int i = 0; i < wholeSegments; i++) {
			int start = i * interval;
			result[i] = str.substring(start, start + interval);
		}
		if (leftOvers != 0) {
			result[result.length - 1] = str.substring(str.length() - leftOvers);
		}
		return result;
	}

	private static String divvy(String str, int interval, String delimiter) {
		String[] chopped = chop(str, interval);
		return String.join(delimiter, chopped);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("FileMetadataPointers [unknown=");
		builder.append(divvy(toHexArray(unknown), 2, " "));
		builder.append("(").append(unknown).append(")");
		builder.append(", extractedSize=");
		builder.append(extractedSize);
		builder.append(", length=");
		builder.append(length);
		builder.append(", type=");
		builder.append(type);
		builder.append(", dataUrlLocation=");
		builder.append(dataUrlLocation);
		builder.append(", dataLocation=");
		builder.append(dataLocation);
		builder.append(", pathLocation=");
		builder.append(pathLocation);
		if (dataUrl != null) {
			builder.append(", dataUrl=");
			builder.append(getDataUrl());
		}
		if (path != null) {
			builder.append(", filePath=");
			builder.append(getFilePath());
		}
		if(deflateSegments!=null&&!deflateSegments.isEmpty()) {
			builder.append(", deflateSegments=[");
			List<String> asStrList = getDeflateSegments().stream().map(segment->"\n\t\t"+segment).collect(Collectors.toList());
			String ofDeflateSegments = String.join(",", asStrList);
			builder.append(ofDeflateSegments);
			builder.append(']');
		}
		builder.append("]");
		return builder.toString();
	}

	/**
	 * 
	 * @param fileChannel
	 * @param len
	 * @return a compacted {@link ByteBuffer} with all, some, or none of the
	 *         requested data
	 */
	private static ByteBuffer getByteBuffer(FileChannel fileChannel, long offset, int len) {
		ByteBuffer bb = ByteBuffer.allocateDirect(len).order(ByteOrder.nativeOrder());
		if (fileChannel != null) {
			try {
				if (offset > -1) {
					fileChannel.position(offset);
				}
				int read = 0;
				int totalRead = 0;
				while ((read = fileChannel.read(bb)) != -1 && totalRead < len) {
					totalRead += read;
				}
				bb.flip();
			} catch (IOException e) {
				LOG.error(e.getLocalizedMessage(), e);
			}
		}
		
		return bb;
	}

	
	private void createParentDirs(Path in) {
		try {
			Files.createDirectories(in.resolve(getPath().getParent()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public File write() {
		return write(null);
	}
	public File write(Path in) {
		if(in==null) {
			in=Paths.get("./");
		}
		OutputStream os = null;
		try {
			createParentDirs(in);
			os = Files.newOutputStream(in.resolve(getPath()), StandardOpenOption.CREATE_NEW,StandardOpenOption.APPEND);
			List<DeflateSegment> segments = getDeflateSegments();
			for(int i=0;i<segments.size();i++) {
				DeflateSegment ds = segments.get(i);
				ds.writeToOutputStream(os);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				os.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return in.resolve(getPath()).toFile();
	}
	
	private class RotatingInflateInputStream extends InputStream {

		
		InputStream delegate;
		
		/**
		 * 
		 */
		public RotatingInflateInputStream() {
			 delegate = Channels.newInputStream(getFileChannel());
		}
		
		/* (non-Javadoc)
		 * @see java.io.InputStream#read()
		 */
		@Override
		public int read() throws IOException {
			// TODO Auto-generated method stub
			return 0;
		}

		/* (non-Javadoc)
		 * @see java.io.InputStream#available()
		 */
		@Override
		public int available() throws IOException {
			// TODO Auto-generated method stub
			return super.available();
		}

		/* (non-Javadoc)
		 * @see java.io.InputStream#close()
		 */
		@Override
		public void close() throws IOException {
			// TODO Auto-generated method stub
			super.close();
		}

		/* (non-Javadoc)
		 * @see java.io.InputStream#mark(int)
		 */
		@Override
		public synchronized void mark(int arg0) {
			// TODO Auto-generated method stub
			super.mark(arg0);
		}

		/* (non-Javadoc)
		 * @see java.io.InputStream#markSupported()
		 */
		@Override
		public boolean markSupported() {
			// TODO Auto-generated method stub
			return super.markSupported();
		}

		/* (non-Javadoc)
		 * @see java.io.InputStream#read(byte[], int, int)
		 */
		@Override
		public int read(byte[] arg0, int arg1, int arg2) throws IOException {
			// TODO Auto-generated method stub
			return super.read(arg0, arg1, arg2);
		}

		/* (non-Javadoc)
		 * @see java.io.InputStream#read(byte[])
		 */
		@Override
		public int read(byte[] arg0) throws IOException {
			// TODO Auto-generated method stub
			return super.read(arg0);
		}

		/* (non-Javadoc)
		 * @see java.io.InputStream#readAllBytes()
		 */
		@Override
		public byte[] readAllBytes() throws IOException {
			// TODO Auto-generated method stub
			return super.readAllBytes();
		}

		/* (non-Javadoc)
		 * @see java.io.InputStream#readNBytes(byte[], int, int)
		 */
		@Override
		public int readNBytes(byte[] arg0, int arg1, int arg2) throws IOException {
			// TODO Auto-generated method stub
			return super.readNBytes(arg0, arg1, arg2);
		}

		/* (non-Javadoc)
		 * @see java.io.InputStream#reset()
		 */
		@Override
		public synchronized void reset() throws IOException {
			// TODO Auto-generated method stub
			super.reset();
		}

		/* (non-Javadoc)
		 * @see java.io.InputStream#skip(long)
		 */
		@Override
		public long skip(long arg0) throws IOException {
			// TODO Auto-generated method stub
			return super.skip(arg0);
		}

		/* (non-Javadoc)
		 * @see java.io.InputStream#transferTo(java.io.OutputStream)
		 */
		@Override
		public long transferTo(OutputStream arg0) throws IOException {
			// TODO Auto-generated method stub
			return super.transferTo(arg0);
		}
		
	}
	
//	public InputStream getAsInputStream() {
//		FileChannel fc = getFileChannel();
//		InputStream is = null;
//		if(fc!=null) {
//			is = Channels.newInputStream(fc);
//			if(isProbablyAZipStream()) {
//				is=new InflaterInputStream(is);
//				
//			} else {
//				
//			}
//		}
//	}
	
	private ByteBuffer getByteBufferOfData() {
		ByteBuffer bb = getByteBuffer(getFileChannel(), getDataLocation(), getLength());
		return bb;
	}
	
	DeflateSegment getLastSegment(){
		DeflateSegment last = null;
		if(this.deflateSegments!=null) {
			int numberOfSegments = this.deflateSegments.size();
			if(numberOfSegments>0) {
				last=this.deflateSegments.get(numberOfSegments-1);
			}
		}
		return last;
	}

	public List<DeflateSegment> getDeflateSegments() {
		if (this.deflateSegments == null) {
			if (isProbablyAZipStream()) {
				this.deflateSegments = new ArrayList<DeflateSegment>();
				FileChannel fileChannel = getFileChannel();
				try{
					fileChannel.position(getDataLocation());
					int totalBytesSought=0;
					int len = getLength();
					while(totalBytesSought<len) {
						DeflateSegment df = new DeflateSegment(this);
						totalBytesSought+=(df.length());
						this.deflateSegments.add(df);
					}
					
				}catch(IOException ioe) {
					LOG.error(ioe.getLocalizedMessage(),ioe);
				}
				((ArrayList<DeflateSegment>) this.deflateSegments).trimToSize();
			} else {
				this.deflateSegments = Collections.emptyList();
			}
		}
		return this.deflateSegments;
	}
	


	private boolean isProbablyAZipStream() {
		if(this.deflateSegments!=null&&this.deflateSegments.isEmpty()) {
			return false;
		}
		boolean zipped = false;
		FileChannel fc = getFileChannel();
		int targetLen = (Integer.BYTES<<1) + 2;
		ByteBuffer bb = getByteBuffer(fc, getDataLocation(), targetLen);
		zipped = bb.remaining() == targetLen;
		if (!zipped) {
			LOG.debug("Not zipped. Expected to read %d bytes but only read %d", targetLen, bb.remaining());
		} else {
			int compressedSize = bb.getInt();
			int inflatedSize = bb.getInt();
			byte firstByte = bb.get();
			byte secondByte = bb.get();
			boolean compressAndDeflateAreLegit = compressedSizeAndInflatedSizeAreLegit(compressedSize, inflatedSize);
			boolean firstByteIsCorrect = byteIsValidFirstInflateByte(firstByte);
			boolean secondByteIsCorrect = byteIsValidSecondInflateByte(secondByte);

			if (!firstByteIsCorrect) {
				LOG.debug("The first inflate header byte is not correct. Expected %x but was given %x",
						ZLIB_FIRST_MAGIC_VALUE, firstByte);
			}
			if (!secondByteIsCorrect) {
				LOG.debug(
						"The second inflate header byte is not correct. Expected %x, %x, %x, or %x but was given %x",
						x01, x5A, x9C, xDA, secondByte);
			}
			zipped = compressAndDeflateAreLegit && firstByteIsCorrect && secondByteIsCorrect;
		}
		return zipped;
	}

	private static final int MIN_COMPRESSED_SIZE = 2;

	private static boolean compressedSizeIsValid(int compressedSize) {
		boolean valid = compressedSize > MIN_COMPRESSED_SIZE;
		return valid;
	}

	public static final int MAX_INFLATE_BUFFER_SIZE = 0x20000;

	private static boolean inflatedSizeIsValid(int inflatedSize) {
		boolean inRange = inflatedSize > -1 && inflatedSize <= MAX_INFLATE_BUFFER_SIZE;
		return inRange;
	}

	private static boolean compressedSizeAndInflatedSizeAreLegit(int compressedSize, int inflatedSize) {
		boolean compressedSizeIsValid = compressedSizeIsValid(compressedSize);
		boolean inflatedSizeIsValid = false;
		boolean valid = compressedSizeIsValid;
		boolean coherent = false;
		if (valid) {
			inflatedSizeIsValid = inflatedSizeIsValid(inflatedSize);
			valid = inflatedSizeIsValid;
			if (!valid) {
				LOG.debug("The 'inflatedSize' value %d, is not valid because it is greater than %d",
						inflatedSize, MAX_INFLATE_BUFFER_SIZE);
			}
		} else {
			LOG.debug("The 'compressedSize' value %d, is not valid because it is less than %d",
					compressedSize, MIN_COMPRESSED_SIZE);
		}
		if (valid) {
			coherent = compressedSize < inflatedSize;
			valid = coherent;
			if (!valid) {
				LOG.debug(
						"The 'compressedSize' and 'inflateSize' are not coherent. Expected compressSize<=inflatedSize, but got %d and %d respectively",
						compressedSize, inflatedSize);
			}
		}
		return valid;
	}

	private static final byte ZLIB_FIRST_MAGIC_VALUE = 0x78;

	private static boolean byteIsValidFirstInflateByte(byte value) {
		boolean valid = value == 0x78;
		return valid;
	}

	private static final byte x01 = 0x01;
	private static final byte x5A = 0x5A;
	private static final byte x9C = (byte) 0x9C;
	private static final byte xDA = (byte) 0xDA;

	
	
	private static boolean byteIsValidSecondInflateByte(byte value) {
		switch (value) {
		case x01:
		case x5A:
		case x9C:
		case xDA:
			return true;
		default:
			return false;
		}
	}

	
	public static final class Comparators {
		
		public static final Comparator<EArcEntry> checksum = (a,b)->{
			return Comparator.comparingLong(EArcEntry::getUnknown).compare(a, b);
		};
		
		public static final Comparator<EArcEntry> dataLocation = (a,b)->{
			return Comparator.comparingLong(EArcEntry::getDataLocation).compare(a, b);
		};
		public static final Comparator<EArcEntry> type = (a,b)->{
			return Comparator.comparingInt(EArcEntry::getType).thenComparing(checksum).compare(a, b);
		};
		private static final Function<EArcEntry,String> urlExrtactor = EArcEntry::getDataUrl;
		public static final Comparator<EArcEntry> url= (a,b)->{
			return Comparator.comparing(urlExrtactor).compare(a,b);
		};
		
	}
	
	
	
	
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(EArcEntry o) {
		return Comparators.dataLocation.compare(this, o);
	}

}
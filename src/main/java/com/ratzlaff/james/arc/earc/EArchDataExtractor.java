package com.ratzlaff.james.arc.earc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipInputStream;

public class EArchDataExtractor {

	/**
	 * 
	 * @param pointers
	 * @return a {@link MappedByteBuffer} of the data associated to the {@link EArcEntry metadataPointer} object
	 */
	public static MappedByteBuffer getData(EArcEntry pointers) {
		MappedByteBuffer mbb = null;
		try {
			mbb = pointers.getFileChannel().map(MapMode.READ_ONLY, pointers.getDataLocation(), pointers.getLength());
			mbb.order(ByteOrder.nativeOrder());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return mbb;
	}
	public static ByteBuffer getRawDataAsDirectByteBuffer(EArcEntry pointer) {
		return getRawDataAsArrayBackedByteBuffer(pointer, 0,0);
	}
	public static ByteBuffer getRawDataAsDirectByteBuffer(EArcEntry pointer, long offsetFromOriginalDataLocation) {
		return getRawDataAsArrayBackedByteBuffer(pointer, offsetFromOriginalDataLocation,0);
	}
	public static ByteBuffer getRawDataAsDirectByteBuffer(EArcEntry pointer, long offsetFromOriginalDataLocation, int truncateOrExpand) {
		long offset = pointer.getDataLocation()+offsetFromOriginalDataLocation;
		int len = pointer.getLength()+truncateOrExpand;
		ByteBuffer result = ByteBuffer.allocateDirect(len).order(ByteOrder.nativeOrder());
		result = getRawDataAsByteBuffer(pointer.getFileChannel(), offset, len, result);
		return result;
	}
	public static ByteBuffer getRawDataAsArrayBackedByteBuffer(EArcEntry pointer) {
		return getRawDataAsArrayBackedByteBuffer(pointer, 0,0);
	}
	public static ByteBuffer getRawDataAsArrayBackedByteBuffer(EArcEntry pointer, long offsetFromOriginalDataLocation) {
		return getRawDataAsArrayBackedByteBuffer(pointer, offsetFromOriginalDataLocation,0);
	}
	public static ByteBuffer getRawDataAsArrayBackedByteBuffer(EArcEntry pointer, long offsetFromOriginalDataLocation, int truncateOrExpand) {
		long offset = pointer.getDataLocation()+offsetFromOriginalDataLocation;
		int len = pointer.getLength()+truncateOrExpand;
		ByteBuffer result = ByteBuffer.allocate(len).order(ByteOrder.nativeOrder());
		result = getRawDataAsByteBuffer(pointer.getFileChannel(), offset, len, result);
		return result;
	}
	
	private static ByteBuffer getRawDataAsByteBuffer(FileChannel fc, long offset, int len, ByteBuffer result) {
		if(result==null) {
			result = ByteBuffer.allocateDirect(len).order(ByteOrder.nativeOrder());
		}
		if(result.remaining()<len) {
			int toAlloc = result.capacity()-result.remaining()+len;
			ByteBuffer toCopyTo = result.isDirect()?ByteBuffer.allocateDirect(toAlloc):ByteBuffer.allocate(toAlloc);
			toCopyTo.put(result);
			result=toCopyTo;
		}
		int bytesRead=0;
		int totalBytesRead=0;
		try {
			while((bytesRead=fc.read(result, offset+totalBytesRead))!=-1&&totalBytesRead<len) {
				totalBytesRead+=bytesRead;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		result.flip();
		return result;
		
	}

	private static int getCompressedSize(EArcEntry pointer) {
		int compressedSize=0;
		if(pointer!=null) {
			if(isZipped(pointer)) {
				try {
					pointer.getFileChannel().position(pointer.getDataLocation());
					ByteBuffer intReader = ByteBuffer.allocateDirect(Integer.BYTES);
					pointer.getFileChannel().read(intReader);
					compressedSize = intReader.getInt(0);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return compressedSize;
	}
	
	/**
	 * 
	 * @param fc the {@link FileChannel} to read from
	 * @param offset the offset to being at
	 * @param len the number of bytes to be read
	 * @return an ARRAY backed byte buffer containing the data read
	 * @throws IOException
	 */
	private static ByteBuffer createByteBufferSegment(FileChannel fc, long offset, int len) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(len);
		int bytesRead=0;
		int totalBytesRead=0;
		while(bytesRead!=-1&&totalBytesRead<(len)) {
			bytesRead=fc.read(bb, offset+totalBytesRead);
			if(bytesRead==-1) {
				break;
			}
			totalBytesRead+=bytesRead;
		}
		bb.flip();
		return bb;
	}
	
	public static ByteBuffer readDataIntoByteBuffer(EArcEntry pointer) {
		long offset = pointer.getDataLocation();
		int len = pointer.getLength();
		if(isZipped(pointer)) {
			offset+=8;
			len-=8;
		}
		ByteBuffer result = null;
		try {
			result = createByteBufferSegment(pointer.getFileChannel(), offset, len);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * SPECIAL NOTE!!!: It turns out that the maximum deflated size can only be 131072 bytes,there for a filePointer may possibly have multiple zip entries
	 * @param mbb
	 * @param dataBlockLength
	 * @param expectedDecompressedLength
	 * @return
	 */
	private static Boolean isZipped(ByteBuffer mbb, int dataBlockLength, int expectedDecompressedLength) {
		Boolean zipped = false;
		if (mbb != null) {
			if (mbb.remaining() >= 9) {
				int compressedLength = mbb.getInt(0);
				int decompressedLength = mbb.getInt(4);
				
				byte header = mbb.get(8);
				boolean hasMagic =header == (byte) 0x78; 
				zipped = hasMagic;
				int compressedTruncation = (Integer.BYTES)<<1;
//				if(zipped) {
//					zipped=compressedLength==(dataBlockLength-compressedTruncation);
//					if(!zipped) {
//						System.err.println(String.format("The compressed length in the data header (%d bytes) is not equal to the expected compressed length (%d bytes - %d bytes = %d bytes) from the file metadata table",compressedLength,dataBlockLength,compressedTruncation,dataBlockLength-2));
//					}
//				}
//				if(zipped) {
//					zipped=decompressedLength==(expectedDecompressedLength);
//					if(!zipped) {
//						System.err.println(String.format("The decompressed size in the data header (%d bytes) is not equal to the expected decompressed size (%d bytes) from the file metadata table",decompressedLength,expectedDecompressedLength));
//					}
//				}
				if (!zipped) {
					header = mbb.get(0);
					byte header2 = mbb.get(1);
					zipped = header == 0x78 && header2 == 0xDA;
					if(zipped) {
						System.out.println("It appears this data might be zipped but has abnormal headers");
						zipped=null;
					}
					
				}
			}
		}
		return zipped;
	}
	public static byte[] toByteArray(ByteBuffer bb, int offset, int len) {
		if(offset<0) {
			offset=0;
		}
		if(len>bb.remaining()||len<0) {
			len=bb.remaining();
		}
		byte[] bytes = new byte[len];
		bb.get(bytes, offset, len);
		return bytes;
	}
	
	public static ByteArrayInputStream toByteArrayInputStream(EArcEntry pointer) {
		ByteBuffer data = getRawDataAsDirectByteBuffer(pointer);
		Boolean zipped = isZipped(data,pointer.getLength(),pointer.getExtractedSize());
		if(zipped!=null&&zipped) {
			data.position(8);
		}
		return toByteArrayInputStream(data);
	}
	
	public static ByteArrayInputStream toByteArrayInputStream(ByteBuffer bb) {
		return toByteArrayInputStream(bb, 0, bb.remaining());
	}
	public static ByteArrayInputStream toByteArrayInputStream(ByteBuffer bb, int offset, int len) {
		byte[] bytes = toByteArray(bb, offset, len);
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		return bais;
	}
	
	public static byte[] extract(EArcEntry pointer) {
		
		
		byte[] reso = new byte[pointer.getExtractedSize()];
		ByteArrayInputStream asByteArrayInputStream = toByteArrayInputStream(pointer);

		if(isZipped(pointer)) {
			InflaterInputStream zis = new InflaterInputStream(toByteArrayInputStream(pointer), new Inflater(),0x02000);
			try {
				int bytesRead=0;
				int totalBytesRead=0;
				while((bytesRead=zis.read(reso))!=-1&&totalBytesRead<reso.length){
					totalBytesRead+=bytesRead;
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					zis.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else {
			reso = getRawDataAsArrayBackedByteBuffer(pointer).array();
		}
		return reso;
	}

	
	
	public static boolean isZipped(EArcEntry pointer) {
		ByteBuffer bb = getRawDataAsArrayBackedByteBuffer(pointer, 0, -(pointer.getLength()-9));
		boolean zipped = isZipped(bb, pointer.getLength(), pointer.getExtractedSize());
		return zipped;
	}

}

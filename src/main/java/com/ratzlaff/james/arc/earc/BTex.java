/**
 * 
 */
package com.ratzlaff.james.arc.earc;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * @author James Ratzlaff
 *<pre>
 * 	magic										0x00 4B
 *	type										0x04 4B (LE) 
 *	unknown_0									0x08 4B? always 0 
 *	unknown_1									0x0C 2B? always 0 
 *	texSpecHdrSize								0x0E 2B value is always 128, which is where the BTEX header begins 
 *	filesize									0x10 4B  
 *	BTEX_MAGIC									0x80 4B always 'BTEX' (LE); 
 *	texDataHdrSize								0x84 4B 
 *	texDataSize									0x88 4B 
 *	unknown_2									0x8C 2B? value is always 4 (possibly number of channels)
 *	unknown_3 									0x8E 4B always (LE) 0xhh (so far found 0A [ps4] and 06 [pc])  0x01 0x01 0x00  
 *	unknown_4									0x92 4B always (LE) 0x38 0x00 0x20 0x00 [ps4 and pc]
 *	--skip 10 bytes-- 
 *	width										0xA0 2B unsigned short 
 *	height										0xA2 2B unsigned short 
 *	unknown_5									0xA4 2B seems to be unique across files.  Always seems to be a power of 2.
 *	format										0xA6 2B. 0x18 == DXT1[raw read], (0x21 == BC4/ATI1, 0x22==BC5,0x24==BC7 [convert to rgba32]) 
 *	unknown_6									0xA8 4B similar across file (in both ps4 and win ver) . Has little endian pattern 0xXX 0x01 0x02 0xXX
 *	numberOfSegments							0xAC 2B the number of offset+len combos to be read
 *	unknown_7									0xAE 2B always 8, possibly the length (in bytes) of a segment. A length offset combo is to adjacent integers so it makes sense 
 *	unknown_8									0xB0 4B always 0 
 *	unknown_9									0xB4 4B always 0x38 
 *	unknown_10									0xB8 4B seems to be a number between 56 (ps4) and 160 (pc) all numbers seen have been divisibile by 8. Probably the qword aligned end offset of the name 
 *	--skip 24 bytes-- 
 *	unknown_11									0xD4 4B always 1, possibly number of mipmaps 
 *	unknown_12									0xD8 4B appears to be zero
 *	segment_offsets_and_lengths					0xDC (numberOfSegments*unknown_8)B...offsets and lengths of segments.  Appears to be logarithmic.
 *	filename									0xDC+(numberOfSegments*unknown_8) reading up to unknown_10
 *	texData										texSpecHdrSize+texDataHdrSize
 *</pre>
 */

public class BTex {
	public static final int MAGIC = 0x42444553;

	private BTexHeader header;
	private int BTEX_MAGIC; 					/* 0x80 4B always BTEX; */
	private int texDataHdrSize;					/* 0x84 4B */
	private int texDataSize;					/* 0x88 4B */
	private short unknown_2;					/* 0x8C 2B? value is always 4 (possibly number of channels)*/
	private int unknown_3;						/* 0x8E 4B always (LE) 0xhh (so far found 0A [ps4] and 06 [pc])  0x01 0x01 0x00 */ 
	private int unknown_4; 						/* 0x92 4B always (LE) 0x38 0x00 0x20 0x00 [ps4 and pc]*/
	/** skip 10 bytes */
	private int width;							/* 0xA0 2B unsigned short */
	private int height;							/* 0xA2 2B unsigned short */
	private short unknown_5;					/* 0xA4 2B seems to be unique across files.  Always seems to be a power of 2.*/
	private short format;						/* 0xA6 2B. 0x18 == DXT1[raw read], (0x21 == BC4/ATI1, 0x22==BC5,0x24==BC7 [convert to rgba32]) */
	private int unknown_6;						/* 0xA8 4B similar across file (in both ps4 and win ver) . Has little endian pattern 0xXX 0x01 0x02 0xXX*/
	private short numberOfSegments;				/* 0xAC 2B the number of offset+len combos to be read*/
	private short unknown_7;					/* 0xAE 2B always 8, possibly the length (in bytes) of a segment. A length offset combo is to adjacent integers so it makes sense */
	private int unknown_8;						/* 0xB0 4B always 0 */
	private int unknown_9;						/* 0xB4 4B always 0x38 */
	private int unknown_10;						/* 0xB8 4B seems to be a number between 56 (ps4) and 160 (pc) all numbers seen have been divisibile by 8. Probably the qword aligned end offset of the name */
	/** skip 24 bytes **/
	private int unknown_11; 					/* 0xD4 4B always 1, possibly number of mipmaps */
	private int unknown_12;						/* 0xD8 4B appears to be zero */
	private List<OffsetLen> offsetsAndLengths;  /* 0xDC numberOfSegments*unknown_8 bytes...offsets and lengths of segments.  Appears to be logarithmic. */
	
	public BTex(ByteBuffer bb) {
		
	}
	
	public static class BTexHeader {
		private int magic;
		private String type;
		private int unknown_0;
		private short unknown_1;
		private short texSpecHdrSize;
		private int filesize;
		
		public BTexHeader(ByteBuffer bb) {
			magic=bb.getInt();
			byte[] typeBytes = new byte[4];
			bb.get(typeBytes);
			type=new String(typeBytes);
			unknown_0=bb.getInt();
			unknown_1=bb.getShort();
			texSpecHdrSize=bb.getShort();
			filesize=bb.get();
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + filesize;
			result = prime * result + magic;
			result = prime * result + texSpecHdrSize;
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			result = prime * result + unknown_0;
			result = prime * result + unknown_1;
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof BTexHeader)) {
				return false;
			}
			BTexHeader other = (BTexHeader) obj;
			if (filesize != other.filesize) {
				return false;
			}
			if (magic != other.magic) {
				return false;
			}
			if (texSpecHdrSize != other.texSpecHdrSize) {
				return false;
			}
			if (type == null) {
				if (other.type != null) {
					return false;
				}
			} else if (!type.equals(other.type)) {
				return false;
			}
			if (unknown_0 != other.unknown_0) {
				return false;
			}
			if (unknown_1 != other.unknown_1) {
				return false;
			}
			return true;
		}

		/**
		 * @return the magic
		 */
		public int getMagic() {
			return magic;
		}

		/**
		 * @param magic the magic to set
		 */
		public void setMagic(int magic) {
			this.magic = magic;
		}

		/**
		 * @return the type
		 */
		public String getType() {
			return type;
		}

		/**
		 * @param type the type to set
		 */
		public void setType(String type) {
			this.type = type;
		}

		/**
		 * @return the unknown_0
		 */
		public int getUnknown_0() {
			return unknown_0;
		}

		/**
		 * @param unknown_0 the unknown_0 to set
		 */
		public void setUnknown_0(int unknown_0) {
			this.unknown_0 = unknown_0;
		}

		/**
		 * @return the unknown_1
		 */
		public short getUnknown_1() {
			return unknown_1;
		}

		/**
		 * @param unknown_1 the unknown_1 to set
		 */
		public void setUnknown_1(short unknown_1) {
			this.unknown_1 = unknown_1;
		}

		/**
		 * @return the texSpecHdrSize
		 */
		public short getTexSpecHdrSize() {
			return texSpecHdrSize;
		}

		/**
		 * @param texSpecHdrSize the texSpecHdrSize to set
		 */
		public void setTexSpecHdrSize(short texSpecHdrSize) {
			this.texSpecHdrSize = texSpecHdrSize;
		}

		/**
		 * @return the filesize
		 */
		public int getFilesize() {
			return filesize;
		}

		/**
		 * @param filesize the filesize to set
		 */
		public void setFilesize(int filesize) {
			this.filesize = filesize;
		}
		
	}
	
	public static class OffsetLen {
		private int offset;
		private int length;
		
		public OffsetLen(int offset, int length) {
			this.offset=offset;
			this.length=length;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + length;
			result = prime * result + offset;
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof OffsetLen)) {
				return false;
			}
			OffsetLen other = (OffsetLen) obj;
			if (length != other.length) {
				return false;
			}
			if (offset != other.offset) {
				return false;
			}
			return true;
		}

		/**
		 * @return the offset
		 */
		public int getOffset() {
			return offset;
		}

		/**
		 * @param offset the offset to set
		 */
		public void setOffset(int offset) {
			this.offset = offset;
		}

		/**
		 * @return the length
		 */
		public int getLength() {
			return length;
		}

		/**
		 * @param length the length to set
		 */
		public void setLength(int length) {
			this.length = length;
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("/** OffsetLen **/").append("{ 'offset':").append(offset).append(",'length':").append(length).append("}");
			return sb.toString();
		}
		
	}
	
	
	
	
	
}

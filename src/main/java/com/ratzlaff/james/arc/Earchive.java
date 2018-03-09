package com.ratzlaff.james.arc;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;

import com.ratzlaff.james.arc.earc.ContainerNode;
import com.ratzlaff.james.arc.earc.EArcEntry;
import com.ratzlaff.james.arc.earc.EArcHeader;
import com.ratzlaff.james.arc.earc.LeafNode;

/**
 * 
 * @author James Ratzlaff
 *
 */
public class Earchive implements Closeable{
	
	
	
	
	// header
	// 0x00 magic int (always has the value 0x41465243, 'CRAF' or 'FARC')
	// 0x04 version(?) int?(always has the value 196628)
	// 0x08 file_count int
	// 0x0C min_data_block_size? int (always has the value of 512)
	// 0x10 metadata_start? int (always has the value of 64)
	// 0x14 url_tbl_loc int (data://blah/bloh/blee.file)
	// 0x18 path_tbl_loc int (blah/bloh/blee.file)
	// 0x1C data_tbl_loc int (should be power of 2)
	// 0x20 ? int (appears to be a flag...all files seem to have either 1 or 0)
	// 0x24 ? int (always has the value 0x80)

	// meta_data
	// file_offset meta_data_offset name type desc
	// 0x30 0x00 hash? long (not sure which type)
	// 0x38 0x08 extracted_size int (basically the uncompressed size, if it is
	// indeed uncompressed)
	// 0x4C 0x0C data_length int (seems to need to be divisible by 8. The needed
	// data to unzip it may be slightly smaller (basically if the file ends with
	// nulls [0x00], read backward until a non-null limit is hit. That is the end of
	// the file [maybe]. My guess is so the data can be read in as longs)?
	// 0x50 0x10 type? int (appears to be a values 0-5, 0 and 1 appears to indicate
	// raw data, 5 and sometimes 4 indicate a reference to another archive)
	// 0x54 0x14 data_url_loc int (location of data_url)
	// 0x58 0x18 data_loc long (location of the actual file)
	// 0x5C 0x1C path_loc long (location of the file path string)

	// file types?
	// 0x00 .lnkani,.listb,.clsn (appears to always be uncompressed, extracted_size
	// and data_length always seem to be the same)
	// 0x01 .lnkani,.elx,.autoext (appears to always be uncompressed, extracted_size
	// and data_length always seem to be the same)
	// 0x02 .btex,.swf,.swfb,.anmgph,.pka,.bnm (compressed)
	// 0x03 .ebex,.sax (compressed, includes extracted_size and data_length)
	// 0x04 .gpubin,.htpk (compressed, does not include extracted_size or
	// data_length in metadata)
	// 0x05 .ebex@,.dds (not compressed. maybe it's a file inside of another
	// archive? ie a substream, see weird types)

	// weird types:
	// given the path:
	// $archives/character/nh/common/script/seq/nh_common_script.earc
	// the associated data_url is
	// data://character/nh/common/script/seq/nh_common_script.ebex@

	/**
	 * The magic number of an earc file
	 * When converted to a string it reads 'FARC'
	 */
	public static final int MAGIC = getNativeValue(0x43524146);// ASCII VALUE:FARC
	private EArcHeader header;
	private Path path;
	private transient FileChannel fileChannel;

	
	/**
	 * Creates a new {@link Earchive} object from the given path.
	 * This method will not throw any exception but will output to the error output if the path does not exist or the file the path resolves to is not an earc file.
	 * @param path the Path of a .earc file
	 * @return a new {@link Earchive} object
	 * 
	 * @see #isEArcFile(Path)
	 */
	public static Earchive create(Path path) {
		Earchive archive = null;
		if(isEArcFile(path)) {
			archive = new Earchive(path);
		} else {
			System.err.println(String.format("The given path, \"%s\" is not earchive. Returning null.", path));
		}
		return archive;
	}
	/**
	 * equivalent to invoking {@link #create(Path) create(}{@link Paths#get(String, String...) Paths.get(path,paths))}
	 * @param path the first path node
	 * @param paths child path nodes
	 * @return a new Earchive object
	 * @see #create(Path)
	 */
	public static Earchive create(String path, String...paths) {
		Path p = Paths.get(path, paths);
		Earchive archive = create(p);
		return archive;
	}
	
	private FileChannel getFileChannel() {
		if(fileChannel==null) {
			try {
				fileChannel=FileChannel.open(getPath(), StandardOpenOption.READ);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return fileChannel;
	}
	
	
	
	public int getFileCount() {
		return getHeader().getFileCount();
	}
	
	private int getMinDataBlockSize() {
		return getHeader().getMinDataBlockSize();
	}
	
	private int getMetadataLocation() {
		return getHeader().getMetadataLocation();
	}
	
	private int getUrlTableLocation() {
		return getHeader().getUrlTableLocation();
	}
	
	private int getPathTableLocation() {
		return getHeader().getPathTableLocation();
	}
	
	public EArcEntry getEntryAt(int index) {
		return getHeader().getEntryAt(index);
	}
	public EArcEntry getEntryAt(int index, ByteBuffer bb) {
		return getHeader().getEntryAt(index, bb);
	}
	public EArcEntry[] getEntries() {
		return getHeader().getEntries();
	}
	
	
	public ContainerNode<EArcEntry> getTree(){
		ContainerNode<EArcEntry> tree = getTree(null);
		return tree;
	}
	
	public ContainerNode<EArcEntry> getTree(ContainerNode<EArcEntry> root){
		EArcEntry[] pointers = getEntries();
		if(root==null) {
			root = ContainerNode.newRoot();
		}
//		Comparator<EArcEntry> cmpa = (a,b)->{return Long.compare(b.getExtractedSize(), a.getExtractedSize());};
		for(EArcEntry pointer : pointers) {
			LeafNode.addToAndGetContainer(root, pointer, pointer.getFilePath());
		}
		return root;
	}
	
	public EArcHeader getHeader() {
		if(header==null) {
			header=new EArcHeader(this::getFileChannel);
		}
		return header;
	}
	
	private Earchive(Path path) {
		this.path=path;
	}
	
	/**
	 * 
	 * @return this earc's file {@link Path} 
	 */
	public Path getPath() {
		return this.path;
	}

	/**
	 * 
	 * @param path
	 *            the path you intend on using to open the file
	 * @return this instance
	 */
	public Earchive setPath(Path path) {
		if (this.path == null || !this.path.equals(path)) {
			if(path==null||isEArcFile(path)) {
				closeFileChannel();
				this.path = path;
				header = null;
			} else {
				System.err.println(String.format("The file, \"%s\" is not an earc file.%n\tThis no changes have been made to this object (retaining original path of \"%s\")", path, this.path));
			}
		}
		return this;
	}
	
	private void closeFileChannel() {
		try {
			close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean exists(Path p) {
		boolean exists = false;
		if (p != null) {
			exists = Files.exists(p);
		}
		return exists;
	}

	/**
	 * This does a <b>very</b> simple magic number test to check if a file is an earc file
	 * @param p the path to test if it an earc file
	 * @return {@code true} if the file exists and equals {@link #MAGIC} otherwise {@code false}
	 */
	public static boolean isEArcFile(Path p) {
		boolean isEArch = false;
		ByteBuffer tinyBuffer = ByteBuffer.allocateDirect(Integer.BYTES).order(ByteOrder.nativeOrder());

		try {
			if (exists(p)) {
				FileChannel fc = FileChannel.open(p, StandardOpenOption.READ);
				if (fc.size() > 3) {
					fc.read(tinyBuffer);
					tinyBuffer.flip();
					int headerValue = tinyBuffer.getInt();
					isEArch = MAGIC == headerValue;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return isEArch;
	}

	
	
	/**
	 * This method is useful if you are copying and pasting hex values from a hex editor from left to right.
	 * This method is the same as invoking {@link #getNativeValue(int, ByteOrder) getNativeValue(value,}{@link ByteOrder#BIG_ENDIAN ByteOrder.BIG_ENDIAN)}
	 * @param value a {@link ByteOrder#BIG_ENDIAN big-endian} value 
	 * @return the value represented as the as value of this machines native byte ordering
	 */
	public static int getNativeValue(int value) {
		int nativeValue =  getNativeValue(value, ByteOrder.BIG_ENDIAN);
		return nativeValue;
	}
	/**
	 * 
	 * @param value the value to modify if valueEndianess is not the same as this machine byte ordering 
	 * @param valueEndianess the endianess of this value
	 * @return the value represented as the as value of this machines native byte ordering
	 */
	public static int getNativeValue(int value, ByteOrder valueEndianess) {
		int nativeValue = value; 
		if(valueEndianess==null) {
			valueEndianess=ByteOrder.BIG_ENDIAN;
		}
		if(valueEndianess!=ByteOrder.nativeOrder()) {
			nativeValue = Integer.reverseBytes(nativeValue);
		}
		return nativeValue;
	}

	/* (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		if(fileChannel!=null&&fileChannel.isOpen()) {
			fileChannel.close();
		}
		fileChannel=null;
	}
}

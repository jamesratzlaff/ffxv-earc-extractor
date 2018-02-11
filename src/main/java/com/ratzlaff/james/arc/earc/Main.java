package com.ratzlaff.james.arc.earc;


import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ratzlaff.james.arc.Earchive;
import com.ratzlaff.james.util.io.HexPrinter;

/**
 * 
 * @author James Ratzlaff
 *
 */
public class Main {
	private static final transient Logger LOG = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) throws Exception {
		
		if(args.length<1) {
			printUsage();
		} else {
			
			String inputFile = args[0];
			Earchive earch = Earchive.create(inputFile);
//			print(earch);
//			String outputDir = args.length>1?args[1]:"./";
			FileMetadataPointers pntr=earch.getFilePointersAt(0);
			DeflateSegment d = pntr.getDeflateSegments().get(0);
			ByteBuffer bb = d.getCompressedDataAsByteBuffer(null);
			System.out.println(pntr);
			HexPrinter.printAsHex(bb,(int)d.getAbsoluteDeflateDataOffset());
			
			
//			earch.getFilePointersAt(0).extractTo(Paths.get(outputDir));
			
			
			//extractAllFiles(earch, outputDir);
			
			
		}
	}
	
	private static void print(Earchive earch) {
		populateFilePointers(earch);
		System.out.println(earch.getHeader());
	}
	
	private static void populateFilePointers(Earchive earch) {
		Arrays.stream(earch.getMetadataPointers()).forEach(p -> {
			p.getDataUrl();
			p.getFilePath();
		});
	}
	
	public static void extractAllFiles(Earchive earch, String outputDir) {
		final Path out = Paths.get(outputDir);
		Arrays.stream(earch.getMetadataPointers()).forEach(p -> {
			p.getDataUrl();
			p.getFilePath();
			System.out.println(String.format("extracting \"%s\" to \"%s\"", p.getFilePath(),out));
			p.extractTo(out);
		});
	}
	
	private static void printUsage() {
		System.out.println("Usage: inputFile.earc [outputDirectory]");
	}
	
	private static String fmt(byte b) {
		String str = Integer.toHexString((int)(b&(0xFF)));
		if(str.length()<2) {
			str='0'+str;
		}
		return str.toUpperCase();
	}
	
	private static String toFormattedStr(byte[] bytes) {
		return toFormattedStr(bytes, 40, 0);
	}
	
	private static String toFormattedStr(byte[] bytes, int elementsPerLine, int pad) {
		String[] asStrs = asStrs(bytes,pad);
		String result = fmt("\n",elementsPerLine,asStrs);
		return result;
	}
	
	private static String fmt(String delimiter, int elementsPerDelimit,String...strs) {
		if(delimiter==null) {
			delimiter="\n";
		}
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<strs.length;i++) {
			if(i>0) {
				if(elementsPerDelimit>0&&i%elementsPerDelimit==0) {
					sb.append(delimiter);
				} else {
//					sb.append(' ');
				}
			}
			sb.append(strs[i]);
		}
		return sb.toString();
	}
	
	private static String[] asStrs(byte[] bytes) {
		return asStrs(bytes,0);
	}
	
	private static String[] asStrs(byte[] bytes, int pad) {
		pad=Math.max(pad, 0);
		String[] strs=new String[bytes.length+pad];
		
		for(int i=0;i<pad;i++) {
			strs[i]="00";
		}
		
		for(int i=pad;i<strs.length;i++) {
			byte b = bytes[i-pad];
			strs[i]=fmt(b);
		}
		return strs;
	}
	

	
}

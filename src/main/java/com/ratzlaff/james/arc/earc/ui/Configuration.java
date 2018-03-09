/**
 * 
 */
package com.ratzlaff.james.arc.earc.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

import com.ratzlaff.james.arc.earc.obfus.DeflateDeobfuscator;

/**
 * @author James Ratzlaff
 *
 */
public class Configuration implements Serializable {
	private static final transient Configuration config;
	static {
		config=new Configuration();
	}
	public static final String CONFIG_FOLDER_NAME = ".earcexplorer";
	public static final String CONFIG_FILE_NAME = "configuration.properties";

	private Path filePath;
	private Map<String,Integer> nameToIndexMap;
	private List<ConfigurationItem> items;
	

	public static Configuration get() {
		return config;
	}
	public <T> T getOrDefault(Class<?> owner, String name, Function<String,T> func, T def, Function<T,String> toString){
		T val=null;
		if(name!=null) {
			String nameToUse = ConfigurationItem.createSanitizedName(owner, name);
			ConfigurationItem configItem = getItem(nameToUse);
			if(configItem==null) {
				String asStr = toString!=null?toString.apply(def):String.valueOf(def);
				configItem = new ConfigurationItem(owner, name, asStr);
				add(configItem);
				try {
					save();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(configItem!=null) {
				val = configItem.getValue(func);
			}
		}
		return val;
	}
	
	public String getStringOrDefault(Class<?> owner, String name, String def){
		return getOrDefault(owner, name, (s)->s, def, (s)->s);
	}
	
	public <T> T getOrDefault(Class<?> owner, String name, Function<String,T> func, T def){
		return getOrDefault(owner, name, func, def, null);
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7993028549635347936L;

	private static Path getConfigFolder() {
		Path result = Paths.get(System.getProperty("user.home", "." + File.separator));
		String appDataDir = System.getenv("APPDATA");
		if (appDataDir != null) {
			result = result.resolve(appDataDir);
		}
		result = result.resolve(CONFIG_FOLDER_NAME);
		if (!Files.exists(result)) {
			try {
				Files.createDirectories(result);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	public static Path getDefaultConfigFilePath() {
		return getConfigFolder().resolve(CONFIG_FILE_NAME);
	}

	public Path getConfigurationFilePath() {
		if (filePath == null) {
			filePath = getDefaultConfigFilePath();
		}
		return filePath;
	}
	
	public ConfigurationItem getItem(String name) {
		getItems();
		Integer index = nameToIndexMap.get(name);
		if(index!=null) {
			return items.get(index);
		}
		return null;
	}
	
	
	public Configuration add(ConfigurationItem i) {
		if(i!=null) {
			List<ConfigurationItem> items = getItems();
			Integer existing = nameToIndexMap.get(i.name());
			if(existing!=null) {
				items.set(existing, i);
			} else {
				items.add(i);
				nameToIndexMap.put(i.name(), items.size()-1);
			}
		}
		return this;
	}
	
	public List<ConfigurationItem> getItems(){
		if(items==null) {
			try {
				items=load();
				nameToIndexMap=new HashMap<String,Integer>(items.size());
			} catch (IOException e) {
				e.printStackTrace();
			}
			for(int i=0;i<items.size();i++) {
				ConfigurationItem ci = items.get(i);
				nameToIndexMap.put(ci.name(), i);
			}
		}
		return items;
	}
	
	private List<ConfigurationItem> load() throws IOException{
		List<ConfigurationItem> items = new ArrayList<ConfigurationItem>();
		Path configPath = getConfigurationFilePath();
		if(!Files.exists(configPath)) {
				Files.createFile(configPath);
		}
		if(Files.exists(configPath)) {
			BufferedReader br=Files.newBufferedReader(configPath);
			ConfigurationItem ci=null;
			while((ci=ConfigurationItem.create(br))!=null) {
				items.add(ci);
			}
		}
		return items;
	}
	
	public void save() throws IOException {
		if(getItems()!=null) {
			BufferedWriter bw = Files.newBufferedWriter(getConfigurationFilePath(), StandardOpenOption.WRITE);
			getItems().forEach(element->{
				element.accept(bw);
			});
			bw.close();
		}
	}
	
	
	private Configuration() {

	}
	/**
	 * @param owner
	 * @param name
	 * @param func
	 * @param string
	 * @param toString
	 * @return
	 */
	public <T> long  getOrDefault(Class<DeflateDeobfuscator> owner, String name, LongFunction<String> func, String string,
			ToLongFunction<T> toString) {
		// TODO Auto-generated method stub
		return 0;
	}

}

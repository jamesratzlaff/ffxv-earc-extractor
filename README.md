# ffxv-earc-extractor
list and extract data from earc files (found in games such as Final Fantasy XV for PC)
Requires a minimum of Java 9
As and FYI, to use the GUI you need to also have javaFX (not sure if it comes bundled with openJDK)
To execute the GUI, run the EarcExplorer's main class. 
	- Drag-n-Drop an earc file into it. 
	- Navigate to a file or folder you want to extract
	- Drag-n-Drop said file or folder to where you want to extract it to in your operating system's GUI 
Note: I do not provide the keys to deobfuscate/decrypt offset and size related values for retail earchives.  You'll have to get those yourself. Google,reddit, or github can probably lead you to those if you are too cool to use a debugger.
Once you do have the keys go to the configuration file (run the application to generate the config file. the file name is configuration.properties. for windows it will be created in %APPDATA%\.earcexplorer\ and linux ~/.earcexplorer/) and enter the appropriate values.  These values can be in hex or decimal.

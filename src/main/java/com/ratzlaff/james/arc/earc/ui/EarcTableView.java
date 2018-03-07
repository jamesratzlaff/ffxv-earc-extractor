/**
 * 
 */
package com.ratzlaff.james.arc.earc.ui;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.ratzlaff.james.arc.Earchive;
import com.ratzlaff.james.arc.earc.ContainerNode;
import com.ratzlaff.james.arc.earc.EArcEntry;
import com.ratzlaff.james.arc.earc.EArchiveFile;
import com.ratzlaff.james.arc.earc.LeafNode;
import com.ratzlaff.james.arc.earc.TreeNode;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.SortEvent;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Callback;

/**
 * @author James Ratzlaff
 *
 */
public class EarcTableView {
	public static final String FOLDER = new String(Character.toChars(0x1F4C1));//"\uD83D\uDCC1";
	public static final String FILE = "\uD83D\uDCC4";
	public static final String CURVED_UP_ARROW='\u2934'+""+'\uFE0F';
	
	private List<Earchive> archives;
	private ContainerNode<EArcEntry> rootNode=ContainerNode.newRoot();
	
	private TableView<TreeNode<EArcEntry>> tableView;
	
	private ObservableList<TreeNode<EArcEntry>> observableList;
	
	private TreeNode<EArcEntry> currentNode;
	private TableColumn<TreeNode<EArcEntry>, String> nameColumn = createTableColumn(TreeNode::getNameInScopeOf,this::getCurrentScope,"Name");
	private TableColumn<TreeNode<EArcEntry>, String> dataUrl = createTableColumn("Data URL", EArcEntry::getDataUrl);
	private TableColumn<TreeNode<EArcEntry>, String> pathStr = createTableColumn(TreeNode::toString,"Path");
	private TableColumn<TreeNode<EArcEntry>, Integer> compressedSize = createTableColumn("Compressed Size", EArcEntry::getLength);
	private TableColumn<TreeNode<EArcEntry>, Integer> decompressedSize= createTableColumn("Decompressed Size", EArcEntry::getExtractedSize);
	
	private static final String[] fontNames = {"Apple Color Emoji","Segoe UI Emoji","NotoColorEmoji","Segoe UI Symbol","Android Emoji","EmojiSymbols","EmojiOne Mozilla"};
	private static final Font emojiFont = getFirstAvailableEmojiFont(); 
	
	private static Font getFirstAvailableEmojiFont() {
		String family = getFirstAvailableEmojiFontFamily();
		Font result = Font.font(family);
		return result;
	}
	
	private static String getFirstAvailableEmojiFontFamily() {
		List<String> available = getAvailableEmojiFontFamilies();
		if(available.isEmpty()) {
			return Font.getDefault().getFamily();
		}
		return available.get(0);
	}
	
	private static List<String> getAvailableEmojiFontFamilies(){
		List<String> available = getAvailableEmojiFontFamiliesIn(fontNames);
		return available;
	}
	
	private static List<String> getAvailableEmojiFontFamiliesIn(String...toLookFor){
		List<String> families = Font.getFamilies();
		List<String> available = getAvailableFontFamiliesIn(families, toLookFor);
		return available;
	}
	
	private static List<String> getAvailableFontFamiliesIn(List<String> fontFamilies, String...toLookFor){
		List<String> available = Arrays.stream(toLookFor).filter(family->hasFontFamily(family, fontFamilies)).collect(Collectors.toList());
		return available;
	}
	
	private static boolean hasFontFamily(String familyName, List<String> fontFamilies) {
		if(fontFamilies==null) {
			return false;
		}
		return fontFamilies.contains(familyName);
	}
	
	public TreeNode<EArcEntry> getCurrentNode(){
		return currentNode;
	}
	
	public ObservableList<TreeNode<EArcEntry>> getObservableList(){
		return observableList;
	}
	public void setCurrentNode(TreeNode<EArcEntry> current) {
		this.currentNode=current;
	}
	@SuppressWarnings("unchecked")
	public EarcTableView(Path path,Path...paths) {
		archives=path==null&&paths.length==0?new ArrayList<Earchive>():createEarchivesFromPaths(path, paths);
		rootNode=addNodesFromArchives(rootNode, archives);
		
		currentNode=rootNode;
		observableList = FXCollections.observableArrayList(rootNode.listNodes());
		tableView=new TableView<TreeNode<EArcEntry>>(observableList);
		LeafNode.valueAsStringFunction=null;
		nameColumn.setCellFactory(new Callback<TableColumn<TreeNode<EArcEntry>, String>, TableCell<TreeNode<EArcEntry>, String>>() {
            @Override
            public TableCell<TreeNode<EArcEntry>, String> call(TableColumn<TreeNode<EArcEntry>, String> param) {
            	return new TableCell<TreeNode<EArcEntry>, String>() {

                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        
                        if (!isEmpty()) {
                        	this.setFont(emojiFont);
//                            this.setTextFill(Color.RED);
//                            // Get fancy and change color based on data
//                            if(item.contains("@")) 
//                                this.setTextFill(Color.BLUEVIOLET);
                            setText(item);

                        } else {
                        	setText("");
                        }
                    }

                };
            }
        });
		tableView.getColumns().addAll(nameColumn,pathStr,dataUrl,compressedSize,decompressedSize);

		
		System.out.println(tableView.getSortOrder());
//		Callback<TableView<TreeNode<EArcEntry>>,Boolean> oldHandler = tableView.getSortPolicy();
//
//		tableView.setOnSort(new EventHandler<SortEvent<TableView<TreeNode<EArcEntry>>>>() {
//			
//			@Override
//			public void handle(SortEvent<TableView<TreeNode<EArcEntry>>> arg0) {
//				oldHandler.handle(arg0);
//			}
//
//			
//		});
	}
	public EarcTableView addArchiveFiles(List<File> files) {
		List<Path> paths = files.stream().map(f->f.toPath()).collect(Collectors.toList());
		List<Earchive> asArchives = new ArrayList<Earchive>(paths.size());
		for(Path p : paths) {
			try {
				Earchive ea = Earchive.create(p);
				asArchives.add(ea);
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		return addArchives(asArchives);
	}
	public EarcTableView addArchives(List<Earchive> archives) {
		this.archives.addAll(archives);
		addNodesFromArchives(rootNode, archives);
		getObservableList().clear();
		getObservableList().addAll(currentNode.listNodes());
		getTableView().sort();
		return this;
		
	}
	
	public TableView<TreeNode<EArcEntry>> getTableView(){
		return this.tableView;
	}
	
	public static <T,V> TableColumn<TreeNode<T>,V> createTableColumn(String name, Function<T,V> getter){
		TableColumn<TreeNode<T>,V> tc = new TableColumn<>(name);
		tc.setCellValueFactory(new Callback<CellDataFeatures<TreeNode<T>, V>, ObservableValue<V>>() {
            public ObservableValue<V> call(CellDataFeatures<TreeNode<T>, V> p) {
            	T val = p.getValue().getValue();
            	V value = null;
            	if(getter!=null&&val!=null) {
            		value=getter.apply(val);
            	}
            	return new ReadOnlyObjectWrapper<V>(value);
            }
         });
		return tc;
	}
	
	public TreeNode<EArcEntry> getCurrentScope(){
		return currentNode;
	}
	
	public static <T,V> TableColumn<TreeNode<T>,V> createTableColumn(BiFunction<TreeNode<T>,TreeNode<T>,V> getter,Supplier<TreeNode<T>> scope, String name){
		TableColumn<TreeNode<T>,V> tc = new TableColumn<>(name);
		tc.setCellValueFactory(new Callback<CellDataFeatures<TreeNode<T>, V>, ObservableValue<V>>() {
            public ObservableValue<V> call(CellDataFeatures<TreeNode<T>, V> p) {
            	TreeNode<T> val = p.getValue();
            	V value = null;
            	if(getter!=null&&val!=null) {
            		value=getter.apply(val,scope.get());
            	}
            	if(value!=null) {
            		String prepend = FILE;
            		if(!val.isLeaf()) {
            			prepend=FOLDER;
            			if("..".equals(value)) {
            				prepend=CURVED_UP_ARROW;
            			}
            		}
            		value=(V)(prepend+" "+value);
            	}
            	return new ReadOnlyObjectWrapper<V>(value);
            }
         });
		return tc;
	}

	public static <T,V> TableColumn<TreeNode<T>,V> createTableColumn(Function<TreeNode<T>,V> getter, String name){
		TableColumn<TreeNode<T>,V> tc = new TableColumn<>(name);
		tc.setCellValueFactory(new Callback<CellDataFeatures<TreeNode<T>, V>, ObservableValue<V>>() {
            public ObservableValue<V> call(CellDataFeatures<TreeNode<T>, V> p) {
            	TreeNode<T> val = p.getValue();
            	V value = null;
            	if(getter!=null&&val!=null) {
            		value=getter.apply(val);
            	}
            	return new ReadOnlyObjectWrapper<V>(value);
            }
         });
		return tc;
	}
	
	
	public static ContainerNode<EArcEntry> addNodesFromArchives(ContainerNode<EArcEntry> root, Collection<Earchive> archives){
		ContainerNode<EArcEntry> container = root!=null?root:ContainerNode.newRoot();
		archives.forEach(archive->archive.getTree(container));
		return container;
	}
	
	private static List<Earchive> createEarchivesFromPaths(Path path, Path...paths){
		List<Path> allPaths = new ArrayList<Path>(paths.length+(path==null?0:1));
		List<Earchive> result = new ArrayList<Earchive>(allPaths.size());
		if(path!=null) {
			allPaths.add(path);
		}
		if(paths.length>0) {
			allPaths.addAll(Arrays.asList(paths));
		}
		result = allPaths.stream().filter(el->el!=null).map(Earchive::create).collect(Collectors.toList());
		return result;
		
	}
	
	
	
	
	
	
	
	
}

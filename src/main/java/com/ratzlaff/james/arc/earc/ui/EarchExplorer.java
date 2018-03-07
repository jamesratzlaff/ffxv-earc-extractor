package com.ratzlaff.james.arc.earc.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.ratzlaff.james.arc.earc.ContainerNode;
import com.ratzlaff.james.arc.earc.EArcEntry;
import com.ratzlaff.james.arc.earc.LeafNode;
import com.ratzlaff.james.arc.earc.TreeNode;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class EarchExplorer extends Application {

	@Override
	public void start(Stage stage) {
		stage.setTitle("EArchive Explorer");
		stage.setMaximized(true);

		EarcTableView etv = new EarcTableView(null/*Paths.get("C:\\Program Files\\SquareEnix\\FINAL FANTASY XV BENCHMARK\\datas\\character\\sm\\sm03\\entry\\sm03_000.earc")*/);
		TableView<TreeNode<EArcEntry>> tableView = etv.getTableView();
		CheckBox flatView = new CheckBox("flat");

		tableView.setRowFactory(tv -> {
			TableRow<TreeNode<EArcEntry>> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && (!row.isEmpty())) {
					TreeNode<EArcEntry> rowData = row.getItem();
					if (!rowData.isLeaf()) {
						etv.setCurrentNode(rowData);
						etv.getObservableList().clear();
						if (!flatView.isSelected()) {
							etv.getObservableList().addAll(rowData.listNodes());
						} else {
							etv.getObservableList()
									.addAll(((ContainerNode<EArcEntry>) etv.getCurrentNode()).getAllChildren());
						}
						tableView.sort();

					}
				}
			});
			return row;
		});
		Path temp=null;
		try {
			temp = Files.createTempDirectory("lerfin");
			temp.toFile().deleteOnExit();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		tableView.setOnDragDetected(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {

				// for paste as file, e.g. in Windows Explorer
				try {
					TreeNode<EArcEntry> sourceFile = tableView.getSelectionModel().getSelectedItem();
					if(sourceFile!=null) {
					List<File> output = new ArrayList<File>();
					Clipboard clipboard = Clipboard.getSystemClipboard();
					Dragboard db = tableView.startDragAndDrop(TransferMode.ANY);

					ClipboardContent content = new ClipboardContent();
					
					
					String name = "herp";
					String ext = "der";
					Path temp = Files.createTempDirectory(name);
					if(sourceFile.isLeaf()) {
						EArcEntry entry = ((LeafNode<EArcEntry>)sourceFile).getValue();
						File o = entry.write(temp);
						output.add(o);
					} else {
						List<EArcEntry> kids = ((ContainerNode<EArcEntry>)sourceFile).getAllChildren().stream().filter(child->child.isLeaf()).map(leaf->(LeafNode<EArcEntry>)leaf).map(leaf->leaf.getValue()).collect(Collectors.toList());
						for(EArcEntry kid : kids) {
							File f = kid.write(temp);
							output.add(f);
						}
					}
					Path copyPath = temp.resolve(Paths.get(sourceFile.toString()));
					
					
//					Files.copy(copyPath, temp.resolve(sourceFile.getName()),
//							StandardCopyOption.COPY_ATTRIBUTES);

					content.putFiles(java.util.Collections.singletonList(copyPath.toFile()));
					db.setContent(content);
					clipboard.setContent(content);
					}
					event.consume();

				} catch (IOException ex) {
					ex.printStackTrace();
				}

			}
		});
		
		

		VBox dragTarget = new VBox(flatView, tableView);
		tableView.setOnDragOver(new EventHandler<DragEvent>() {

            @Override
            public void handle(DragEvent event) {
                if (event.getGestureSource() != dragTarget
                        && event.getDragboard().hasFiles()) {
                    /* allow for both copying and moving, whatever user chooses */
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }
                event.consume();
            }
        });

        tableView.setOnDragDropped(new EventHandler<DragEvent>() {

            @Override
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                boolean success = false;
                System.out.println(event.getSource());
                if (db.hasFiles()) {
                	etv.addArchiveFiles(db.getFiles());
                    success = true;
                }
                /* let the source know whether the string was successfully 
                 * transferred and used */
                event.setDropCompleted(success);

                event.consume();
            }
        });
		tableView.setPrefHeight(1080);
		flatView.setOnAction(e -> {
			etv.getObservableList().clear();
			if (flatView.isSelected()) {
				etv.getObservableList().addAll(((ContainerNode<EArcEntry>) etv.getCurrentNode()).getAllChildren());
			} else {
				etv.getObservableList().addAll(etv.getCurrentNode().listNodes());
			}
			tableView.sort();
		});
		// Pane p = new Pane(flatView,tableView);
		// p.setPrefSize(prefWidth, prefHeight);
		stage.setScene(new Scene(dragTarget));
		stage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
}

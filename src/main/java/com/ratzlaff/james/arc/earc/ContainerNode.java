/**
 * 
 */
package com.ratzlaff.james.arc.earc;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author James Ratzlaff
 *
 */
public class ContainerNode<T> extends TreeNode<T> {

	private static final Logger LOG = LoggerFactory.getLogger(ContainerNode.class);
	private Map<String,TreeNode<T>> children;

	
	public static <T> ContainerNode<T> newRoot(){
		return new ContainerNode<T>();
	}
	
	/**
	 * 
	 */
	protected ContainerNode() {
		super();
	}
	
	private static <T> void createDotDot(ContainerNode<T> parent,ContainerNode<T> child){
		if(child!=null&&(parent!=null&&!child.getChildren().containsKey(".."))) {
			child.getChildren().put("..", parent);
		}
	}

	/**
	 * @param parent
	 * @param name
	 */
	public ContainerNode(ContainerNode<T> parent, String name) {
		super(parent, name);
		if(parent!=null&&!isRoot()) {
			createDotDot(parent,this);
		}
		
	}
	
	@Override
	public ContainerNode<T> getParent(){
		return (ContainerNode<T>)super.getParent();
	}
	
	@Override
	public boolean hasChildren() {
		return this.children!=null&&!getChildren().isEmpty();
	}
	public Map<String,TreeNode<T>> getChildren(){
		if(this.children==null) {
			this.children=new LinkedHashMap<String,TreeNode<T>>();
		}
		return this.children;
	}
	
	private Set<TreeNode<T>> getAllChildren(boolean first){
		Set<TreeNode<T>> all =new LinkedHashSet<TreeNode<T>>();
		for(String key : getChildren().keySet()) {
			if(!first&&"..".equals(key)) {
				continue;
			}
			TreeNode<T> current = getChildren().get(key);
			all.add(current);
			if(current instanceof ContainerNode) {
				if(!"..".equals(key)) {
					all.addAll(((ContainerNode<T>) current).getAllChildren(false));
				}
			}
		}
		return all;
	}
	
	public Set<TreeNode<T>> getAllChildren(){
		Set<TreeNode<T>> all = getAllChildren(true);
		return all;
	}
	
	

	/**
	 * 
	 * @param children
	 * @return the last node added in the given list of children
	 */
	public TreeNode<T> addChildrenRecursive(String...children) {
		ContainerNode<T> currentParent = this;
		TreeNode<T> result = currentParent;
		for(int i=0;i<children.length&&currentParent!=null;i++) {
			String childName = children[i];
			if(childName==null||childName.trim().isEmpty()) {
				continue;
			}
			TreeNode<T> child = currentParent.getChildren().get(childName);
			if(child==null) {
				child = new ContainerNode<T>(currentParent,childName);
			}
			if(child instanceof ContainerNode) {
				currentParent=(ContainerNode<T>)child;
			} else {
				currentParent=null;
			}
			result=child;
			if(currentParent==null&&i<children.length-1) {
				LOG.warn("A non-container node is being referenced as a ContainerNode.");
			}
		}
		return result;
	}
	
	@Override
	public boolean isLeaf() {
		return false;
	}
	
	
}

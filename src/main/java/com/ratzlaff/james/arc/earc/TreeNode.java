/**
 * 
 */
package com.ratzlaff.james.arc.earc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author James Ratzlaff
 *
 */
public abstract class TreeNode<T> implements Comparable<TreeNode<T>> {
	private static final Logger LOG = LoggerFactory.getLogger(TreeNode.class);
	private String name;
	protected TreeNode<T> parent;
	private final TreeNode<T> root;
	public static final String SEPARATOR = "/";

	protected TreeNode() {
		this(null, "", true);
	}

	protected String getSeparator() {
		return SEPARATOR;
	}

	public abstract boolean isLeaf();

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof TreeNode))
			return false;
		TreeNode other = (TreeNode) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		return true;
	}

	public TreeNode(TreeNode<T> parent, String name) {
		this(parent, name, false);
	}

	private TreeNode(TreeNode<T> parent, String name, boolean isRoot) {
		Objects.requireNonNull(name, "A TreeNode cannot have a null name");
		if (name.trim().isEmpty()) {
			if (parent != null) {

			} else if (!isRoot) {
				throw new RuntimeException(String.format(
						"Creating a root node manually using constructor 'new %s(TreeNode<T> parent,String name)' is not allowed",
						getClass().getSimpleName()));
			}
		}
		this.parent = (parent == null && !name.isEmpty() ? ContainerNode.newRoot() : parent);
		setName(name);
		if (this.parent != null) {
			root = this.parent.getRoot();
		} else {
			root = this;
		}
		if (getParent() != null) {
			getParent().addChild(this);
		}
	}

	public TreeNode<T> removeChild(String name) {
		TreeNode<T> removed = name != null ? getChildren().remove(name) : null;
		if (removed != null) {
			removed.parent = null;
		}

		return removed;
	}

	/**
	 * 
	 * @param oldName
	 * @param newName
	 * @return the Node that has been renamed, or null if the node with
	 *         {@code oldName} doesn't exist or it wasn't/couldn't-be renamed
	 */
	private TreeNode<T> renameChild(String oldName, String newName) {
		TreeNode<T> renamedNode = null;
		if (oldName != null) {
			if (oldName.equals(newName)) {
				return renamedNode;
			}
		}
		if (newName != null && hasChild(newName)) {
			LOG.debug("Cannot rename '{}' to '{}' because an existing node with the name '{}' already exists{}{}.",
					oldName, newName, newName, !isRoot() ? " in " : "", !isRoot() ? this.toString() : "");
			return renamedNode;
		}
		if (hasChildren()) {
			renamedNode = getChildren().remove(oldName);
			if (renamedNode != null) {
				String currentName = renamedNode.getName();
				if (currentName == null) {
					if (newName == null) {
						renamedNode.parent = null;
					} else {
						renamedNode.setName(newName);
					}
				} else if (!currentName.equals(newName)) {
					renamedNode.setName(newName);
				}
			}
		}
		if (renamedNode != null && renamedNode.parent != null) {
			renamedNode.parent.addChild(renamedNode);
		}
		return renamedNode;
	}

	public boolean hasChild(String child) {
		return child != null && hasChildren() && getChildren().containsKey(child);
	}

	public boolean setParent(TreeNode<T> node) {
		if (isRoot()) {
			return false;
		} else if (node != null) {
			if (!node.equals(this.getParent())) {
				TreeNode<T> oldParent = runawayFromParent();
				if (!node.addChild(this)) {
					if (oldParent != null) {
						oldParent.addChild(this);
					}
					return false;
				}
			} else {
				return false;
			}
		} else {
			runawayFromParent();
		}
		return true;
	}

	/**
	 * 
	 * @return the parent that was previously set to this node
	 */
	private TreeNode<T> runawayFromParent() {
		TreeNode<T> oldParent = this.getParent();
		if (oldParent != null) {
			oldParent.removeChild(this.getName());
		}
		return oldParent;
	}

	public String setName(String name) {
		String oldName = this.name;
		boolean changed = false;
		boolean delete = false;
		if (name != null) {
			if (this.parent != null) {
				if (name.trim().isEmpty()) {
					throw new RuntimeException(
							String.format("The given name '%s', must be non-empty when trimmed", name));
				}
			}
			if (!name.equals(oldName)) {
				this.name = name;
				changed = oldName != null;
			}
		} else {
			if (this.parent != null) {
				if (oldName != null) {
					this.name = name;
					changed = true;
					delete = true;
				}
			}
		}
		if (changed) {
			if (delete) {
				if (this.parent != null) {
					this.parent.removeChild(oldName);
				}
			} else {
				if (this.parent != null)
					if (this.parent.renameChild(oldName, name) == null) {
						this.name = oldName;
						oldName = null;
					}
			}
		}
		return oldName;
	}

	public TreeNode<T> getParent() {
		return parent;
	}
	
	public final String getNameInScopeOf(TreeNode<?> scope) {
		String toReturn=name;
		if(scope!=null) {
			if(scope.getParent()!=null&&scope.getParent()==this) {
				return "..";
			}
		}
		return toReturn;
	}
	
	public final String getName() {
		return getNameInScopeOf(null);
	}

	public TreeNode<T> getRoot() {
		return root;
	}

	public boolean isParentOf(TreeNode<T> node) {
		boolean truth = false;
		if (node != null) {
			TreeNode<T> current = node.getParent();
			while (current != null && !truth) {
				truth = current.equals(this);
				current = current.getParent();
			}
		}
		return truth;
	}

	public int getDepth() {
		int result = -1;
		if (!this.isRoot()) {
			if (this.getParent() != null) {
				result += this.getParent().getDepth();
			}
		}
		return result;
	}

	public boolean isChildOf(TreeNode<T> other) {
		boolean truth = false;
		if (other != null && !this.equals(other)) {
			truth = other.isParentOf(this);
		}
		return truth;
	}

	private void getLeafNodes(Set<LeafNode<T>> found) {
		if (found == null) {
			found = new LinkedHashSet<LeafNode<T>>();
		}
		if (this.isLeaf()) {
			found.add((LeafNode<T>) this);
			// return found;
		}
		if (hasChildren()) {
			for (TreeNode<T> kid : getChildren().values()) {
				System.out.println(kid);
				kid.getLeafNodes(found);
			}
		}
		// return found;
	}

	public Set<LeafNode<T>> getLeafNodes() {
		Set<LeafNode<T>> found = new LinkedHashSet<LeafNode<T>>();
		getLeafNodes(found);
		return found;
	}

	protected boolean addChild(TreeNode<T> node) {
		Map<String, TreeNode<T>> kids = getChildren();
		if (kids.containsKey(node.getName())) {
			LOG.debug("Node '{}' already contains a child named '{}'", this, node.getName());
			return false;
		}
		kids.put(node.getName(), node);
		if (node.parent == null || !node.parent.equals(this)) {
			node.runawayFromParent();
			node.parent = this;
		}

		return true;
	}
	
	private TreeNode<T> getSingleChild(String str){
		TreeNode<T> child = null;
		if("..".equals(str)) {
			if(!isRoot()) {
				child=this.getParent();
			} else {
				return this;
			}
		} else {
			child=this.getChildren().get(str);
		}
		return child;
	}
	
	private TreeNode<T> getChild(List<String> strs){
		TreeNode<T> child = null;
		if (hasChildren()&&!strs.isEmpty()) {
			child=this;
			for(int i=0;i<strs.size()&&child!=null;i++) {
				String str = strs.get(i);
				child=getSingleChild(str);
			}
		}
		return child;
	}
	
	public TreeNode<T> getChild(String name, String... names) {
		List<String> strs=normalize(name, names);
		TreeNode<T> child = getChild(strs);
		return child;
	}

	public boolean hasChildren() {
		return getChildren() != null && !getChildren().isEmpty();
	}

	public Map<String, TreeNode<T>> getChildren() {
		return Collections.emptyMap();
	}

	public final boolean isRoot() {
		return (this == root) || (this.parent == null && "".equals(name));
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (!isRoot()) {
			sb.append(getParent().toString());
			if (!getParent().isRoot()) {
				sb.append('/');
			}
		}
		sb.append(getName());
		return sb.toString();
	}

	public T getValue() {
		return null;
	}

	private Stack<TreeNode<T>> getAsStack() {
		Stack<TreeNode<T>> st = new Stack<TreeNode<T>>();
		TreeNode<T> current = this;
		while (!current.isRoot()) {
			st.push(current);
			current = current.getParent();
		}
		return st;
	}
	
	private Stack<TreeNode<?>> getAsStackGen() {
		Stack<TreeNode<?>> st = new Stack<TreeNode<?>>();
		TreeNode<T> current = this;
		while (!current.isRoot()) {
			st.push(current);
			current = current.getParent();
		}
		return st;
	}
	
	public static <T> Comparator<TreeNode<T>> createComparator(Comparator<T> comparator){
		return (a,b)->{
			int cmp=0;
			T aVal = a.getValue();
			T bVal = b.getValue();
			if(aVal!=null) {
				if(bVal!=null) {
					cmp=comparator.compare(aVal,bVal);
				} else {
					cmp=1;
				}
			} else if(bVal!=null) {
				cmp=-1;
			}
			return cmp;
		};
	}
	
	
	public List<TreeNode<T>> listNodesSortedByValue(Comparator<T> comparator){
		Comparator<TreeNode<T>> asTreeComp = comparator!=null?createComparator(comparator):null;
		List<TreeNode<T>> sorted = listNodesSorted(asTreeComp);
		return sorted;
	}
	
	public List<TreeNode<T>> listNodesSorted(Comparator<TreeNode<T>> comparator){
		List<TreeNode<T>> nodes = listNodes();
		if(nodes!=null) {
			if(comparator!=null) {
				Collections.sort(nodes,comparator);
			} else {
				Collections.sort(nodes,compy);
			}
		}
		return nodes;
	}
	
	public List<TreeNode<T>> listNodes(){
		List<TreeNode<T>> result = null;
		
		if(isRoot()||hasChildren()) {
			result = new ArrayList<TreeNode<T>>(getChildren().values().size());
			result.addAll(getChildren().values());
			
		}
		return result;
	}
	
	
	private static final Comparator<TreeNode<?>> compy = (ths,o)->{
		if (o == null) {
			return 1;
		}
		int cmp = 0;
		
		if (cmp == 0) {
			Stack<TreeNode<?>> myStack = ths.getAsStackGen();
			Stack<TreeNode<?>> oStack = o.getAsStackGen();

			while (cmp == 0) {
				if (myStack.isEmpty()) {
					if (!oStack.isEmpty()) {
						cmp = -1;
					} else {
						break;
					}

				} else if (oStack.isEmpty()) {
					cmp = 1;
				}
				if (cmp == 0) {
					TreeNode<?> myPop = myStack.pop();
					TreeNode<?> oPop = oStack.pop();
					String myName = myPop.getNameInScopeOf(ths);
					String oName = oPop.getNameInScopeOf(ths);
					if("..".equals(myName)) {
						cmp=-1;
					} else if("..".equals(oName)) {
						cmp=1;
					} else {
						cmp=myName.compareTo(oName);
					}
				}
			}

			// TreeNode<T> myParent = this.getParent();
			// if(myParent!=null) {
			// cmp=myParent.compareTo(o.getParent());
			// } else if(o.getParent()!=null) {
			// cmp=-1;
			// }
			// if(cmp==0) {
			// cmp=getName().compareTo(o.getName());
			// }
		}
		return cmp;

	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(TreeNode<T> o) {
		int cmp = compy.compare(this,o);
		return cmp;
	}
	
	public static List<String> normalize(String delim, String firstPath, String... pathSegments) {	
		List<String> asStrs = Arrays.stream(pathSegments).filter(path -> path != null)
				.flatMap(path -> Arrays.stream(path.split(delim!=null?delim:SEPARATOR))).collect(Collectors.toList());
		if (firstPath != null) {
			List<String> firstPathAsList = Arrays.asList(firstPath.split(delim!=null?delim:SEPARATOR));
			if (asStrs.isEmpty()) {
				asStrs.addAll(firstPathAsList);
			} else {
				asStrs.addAll(0, firstPathAsList);
			}
		}
		return asStrs;
	}
	
	public static List<String> normalize(String firstPath, String...pathSegments){
		List<String> strs = normalize(SEPARATOR,firstPath,pathSegments);
		return strs;
	}

}

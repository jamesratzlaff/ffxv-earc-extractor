/**
 * 
 */
package com.ratzlaff.james.arc.earc;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author James Ratzlaff
 *
 */
public class LeafNode<T> extends TreeNode<T> {
	private static final Function<?, String> DEFAULT_AS_STRING = String::valueOf;

	private Comparator<T> sorting;
	private final T value;
	@SuppressWarnings("unchecked")
	public static transient Function<?, String> valueAsStringFunction = (Function<?, String>) DEFAULT_AS_STRING;

	public LeafNode(ContainerNode<T> parent, String name, T value) {
		super(parent, name);
		this.value = value;
	}
	
	public static <T> Function<T,String> getValueAsStringFunction(){
		return (Function<T,String>)valueAsStringFunction;
	}

	public static <T> void setValueAsStringFunction(Function<T, String> func) {
		valueAsStringFunction = func;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ratzlaff.james.arc.earc.TreeNode#removeChild(java.lang.String)
	 */
	@Override
	public TreeNode<T> removeChild(String name) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ratzlaff.james.arc.earc.TreeNode#hasChild(java.lang.String)
	 */
	@Override
	public boolean hasChild(String child) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ratzlaff.james.arc.earc.TreeNode#isParentOf(com.ratzlaff.james.arc.earc.
	 * TreeNode)
	 */
	@Override
	public boolean isParentOf(TreeNode<T> node) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ratzlaff.james.arc.earc.TreeNode#addChild(com.ratzlaff.james.arc.earc.
	 * TreeNode)
	 */
	@Override
	protected boolean addChild(TreeNode<T> node) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ratzlaff.james.arc.earc.TreeNode#getChild(java.lang.String,
	 * java.lang.String[])
	 */
	@Override
	public TreeNode<T> getChild(String name, String... names) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ratzlaff.james.arc.earc.TreeNode#hasChildren()
	 */
	@Override
	public boolean hasChildren() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ratzlaff.james.arc.earc.TreeNode#getChildren()
	 */
	@Override
	public Map<String, TreeNode<T>> getChildren() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof LeafNode)) {
			return false;
		}
		LeafNode other = (LeafNode) obj;
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!value.equals(other.value)) {
			return false;
		}
		return true;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		if (valueAsStringFunction != null) {
			sb.append(" => ").append(getValueAsStringFunction().apply(getValue()));
		}
		return sb.toString();
	}

	public boolean isLeaf() {
		return true;
	}

	public ContainerNode<T> getRoot() {
		return (ContainerNode<T>) super.getRoot();
	}

	public LeafNode<T> setComparator(Comparator<T> comparator) {
		this.sorting = comparator;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ratzlaff.james.arc.earc.TreeNode#compareTo(com.ratzlaff.james.arc.earc.
	 * TreeNode)
	 */
	@Override
	public int compareTo(TreeNode<T> o) {
		int cmp = 0;
		if (sorting == null) {
			cmp = super.compareTo(o);
		}
		if (cmp == 0) {
			if (o == null) {
				cmp = 1;
			}
		}
		if (cmp == 0) {
			if (o instanceof LeafNode) {
				cmp = this.sorting.compare(getValue(), o.getValue());
			} else {
				cmp = super.compareTo(o);
			}
		}
		return cmp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ratzlaff.james.arc.earc.TreeNode#getValue()
	 */
	@Override
	public T getValue() {
		return this.value;
	}

	@SuppressWarnings("unchecked")
	private static <T, N extends TreeNode<T>> N addTo(Class<N> leafOrContainer, ContainerNode<T> container, T value,
			String firstPath, String... pathSegments) {
		if (container == null) {
			container = ContainerNode.newRoot();
		}
		List<String> strs = normalize(firstPath, pathSegments);
		String lastNode = strs.remove(strs.size() - 1);
		ContainerNode<T> parent = (ContainerNode<T>) container
				.addChildrenRecursive(strs.toArray(new String[strs.size()]));
		LeafNode<T> leaf = new LeafNode<T>(parent, lastNode, value);
		return (ContainerNode.class == leafOrContainer) ? (N) container : (N) leaf;
	}

	public static <T> ContainerNode<T> addToAndGetContainer(ContainerNode<T> container, T value, String firstPath,
			String... pathSegments) {
		LeafNode<T> leaf = addToAndGetLeaf(container, value, firstPath, pathSegments);
		if (container == null) {
			container = leaf.getRoot();
		}
		return container;
	}

	public static <T> LeafNode<T> createLeaf(T value, String firstPath, String... pathSegments) {
		return addToAndGetLeaf(null, value, firstPath, pathSegments);
	}

	@SuppressWarnings("unchecked")
	public static <T> LeafNode<T> addToAndGetLeaf(ContainerNode<T> container, T value, String firstPath,
			String... pathSegments) {
		return addTo(LeafNode.class, container, value, firstPath, pathSegments);
	}


	
	
}

/**
 * 
 */
package com.ratzlaff.james.arc.earc.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author James Ratzlaff
 *
 */
public class ConfigurationItem implements Serializable, Consumer<Appendable> {

	public static final Function<ConfigurationItem, String> DEFAULT_SUPPLIER = (c) -> c.getDefaultValue();

	/**
	 * 
	 */
	private static final long serialVersionUID = -5589470783924211799L;
	private String fullyQualifiedName;
	private Supplier<String> valueSupplier;
	private List<String> comments;
	private String defaultValue;
	private String oldName;
	private boolean markedForDeletion = false;

	public static ConfigurationItem create(BufferedReader r) {

		ConfigurationItem item = null;
		String name = null;
		String value = null;
		List<String> comments = new ArrayList<String>();
		String line = "";

		try {
			while ((line = r.readLine()) != null) {
				String trimmed = line.trim();
				if (line.startsWith("#")) {
					comments.add(trimmed);
				} else {
					int eqIndex = trimmed.indexOf('=');
					if (eqIndex > 0) {
						name = trimmed.substring(0, eqIndex).trim();
						value = trimmed.substring(eqIndex+1).trim();
						break;
					}

				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		if (name != null) {
			item = new ConfigurationItem(name, value, null, comments.toArray(new String[comments.size()]));
		}
		return item;
	}

	private static String sanitizeName(String fullyQualifiedName) {
		String sanitized = fullyQualifiedName != null && !fullyQualifiedName.trim().isEmpty()
				? fullyQualifiedName.trim()
				: null;
		if (sanitized != null) {
			boolean valid = Character.isJavaIdentifierStart(sanitized.codePointAt(0));
			if (valid && sanitized.length() > 1) {
				fullyQualifiedName.codePoints().skip(1).allMatch(c -> Character.isJavaIdentifierPart(c));
			}
			if (!valid) {
				sanitized = null;
			}
		}
		return sanitized;
	}
	
	public static String createSanitizedName(Class<?> owner, String name) {
		String nameToUse = name;
		if (owner != null) {
			if (nameToUse != null && !nameToUse.isEmpty()) {
				nameToUse = String.join(".", owner.getName(), nameToUse);
			}

		}
		Objects.requireNonNull(nameToUse, "A null name has been provided");
		String[] splitUp = nameToUse.split("\\.");
		List<String> asList = Arrays.stream(splitUp).map(str -> sanitizeName(str)).collect(Collectors.toList());
		if (asList.stream().anyMatch(str -> str == null)) {
			throw new RuntimeException(String.format("An invalid name has been provided %s", asList));
		}
		nameToUse = String.join(".", asList);
		return nameToUse;

	}

	public ConfigurationItem(Class<?> owner, String name, String defaultValue, Supplier<String> valueSupplier,
			String... comments) {
		String nameToUse = createSanitizedName(owner, name);

		this.fullyQualifiedName = nameToUse;
		this.defaultValue = defaultValue;
		this.valueSupplier = valueSupplier != null ? valueSupplier : this::getDefaultValue;
		this.comments = new ArrayList<String>(comments.length);
		if (comments.length > 0) {
			this.comments.addAll(Arrays.asList(comments));
		}
	}

	public ConfigurationItem(String fullyQualifiedName, Supplier<String> valueSupplier, String... comments) {
		this(fullyQualifiedName, null, valueSupplier, comments);
	}

	public ConfigurationItem(Class<?> owner, String name) {
		this(owner, name, null, null);
	}

	public ConfigurationItem(Class<?> owner, String name, String defaultValue) {
		this(owner, name, defaultValue, null);
	}

	public ConfigurationItem(Class<?> owner, String name, Supplier<String> supplier) {
		this(owner, name, null, supplier);
	}

	public ConfigurationItem(String fullyQualifiedName, String defaultValue, Supplier<String> valueSupplier,
			String... comments) {
		this(null, fullyQualifiedName, defaultValue, valueSupplier, comments);
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public String getValue() {
		String result = null;
		if (this.valueSupplier != null) {
			result = this.valueSupplier.get();
		}
		if (result == null) {
			result = getDefaultValue();
		}
		return result;
	}

	public ConfigurationItem name(String newName) {
		if (newName != null) {
			if (markedForDeletion) {
				markedForDeletion = false;
			}
			if (!newName.equals(this.fullyQualifiedName)) {

				if (newName.equals(oldName)) {
					fullyQualifiedName = newName;
					oldName = null;
				} else {
					oldName = fullyQualifiedName;
					fullyQualifiedName = newName;
				}
			}
		} else {
			markedForDeletion = true;
		}
		return this;
	}

	public ConfigurationItem defaultValue(String value) {
		defaultValue = value;
		return this;
	}

	public List<String> comments() {
		return comments;
	}

	public ConfigurationItem comment(String comment) {
		if (comment != null) {
			comments().add(comment);
		}
		return this;
	}

	public ConfigurationItem comments(String comment, String... comments) {
		comment(comment);
		comments().addAll(Arrays.asList(comments).stream().filter(comm -> comm != null).collect(Collectors.toList()));
		return this;
	}

	public ConfigurationItem supplier(Supplier<String> supplier) {
		this.valueSupplier = supplier;
		return this;
	}

	@SuppressWarnings("unchecked")
	public <T> ConfigurationItem applyTo(Function<String, T> converter, Consumer<T>... consumers) {
		T value = getValue(converter);
		for (Consumer<T> consumer : consumers) {
			consumer.accept(value);
		}
		return this;
	}

	public String name() {
		return fullyQualifiedName;
	}

	public <T> T getValue(Function<String, T> converter) {
		T result = null;
		if (converter != null) {
			result = converter.apply(getValue());
		}
		return result;
	}

	public void accept(Appendable w) {
		if (w != null) {

			try {
				comments().forEach(comment -> {
					try {
						w.append("\n#").append(comment.replaceAll("([\\n]+)", "$1#").replaceAll("[#]+", "#"));
					} catch (IOException e) {
						e.printStackTrace();
					}
				});

				w.append('\n').append(name()).append("=").append(getValue());
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}

		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		accept(sb);
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fullyQualifiedName == null) ? 0 : fullyQualifiedName.hashCode());
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
		if (!(obj instanceof ConfigurationItem)) {
			return false;
		}
		ConfigurationItem other = (ConfigurationItem) obj;
		if (fullyQualifiedName == null) {
			if (other.fullyQualifiedName != null) {
				return false;
			}
		} else if (!fullyQualifiedName.equals(other.fullyQualifiedName)) {
			return false;
		}
		return true;
	}

}

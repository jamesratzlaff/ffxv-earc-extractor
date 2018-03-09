/**
 * 
 */
package com.ratzlaff.james.arc.earc.obfus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author James Ratzlaff
 *
 */
public class EventTrackingKeyGen extends KeyGen implements Timestamped<EventTrackingKeyGen> {

	private List<KeyChangeEvent> events;
	private long timestamp;

	/**
	 * @param parentKey
	 */
	public EventTrackingKeyGen(long parentKey) {
		super(parentKey);
	}

	/**
	 * @param parentKey
	 * @param majorKey
	 * @param minorKey
	 */
	public EventTrackingKeyGen(long parentKey, long majorKey, long minorKey) {
		super(parentKey, majorKey, minorKey);
	}

	private void addEvent(KeyChangeEvent event) {
		if(events==null&&event!=null) {
			events=new ArrayList<KeyChangeEvent>();
		}
		if (event != null) {
			if(events.isEmpty() ) {
				timestamp=System.currentTimeMillis();
			}
			events.add(event);
		}
	}

	/* (non-Javadoc)
	 * @see com.ratzlaff.james.arc.earc.obfus.AbstractKeyGen#beforeSettingTransientKey(long)
	 */
	@Override
	protected void beforeSettingTransientKey(long newVal) {
		addEvent(KeyChangeEvent.KEYS.TRANSIENT.apply(this, newVal));
		super.beforeSettingTransientKey(newVal);
	}

	/* (non-Javadoc)
	 * @see com.ratzlaff.james.arc.earc.obfus.AbstractKeyGen#beforeSettingOffsetKey(long)
	 */
	@Override
	protected void beforeSettingOffsetKey(long newVal) {
		addEvent(KeyChangeEvent.KEYS.OFFSET.apply(this, newVal));
		super.beforeSettingOffsetKey(newVal);
	}

	/* (non-Javadoc)
	 * @see com.ratzlaff.james.arc.earc.obfus.AbstractKeyGen#beforeSettingLengthKey(long)
	 */
	@Override
	protected void beforeSettingLengthKey(long newVal) {
		addEvent(KeyChangeEvent.KEYS.LENGTH.apply(this, newVal));
		super.beforeSettingLengthKey(newVal);
	}

	/* (non-Javadoc)
	 * @see com.ratzlaff.james.arc.earc.obfus.AbstractKeyGen#beforeSettingParentKey(long)
	 */
	@Override
	protected void beforeSettingParentKey(long newVal) {
		addEvent(KeyChangeEvent.KEYS.PARENT.apply(this, newVal));
		super.beforeSettingParentKey(newVal);
	}

	/* (non-Javadoc)
	 * @see com.ratzlaff.james.arc.earc.obfus.AbstractKeyGen#beforeSettingMajorKey(long)
	 */
	@Override
	protected void beforeSettingMajorKey(long newVal) {
		addEvent(KeyChangeEvent.KEYS.MAJOR.apply(this, newVal));
		super.beforeSettingMajorKey(newVal);
	}

	/* (non-Javadoc)
	 * @see com.ratzlaff.james.arc.earc.obfus.AbstractKeyGen#beforeSettingMinorKey(long)
	 */
	@Override
	protected void beforeSettingMinorKey(long newVal) {
		addEvent(KeyChangeEvent.KEYS.MINOR.apply(this, newVal));
		super.beforeSettingMinorKey(newVal);
	}
	
	

	/* (non-Javadoc)
	 * @see com.ratzlaff.james.arc.earc.obfus.AbstractKeyGen#beforeSettingMorphingKey(long)
	 */
	@Override
	protected void beforeSettingMorphingKey(long newValue) {
		addEvent(KeyChangeEvent.KEYS.MORPHING.apply(this, newValue));
		super.beforeSettingMorphingKey(newValue);
	}

	public void clearEvents() {
		events.clear();
	}

	public void writeEventsTo(Appendable writer) {
		boolean first = true;
		for (KeyChangeEvent event : events) {
			if (!first) {
				try {
					writer.append(",\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				first=false;
			}
			try {
				writer.append(event.toString((l) -> {
					return String.valueOf(l - getTimestamp());
				}));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.ratzlaff.james.arc.earc.obfus.Timestamped#getTimestamp()
	 */
	@Override
	public long getTimestamp() {
		return timestamp;
	}

	/* (non-Javadoc)
	 * @see com.ratzlaff.james.arc.earc.obfus.AbstractKeyGen#afterSettingParentKey()
	 */
	@Override
	protected void afterSettingParentKey() {
		// TODO Auto-generated method stub
		super.afterSettingParentKey();
	}

}

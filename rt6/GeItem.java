package org.powerbot.script.rt6;

/**
 * {@inheritDoc}
 */
public class GeItem extends org.powerbot.script.GeItem {

	/**
	 * {@inheritDoc}
	 */
	public GeItem(final int id) {
		super("rs", id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.powerbot.script.GeItem nil() {
		return new GeItem(0);
	}
}

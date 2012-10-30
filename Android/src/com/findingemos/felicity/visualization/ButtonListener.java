package com.findingemos.felicity.visualization;

/**
 * Listener for notifying push events.
 * 
 * @author Niels
 * @version 0.1
 */
public interface ButtonListener {
	/**
	 * Notified when the button is down.
	 */
	public void isDown();

	/**
	 * Notified when the button is up.
	 */
	public void isUp();
}

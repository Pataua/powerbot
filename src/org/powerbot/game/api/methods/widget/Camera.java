package org.powerbot.game.api.methods.widget;

import java.awt.event.KeyEvent;

import org.powerbot.game.api.methods.Game;
import org.powerbot.game.api.methods.input.Keyboard;
import org.powerbot.game.api.util.Random;
import org.powerbot.game.api.util.Time;
import org.powerbot.game.api.util.Timer;
import org.powerbot.game.bot.Bot;

/**
 * @author Timer
 */
public class Camera {
	public static int getX() {
		final Bot bot = Bot.resolve();
		return bot.getClient().getCamPosX() * bot.multipliers.GLOBAL_CAMPOSX;
	}

	public static int getY() {
		final Bot bot = Bot.resolve();
		return bot.getClient().getCamPosY() * bot.multipliers.GLOBAL_CAMPOSY;
	}

	public static int getZ() {
		final Bot bot = Bot.resolve();
		return bot.getClient().getCamPosZ() * bot.multipliers.GLOBAL_CAMPOSZ;
	}

	public static int getYaw() {
		final Bot bot = Bot.resolve();
		return (int) ((bot.getClient().getCameraYaw() * bot.multipliers.GLOBAL_CAMERAYAW) / 45.51);
	}

	public static int getPitch() {
		final Bot bot = Bot.resolve();
		return (int) (((bot.getClient().getCameraPitch() * bot.multipliers.GLOBAL_CAMERAPITCH) - 1024) / 20.48);
	}

	public static boolean setPitch(final boolean up) {
		if (up) {
			return setPitch(100);
		}
		return setPitch(0);
	}

	public static boolean setPitch(final int percent) {
		int curAlt = getPitch();
		int lastAlt = 0;
		if (curAlt == percent) {
			return true;
		}

		final boolean up = curAlt < percent;
		Keyboard.pressKey(up ? (char) KeyEvent.VK_UP : (char) KeyEvent.VK_DOWN, 0, 0);

		final Timer timer = new Timer(100);
		while (timer.isRunning()) {
			if (lastAlt != curAlt) {
				timer.reset();
			}

			lastAlt = curAlt;
			Time.sleep(Random.nextInt(5, 10));
			curAlt = getPitch();

			if (up && curAlt >= percent) {
				break;
			} else if (!up && curAlt <= percent) {
				break;
			}
		}

		Keyboard.releaseKey(up ? (char) KeyEvent.VK_UP : (char) KeyEvent.VK_DOWN, 0, 0);
		return curAlt == percent;
	}

	public static void setAngle(final char direction) {
		switch (direction) {
		case 'n':
			setAngle(0);
			break;
		case 'w':
			setAngle(90);
			break;
		case 's':
			setAngle(180);
			break;
		case 'e':
			setAngle(270);
			break;
		default:
			setAngle(0);
			break;
		}
	}

	public static boolean setNorth() {
		return WidgetComposite.getCompass().click(true);
	}

	public static boolean setNorth(final int up) {
		return WidgetComposite.getCompass().click(true) && setPitch(up);
	}

	public static void setAngle(final int degrees) {
		if (getAngleTo(degrees) > 5) {
			Keyboard.pressKey((char) KeyEvent.VK_LEFT, 0, 0);
			while (getAngleTo(degrees) > 5 && Game.getClientState() == 11) {
				Time.sleep(10);
			}
			Keyboard.releaseKey((char) KeyEvent.VK_LEFT, 0, 0);
		} else if (getAngleTo(degrees) < -5) {
			Keyboard.pressKey((char) KeyEvent.VK_RIGHT, 0, 0);
			while (getAngleTo(degrees) < -5 && Game.getClientState() == 11) {
				Time.sleep(10);
			}
			Keyboard.releaseKey((char) KeyEvent.VK_RIGHT, 0, 0);
		}
	}

	public static int getAngleTo(final int degrees) {
		int ca = getYaw();
		if (ca < degrees) {
			ca += 360;
		}
		int da = ca - degrees;
		if (da > 180) {
			da -= 360;
		}
		return da;
	}
}

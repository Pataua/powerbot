package org.powerbot.script.xenon;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.powerbot.bot.Bot;
import org.powerbot.game.client.BaseInfo;
import org.powerbot.game.client.Client;
import org.powerbot.game.client.RSGround;
import org.powerbot.game.client.RSGroundInfo;
import org.powerbot.game.client.RSInfo;
import org.powerbot.game.client.RSObject;
import org.powerbot.script.xenon.util.Filter;
import org.powerbot.script.xenon.wrappers.GameObject;
import org.powerbot.script.xenon.wrappers.Player;
import org.powerbot.script.xenon.wrappers.Tile;

public class Objects {
	private static final int LOADED_DIST = 104;

	public static Set<GameObject> getLoaded() {
		return getLoaded(LOADED_DIST);
	}

	public static Set<GameObject> getLoaded(int _x, int _y, final int range) {
		final Set<GameObject> objects = new LinkedHashSet<>();
		final Client client = Bot.client();
		if (client == null) return objects;

		final RSInfo info;
		final BaseInfo baseInfo;
		final RSGroundInfo groundInfo;
		final RSGround[][][] grounds;
		if ((info = client.getRSGroundInfo()) == null || (baseInfo = info.getBaseInfo()) == null ||
				(groundInfo = info.getRSGroundInfo()) == null || (grounds = groundInfo.getRSGroundArray()) == null)
			return null;

		final GameObject.Type[] types = {
				GameObject.Type.BOUNDARY, GameObject.Type.BOUNDARY,
				GameObject.Type.FLOOR_DECORATION,
				GameObject.Type.WALL_DECORATION, GameObject.Type.WALL_DECORATION
		};

		_x -= baseInfo.getX();
		_y -= baseInfo.getY();
		final int plane = client.getPlane();

		final RSGround[][] objArr = plane > -1 && plane < grounds.length ? grounds[plane] : null;
		if (objArr == null) return objects;
		for (int x = Math.max(0, _x - range); x <= Math.min(_x + range, objArr.length - 1); x++) {
			for (int y = Math.max(0, _y - range); y <= Math.min(_y + range, objArr[x].length - 1); y++) {
				final RSGround ground = objArr[x][y];
				if (ground == null) continue;

				final RSObject[] objs = {
						ground.getBoundary1(), ground.getBoundary2(),
						ground.getFloorDecoration(),
						ground.getWallDecoration1(), ground.getWallDecoration2()
				};
				for (int i = 0; i < objs.length; i++) {
					if (objs[i] != null) {
						objects.add(new GameObject(objs[i], types[i]));
					}
				}
			}
		}
		return objects;

	}

	public static Set<GameObject> getLoaded(final int range) {
		final Player player = Players.getLocal();
		final Tile location;
		if (player == null || (location = player.getLocation()) == null) {
			return new HashSet<>(0);
		}

		final int x = location.getX(), y = location.getY();
		return getLoaded(x, y, range);
	}

	public static Set<GameObject> getLoaded(final Filter<GameObject> filter) {
		return getLoaded(LOADED_DIST, filter);
	}

	public static Set<GameObject> getLoaded(final int range, final Filter<GameObject> filter) {
		final Set<GameObject> items = getLoaded(range);
		final Set<GameObject> set = new HashSet<>(items.size());
		for (final GameObject item : items) if (filter.accept(item)) set.add(item);
		return set;
	}

	public static GameObject getNearest(final Filter<GameObject> filter) {
		return getNearest(LOADED_DIST, filter);
	}

	public static GameObject getNearest(final int range, final Filter<GameObject> filter) {
		GameObject nearest = null;
		double dist = 104d;

		final Player local = Players.getLocal();
		if (local == null) return null;

		final Tile pos = local.getLocation();
		if (pos == null) return null;
		final Set<GameObject> gameObjects = getLoaded(range);
		for (final GameObject gameObject : gameObjects) {
			final double d;
			if (filter.accept(gameObject) && (d = Calculations.distance(pos, gameObject)) < dist) {
				nearest = gameObject;
				dist = d;
			}
		}

		return nearest;
	}
}

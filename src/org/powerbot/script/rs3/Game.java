package org.powerbot.script.rs3;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.concurrent.Callable;

import org.powerbot.bot.rs3.client.BaseInfo;
import org.powerbot.bot.rs3.client.Client;
import org.powerbot.bot.rs3.client.Constants;
import org.powerbot.bot.rs3.client.DXRender;
import org.powerbot.bot.rs3.client.GLRender;
import org.powerbot.bot.rs3.client.HardReference;
import org.powerbot.bot.rs3.client.HashTable;
import org.powerbot.bot.rs3.client.JavaRender;
import org.powerbot.bot.rs3.client.Node;
import org.powerbot.bot.rs3.client.RSGroundBytes;
import org.powerbot.bot.rs3.client.RSGroundInfo;
import org.powerbot.bot.rs3.client.RSInfo;
import org.powerbot.bot.rs3.client.Render;
import org.powerbot.bot.rs3.client.RenderData;
import org.powerbot.bot.rs3.client.SoftReference;
import org.powerbot.bot.rs3.client.TileData;
import org.powerbot.script.Condition;

public class Game extends ClientAccessor {
	public static final int INDEX_LOGIN_SCREEN = 3;
	public static final int INDEX_LOBBY_SCREEN = 7;
	public static final int INDEX_LOGGING_IN = 9;
	public static final int INDEX_MAP_LOADED = 11;
	public static final int INDEX_MAP_LOADING = 12;
	public static final int[] SIN_TABLE = new int[16384];
	public static final int[] COS_TABLE = new int[16384];

	static {
		final double d = 0.0003834951969714103d;
		for (int i = 0; i < 16384; i++) {
			SIN_TABLE[i] = (int) (16384d * Math.sin(i * d));
			COS_TABLE[i] = (int) (16384d * Math.cos(i * d));
		}
	}

	public final Game.Toolkit toolkit;
	public final Game.Viewport viewport;

	public int mapAngle;

	public Game(final ClientContext factory) {
		super(factory);
		this.toolkit = new Toolkit();
		this.viewport = new Viewport();
	}

	/**
	 * An enumeration of the possible cross-hairs in game.
	 *
	 */
	public enum Crosshair {
		NONE, DEFAULT, ACTION
	}

	/**
	 * Logs out of the game into either the lobby or login screen.
	 *
	 * @param lobby <tt>true</tt> for the lobby; <tt>false</tt> for the login screen
	 * @return <tt>true</tt> if successfully logged out; otherwise <tt>false</tt>
	 */
	public boolean logout(final boolean lobby) {
		if (ctx.hud.open(Hud.Menu.OPTIONS)) {
			final Widget widget = ctx.widgets.get(1433);
			if (Condition.wait(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					return widget.valid();
				}
			}, 100, 10)) {
				if (!widget.getComponent(lobby ? 12 : 13).interact("Select")) {
					return false;
				}
			}
		}
		return Condition.wait(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return getClientState() == (lobby ? INDEX_LOBBY_SCREEN : INDEX_LOGIN_SCREEN);
			}
		});
	}

	/**
	 * Returns the current client state.
	 *
	 * @return the client state
	 * @see Game#INDEX_LOGIN_SCREEN
	 * @see Game#INDEX_LOBBY_SCREEN
	 * @see Game#INDEX_LOGGING_IN
	 * @see Game#INDEX_MAP_LOADED
	 * @see Game#INDEX_MAP_LOADING
	 */
	public int getClientState() {
		final Client client = ctx.client();
		final Constants constants = ctx.constants.get();
		if (client == null || constants == null) {
			return -1;
		}
		final int state = client.getLoginIndex();
		if (state == constants.CLIENTSTATE_3) {
			return 3;
		} else if (state == constants.CLIENTSTATE_7) {
			return 7;
		} else if (state == constants.CLIENTSTATE_9) {
			return 9;
		} else if (state == constants.CLIENTSTATE_11) {
			return 11;
		} else if (state == constants.CLIENTSTATE_12) {
			return 12;
		}
		return -1;
	}

	/**
	 * Determines if the player is logged into the game.
	 *
	 * @return <tt>true</tt> if logged in; otherwise <tt>false</tt>
	 */
	public boolean isLoggedIn() {
		final int state = getClientState();
		return state == INDEX_MAP_LOADED || state == INDEX_MAP_LOADING;
	}

	/**
	 * Determines the current {@link Crosshair} displayed.
	 *
	 * @return the displayed {@link Crosshair}
	 */
	public Crosshair getCrosshair() {
		final Client client = ctx.client();
		final int type = client != null ? client.getCrossHairType() : -1;
		if (type < 0 || type > 2) {
			return Crosshair.NONE;
		}
		return Crosshair.values()[type];
	}

	/**
	 * Determines the base of the loaded region.
	 *
	 * @return the {@link Tile} of the base
	 */
	public Tile getMapBase() {
		final Client client = ctx.client();
		if (client == null) {
			return Tile.NIL;
		}

		final RSInfo info = client.getRSGroundInfo();
		final BaseInfo baseInfo = info != null ? info.getBaseInfo() : null;
		return baseInfo != null ? new Tile(baseInfo.getX(), baseInfo.getY(), client.getPlane()) : Tile.NIL;
	}

	/**
	 * Determines the current floor level.
	 *
	 * @return the current floor level
	 */
	public int getPlane() {
		final Client client = ctx.client();
		if (client == null) {
			return -1;
		}
		return client.getPlane();
	}

	/**
	 * Determines the size of the game space
	 *
	 * @return the {@link Dimension}s of the game space
	 */
	public Dimension getDimensions() {
		final Client client = ctx.client();
		final Canvas canvas;
		if (client == null || (canvas = client.getCanvas()) == null) {
			return new Dimension(0, 0);
		}
		return new Dimension(canvas.getWidth(), canvas.getHeight());
	}

	/**
	 * Determines if a point is in the viewport.
	 *
	 * @param point the point to check
	 * @return <tt>true</tt> if the point is in the viewport; otherwise <tt>false</tt>
	 */
	public boolean isPointInViewport(final Point point) {
		return isPointInViewport(point.x, point.y);
	}

	/**
	 * Determines if a point is in the viewport.
	 *
	 * @param x the x-coordinate
	 * @param y the y-coordinate
	 * @return <tt>true</tt> if the point is in the viewport; otherwise <tt>false</tt>
	 */
	public boolean isPointInViewport(final int x, final int y) {
		final Dimension dimension = getDimensions();
		if (x > 0 && y > 0) {
			if (isLoggedIn()) {
				final Rectangle[] rectangles = ctx.hud.getBounds();
				for (final Rectangle rectangle : rectangles) {
					if (rectangle.contains(x, y)) {
						return false;
					}
				}
			}
			return x < dimension.getWidth() && y < dimension.getHeight();
		}
		return false;
	}

	/**
	 * Determines the tile height at the provided point in the game region.
	 *
	 * @param rX    the relative x
	 * @param rY    the relative y
	 * @param plane the plane
	 * @return the height at the given point
	 */
	public int tileHeight(final int rX, final int rY, int plane) {
		final Client client = ctx.client();
		if (client == null) {
			return 0;
		}
		if (plane == -1) {
			plane = client.getPlane();
		}
		final RSInfo world = client.getRSGroundInfo();
		final RSGroundBytes ground = world != null ? world.getGroundBytes() : null;
		final byte[][][] settings = ground != null ? ground.getBytes() : null;
		if (settings != null) {
			final int x = rX >> 9;
			final int y = rY >> 9;
			if (x < 0 || x > 103 || y < 0 || y > 103) {
				return 0;
			}
			if (plane < 3 && (settings[1][x][y] & 2) != 0) {
				++plane;
			}
			final RSGroundInfo worldGround = world.getRSGroundInfo();
			final TileData[] groundPlanes = worldGround != null ? worldGround.getTileData() : null;
			if (groundPlanes == null || plane < 0 || plane >= groundPlanes.length) {
				return 0;
			}
			final TileData groundData = groundPlanes[plane];
			if (groundData == null) {
				return 0;
			}
			final int[][] heights = groundData.getHeights();
			if (heights != null) {
				final int aX = rX & 0x1ff;
				final int aY = rY & 0x1ff;
				final int start_h = heights[x][y] * (512 - aX) + heights[x + 1][y] * aX >> 9;
				final int end_h = heights[x][1 + y] * (512 - aX) + heights[x + 1][y + 1] * aX >> 9;
				return start_h * (512 - aY) + end_h * aY >> 9;
			}
		}

		return 0;
	}

	/**
	 * Determines an in-view point of the given point in the game region.
	 *
	 * @param x      the relative x position
	 * @param y      the relative y position
	 * @param plane  the plane
	 * @param height the height offset
	 * @return the {@link Point} in game space
	 */
	public Point groundToScreen(final int x, final int y, final int plane, final int height) {
		if (x < 512 || y < 512 || x > 52224 || y > 52224) {
			return new Point(-1, -1);
		}
		final int h = tileHeight(x, y, plane) + height;
		return worldToScreen(x, h, y);
	}

	/**
	 * Transforms the given matrix (3D) into a game screen (2D) point.
	 *
	 * @param x the x
	 * @param y the y
	 * @param z the depth
	 * @return the {@link Point} in game space, otherwise {@code new Point(-1, -1)}
	 */
	public Point worldToScreen(final int x, final int y, final int z) {
		final float _z = (viewport.zOff + (viewport.zX * x + viewport.zY * y + viewport.zZ * z));
		final float _x = (viewport.xOff + (viewport.xX * x + viewport.xY * y + viewport.xZ * z));
		final float _y = (viewport.yOff + (viewport.yX * x + viewport.yY * y + viewport.yZ * z));
		if (_x >= -_z && _x <= _z && _y >= -_z && _y <= _z) {
			return new Point(
					Math.round(toolkit.absoluteX + (toolkit.xMultiplier * _x) / _z),
					Math.round(toolkit.absoluteY + (toolkit.yMultiplier * _y) / _z)
			);
		}
		return new Point(-1, -1);
	}

	/**
	 * Calculates a point on the mini-map.
	 *
	 * @param locatable the {@link Locatable} to convert to map point
	 * @return the map {@link Point}
	 */
	public Point tileToMap(final Locatable locatable) {
		final Point bad = new Point(-1, -1);
		final Client client = ctx.client();
		final Tile b = ctx.game.getMapBase();
		final Tile t = locatable.getLocation().derive(-b.getX(), -b.getY());
		final int tx = t.getX();
		final int ty = t.getY();
		if (client == null || tx < 1 || tx > 103 || ty < 1 || ty > 103) {
			return bad;
		}

		final RelativeLocation r = ctx.players.local().getRelative();
		final float offX = (tx * 4 - r.getX() / 128) + 2;
		final float offY = (ty * 4 - r.getY() / 128) + 2;
		final int d = (int) Math.round(Math.sqrt(Math.pow(offX, 2) + Math.pow(offY, 2)));

		final Component component = ctx.widgets.get(1465, 12);
		final int w = component.getScrollWidth();
		final int h = component.getScrollHeight();
		final int radius = Math.max(w / 2, h / 2) + 10;
		if (d >= radius) {
			return bad;
		}

		final Constants constants = ctx.constants.get();
		final int v = constants != null ? constants.MINIMAP_SETTINGS_ON : -1;
		final boolean f = client.getMinimapSettings() == v;

		final double a = (ctx.camera.getYaw() * (Math.PI / 180d)) * 2607.5945876176133d;
		int i = 0x3fff & (int) a;
		if (!f) {
			i = 0x3fff & client.getMinimapOffset() + (int) a;
		}
		int sin = SIN_TABLE[i], cos = COS_TABLE[i];
		if (!f) {
			final int scale = 256 + client.getMinimapScale();
			sin = 256 * sin / scale;
			cos = 256 * cos / scale;
		}

		int rotX = (int) (cos * offX + sin * offY) >> 14;
		int rotY = (int) (cos * offY - sin * offX) >> 14;
		rotX += w / 2;
		rotY *= -1;
		rotY += h / 2;

		if (rotX > 4 && rotX < w - 4 &&
				rotY > 4 && rotY < h - 4) {
			final Point basePoint = component.getAbsoluteLocation();
			final int sX = rotX + (int) basePoint.getX();
			final int sY = rotY + (int) basePoint.getY();
			final Point p = new Point(sX, sY);
			final Rectangle rbuffer = new Rectangle(p.x - 6, p.y - 6, 12, 12);//entire tile and a half sized 'buffer' area
			for (int pos = 17; pos <= 21; pos++) {
				if (ctx.widgets.get(1465, pos).getViewportRect().intersects(rbuffer)) {
					return bad;
				}
			}
			return p;
		}

		return bad;
	}

	public void updateToolkit(final Render render) {
		if (render == null) {
			return;
		}
		toolkit.absoluteX = render.getAbsoluteX();
		toolkit.absoluteY = render.getAbsoluteY();
		toolkit.xMultiplier = render.getXMultiplier();
		toolkit.yMultiplier = render.getYMultiplier();
		toolkit.graphicsIndex = render.getGraphicsIndex();
		if (render instanceof DXRender) {
			toolkit.gameMode = 2;
		} else if (render instanceof GLRender) {
			toolkit.gameMode = 1;
		} else if (render instanceof JavaRender) {
			toolkit.gameMode = 0;
		} else {
			toolkit.gameMode = -1;
		}

		final Constants constants = ctx.constants.get();
		final RenderData _viewport = render.getRenderData();
		final float[] data;
		if (viewport == null || constants == null || (data = _viewport.getFloats()) == null) {
			return;
		}
		viewport.xOff = data[constants.VIEWPORT_XOFF];
		viewport.xX = data[constants.VIEWPORT_XX];
		viewport.xY = data[constants.VIEWPORT_XY];
		viewport.xZ = data[constants.VIEWPORT_XZ];

		viewport.yOff = data[constants.VIEWPORT_YOFF];
		viewport.yX = data[constants.VIEWPORT_YX];
		viewport.yY = data[constants.VIEWPORT_YY];
		viewport.yZ = data[constants.VIEWPORT_YZ];

		viewport.zOff = data[constants.VIEWPORT_ZOFF];
		viewport.zX = data[constants.VIEWPORT_ZX];
		viewport.zY = data[constants.VIEWPORT_ZY];
		viewport.zZ = data[constants.VIEWPORT_ZZ];
	}

	/**
	 * Looks up a reference in the provided hash table.
	 *
	 * @param nc the hash table
	 * @param id the reference id
	 * @return the found reference, or null.
	 */
	public Object lookup(final HashTable nc, final long id) {
		final Node[] buckets;
		if (nc == null || (buckets = nc.getBuckets()) == null || id < 0) {
			return null;
		}

		final Node n = buckets[(int) (id & buckets.length - 1)];
		for (Node node = n.getNext(); node != n && node != null; node = node.getNext()) {
			if (node.getId() == id) {
				if (node instanceof SoftReference) {
					return ((java.lang.ref.SoftReference<?>) ((SoftReference) node).get()).get();
				} else if (node instanceof HardReference) {
					return ((HardReference) node).get();
				} else {
					return node;
				}
			}
		}
		return null;
	}

	public class Toolkit {
		public float absoluteX, absoluteY;
		public float xMultiplier, yMultiplier;
		public int gameMode, graphicsIndex;
	}

	public class Viewport {
		public float xOff, xX, xY, xZ;
		public float yOff, yX, yY, yZ;
		public float zOff, zX, zY, zZ;
	}
}
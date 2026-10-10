package jks.tools2d.parallax.editor.gvars;

import static jks.tools2d.parallax.editor.vue.Vue_Edition.parallax_Heart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.color.ExtendedColorPicker;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane;

import jks.tools2d.parallax.ParallaxLayer;
import jks.tools2d.parallax.editor.vue.edition.VE_Center_ParallaxShow;
import jks.tools2d.parallax.editor.vue.edition.VE_Options;
import jks.tools2d.parallax.editor.vue.edition.VE_Tab_AControl;
import jks.tools2d.parallax.editor.vue.edition.VE_Tab_TextureList_Adding;
import jks.tools2d.parallax.editor.vue.edition.data.ParallaxDefaultValues;
import jks.tools2d.parallax.editor.vue.edition.data.Position_Infos;
import jks.tools2d.parallax.editor.vue.edition.data.Project_Data;
import jks.tools2d.parallax.editor.vue.edition.data.Project_Infos;
import jks.tools2d.parallax.editor.vue.edition.utils.Utils_Texture;
import jks.tools2d.parallax.editor.vue.edition.utils.WatchedImage;
import jks.tools2d.parallax.pages.Enum_LayerKind;
import jks.tools2d.parallax.pages.Parallax_Model;
import jks.tools2d.parallax.pages.Sequence_Segment;
import jks.tools2d.parallax.pages.WholePage_Model;

/** State of the edition view (one project open at a time). */
public final class GVars_Vue_Edition
{
	/**
	 * The four main tab titles take about 21 times the font size (314 px at 15 px, GVars_UI.fontSize): any narrower and
	 * the tab bar wraps onto a second row that covers the top of the tab content.
	 */
	private static final float TABS_PER_FONT_PX = 21.5f;

	public static int size_Bloc_Selection_Parallax_Width;
	public static int size_Bloc_Parallax;
	public static int size_Height_Bloc_Parallax_Controle;
	public static int sizeTabsBar;

	/** Every image that can be added as a layer: the atlas regions, then the loose PNG files. */
	public static ArrayList<TextureRegion> allImage = new ArrayList<>();

	/** Where each image comes from, used when saving. */
	public static HashMap<TextureRegion, Position_Infos> imageRef = new HashMap<>();
	/** Layers currently drawing each image. */
	public static HashMap<TextureRegion, ArrayList<ParallaxLayer>> textureLink = new HashMap<>();
	/** Loose PNG files of the project, by path. */
	public static HashMap<String, TextureRegion> outsideTextureReserve = new HashMap<>();
	/** Loose PNG files reloaded when they change on disk, by path. */
	public static HashMap<String, WatchedImage> activeFileWatching = new HashMap<>();

	public static int parr_Size_X;
	public static int parr_Size_Y;
	public static int parr_Pos_X;
	public static int parr_Pos_Y;

	public static boolean isPause = true;

	public static ParallaxLayer currentlySelectedParallax;

	public static TabbedPane tabbedPane;

	/** Picker receiving the color under the mouse on the next click in the preview (eyedropper), or null. */
	public static ExtendedColorPicker colorPicked;

	public static Project_Infos projectInfos;
	public static Project_Data projectDatas;

	public static Array<ParallaxLayer> trashedValues = new Array<>();
	public static Array<Integer> trashedValuesPosition = new Array<>();

	/** Folder of the open project: atlases and relative image paths are resolved from it. */
	public static String relativePath;
	public static TextureAtlas atlas;
	/** Some layers of the opened file could not be loaded (missing atlas or loose image), so the editor lacks them. */
	public static boolean loadedIncompletely;

	public static boolean showParallaxFullScreen = false;

	public static VE_Center_ParallaxShow centerControl;
	public static VE_Tab_AControl tabControl;
	public static VE_Options optionsControl;

	public static float hideInterfaceTimmer;

	public static float timeForAutoSaveTimmer;
	public static final float timeForAutoSaveAt = 300;

	private GVars_Vue_Edition()
	{}

	/** Forgets everything about the previous project, releasing the watchers and loose textures it owned. */
	public static void clear()
	{
		for (WatchedImage watched : activeFileWatching.values())
			watched.cancel();
		for (TextureRegion region : outsideTextureReserve.values())
			region.getTexture().dispose();

		Utils_Texture.forgetPixelArt();
		allImage.clear();
		imageRef.clear();
		textureLink.clear();
		outsideTextureReserve.clear();
		activeFileWatching.clear();
		trashedValues.clear();
		trashedValuesPosition.clear();
		currentlySelectedParallax = null;
		colorPicked = null;
		atlas = null;
		loadedIncompletely = false;
		showParallaxFullScreen = false;
		isPause = true;
		timeForAutoSaveTimmer = 0;
	}

	public static ParallaxDefaultValues getDefaults()
	{return projectDatas.defaults;}

	public static void setDefaults(ParallaxDefaultValues defaults)
	{projectDatas.defaults = defaults;}

	public static void buildSizes()
	{
		size_Bloc_Selection_Parallax_Width = Math.max((int) (Gdx.graphics.getWidth() / 3.6f), (int) (GVars_UI.fontSize() * TABS_PER_FONT_PX) + 6);
		size_Bloc_Parallax = Gdx.graphics.getWidth() - size_Bloc_Selection_Parallax_Width;
		size_Height_Bloc_Parallax_Controle = (int) (Gdx.graphics.getHeight() / 5.5f);
		sizeTabsBar = Gdx.graphics.getWidth() / 40;
	}

	public static void selectLayer(ParallaxLayer layer)
	{
		currentlySelectedParallax = layer;
		if (getDefaults().autoGoToSelected)
			tabbedPane.switchTab(2);
	}

	/** Rebuilds the content of the visible tab after the layers or the selection changed. */
	public static void refreshActiveTab()
	{
		if (tabbedPane != null && tabbedPane.getActiveTab() != null)
			tabbedPane.getActiveTab().getContentTable();
	}

	public static void setPage(WholePage_Model parallaxPage)
	{
		parallax_Heart.setPage(parallaxPage);
		atlas = parallaxPage.getLoadedAtlas();

		for (int x = 0; x < parallaxPage.preloadValue.size(); x++)
		{
			ParallaxLayer layer = parallaxPage.preloadValue.get(x);
			Parallax_Model model = parallaxPage.pageModel.pageList.get(x);

			if (layer.kind == Enum_LayerKind.SEQUENCE)
			{
				// One region per segment, each named by its own segment, not by the layer.
				for (int i = 0; i < model.sequenceSegments.size(); i++)
				{
					Sequence_Segment segment = model.sequenceSegments.get(i);
					imageRef.put(layer.getTexRegion().get(i), new Position_Infos(outsideTextureReserve.get(segment.regionName) == null,
							segment.regionName, segment.regionPosition));
				}
			}
			else
				for (TextureRegion texture : regionsOf(layer))
					imageRef.put(texture, new Position_Infos(outsideTextureReserve.get(model.regionName) == null, model.regionName, model.regionPosition));

			addToLinks(layer);
		}
	}

	public static TextureAtlas getAtlas()
	{return atlas;}

	/** Pushes {@link #allImage} to the image list of the "Adding new" tab. */
	public static void setItems()
	{
		if (VE_Tab_TextureList_Adding.imageList == null)
			return;

		VE_Tab_TextureList_Adding.imageList.setItems(allImage.toArray(new TextureRegion[0]));
	}

	/** Links {@code layer} to every image it draws: a SEQUENCE layer to each of its segments'. */
	public static void addToLinks(ParallaxLayer layer)
	{
		for (TextureRegion region : new HashSet<>(regionsOf(layer)))
			textureLink.computeIfAbsent(region, k -> new ArrayList<>()).add(layer);
	}

	public static void removeFromLinks(ParallaxLayer layer)
	{
		for (TextureRegion region : regionsOf(layer))
		{
			ArrayList<ParallaxLayer> linked = textureLink.get(region);
			if (linked != null)
				linked.remove(layer);
		}
	}

	/**
	 * The images a layer draws: a SEQUENCE layer's segments in order, none for an EMPTY or a PARTICLES layer, whose
	 * region list is null.
	 */
	public static List<TextureRegion> regionsOf(ParallaxLayer layer)
	{return layer.drawsImage() ? layer.getTexRegion() : Collections.emptyList();}

	/** The image a layer draws, null for an EMPTY or a PARTICLES layer. */
	public static TextureRegion imageOf(ParallaxLayer layer)
	{return layer.drawsImage() ? layer.getTexRegion().get(0) : null;}

	/** Puts {@code replacement} in {@code layer}'s place in the stack and in the image links, and selects it. */
	public static void replaceLayer(ParallaxLayer layer, ParallaxLayer replacement)
	{
		List<ParallaxLayer> layers = parallax_Heart.parallaxReader.layers;
		int index = layers.indexOf(layer);
		if (index < 0)
			return;

		layers.set(index, replacement);
		removeFromLinks(layer);
		addToLinks(replacement);
		currentlySelectedParallax = replacement;
	}
}

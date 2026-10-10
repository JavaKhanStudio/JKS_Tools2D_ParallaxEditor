package jks.tools2d.parallax.editor.gvars;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton.ImageTextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.LinkLabel.LinkLabelStyle;
import com.kotcrab.vis.ui.widget.MenuItem.MenuItemStyle;
import com.kotcrab.vis.ui.widget.Tooltip.TooltipStyle;
import com.kotcrab.vis.ui.widget.VisCheckBox.VisCheckBoxStyle;
import com.kotcrab.vis.ui.widget.VisImageTextButton.VisImageTextButtonStyle;
import com.kotcrab.vis.ui.widget.VisTextButton.VisTextButtonStyle;
import com.kotcrab.vis.ui.widget.VisTextField.VisTextFieldStyle;
import com.kotcrab.vis.ui.widget.spinner.Spinner.SpinnerStyle;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane.TabbedPaneStyle;

public final class GVars_UI
{
	public static Skin baseSkin;
	public static Stage mainUi;

	/** Label style of the headings in the option tabs, uses {@link #areaTextFont}. */
	public static LabelStyle labelStyle_OptionsTitle;
	/** Plain copy of the default label style, for labels whose style gets tweaked. */
	public static LabelStyle labelStyle_Second;

	/**
	 * The editor's one text font, sized after the window width ({@link #fontSize}): every skin style that used the
	 * skin's fixed 15 px font (labels, buttons, check boxes, select boxes, text fields, tabs) uses it (r67).
	 */
	public static BitmapFont areaTextFont;

	/** The style types of the skin holding a font; {@link #resize} swaps the font in each. */
	private static final Class<?>[] FONT_STYLES = { LabelStyle.class, TextButtonStyle.class, VisTextButtonStyle.class, CheckBoxStyle.class,
			VisCheckBoxStyle.class, SelectBoxStyle.class, ListStyle.class, WindowStyle.class, TextFieldStyle.class, VisTextFieldStyle.class,
			ImageTextButtonStyle.class, VisImageTextButtonStyle.class, TooltipStyle.class, LinkLabelStyle.class, MenuItemStyle.class,
			SpinnerStyle.class, TabbedPaneStyle.class };

	/** The size {@link #areaTextFont} was made at. */
	private static int fontBuiltSize;

	/** The skin's own font, which the styles held before the first {@link #resize}. */
	private static BitmapFont skinFont;

	private static FreeTypeFontGenerator generator;

	private GVars_UI()
	{}

	public static void init()
	{
		mainUi = new Stage(new ScreenViewport());
		baseSkin = new Skin(Gdx.files.internal("skins/uis/uiskin.json"));
		if (!VisUI.isLoaded())
		{
			// VisUI 1.5.9 targets libGDX 1.14.1; 1.14.2 only reverted return types VisUI never calls (checked in its bytecode).
			VisUI.setSkipGdxVersionCheck(true);
			VisUI.load(baseSkin);
		}
		// The editor was laid out around compact VisUI widgets (the 2019 version did this from inside a copied color picker).
		VisUI.getSizes().scaleFactor = 0.6f;

		skinFont = baseSkin.getFont("default-font");
		labelStyle_Second = new LabelStyle(baseSkin.get("default", LabelStyle.class));
		labelStyle_OptionsTitle = new LabelStyle(baseSkin.get("default", LabelStyle.class));
		generator = new FreeTypeFontGenerator(Gdx.files.internal("ui/fonts/OpenSansRegular.ttf"));
		resize();

		Gdx.input.setInputProcessor(mainUi);
	}

	/** Regenerates the size-dependent font after the window size changed. */
	public static void resize()
	{
		BitmapFont previous = areaTextFont;

		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = fontBuiltSize = fontSize();
		parameter.color = Color.WHITE;
		parameter.minFilter = TextureFilter.Linear;
		parameter.magFilter = TextureFilter.Linear;
		areaTextFont = generator.generateFont(parameter);

		Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		for (Class<?> type : FONT_STYLES)
		{
			ObjectMap<String, ?> styles = baseSkin.getAll(type);
			if (styles != null)
				for (Object style : styles.values())
					swapFont(style, previous == null ? skinFont : previous, seen);
		}
		swapFont(labelStyle_Second, previous == null ? skinFont : previous, seen);
		labelStyle_OptionsTitle.font = areaTextFont;

		if (previous != null)
			previous.dispose();
	}

	/**
	 * Regenerates the font if the window changed size since it was made: a screen built after a resize elsewhere.
	 * Returns true when it did: every label and button built before draws the disposed font, so rebuild them.
	 */
	public static boolean fitWindow()
	{
		if (fontBuiltSize == fontSize())
			return false;
		resize();
		return true;
	}

	/**
	 * The font size for a window this wide: 16 px at the 1300 px default, more on a larger screen. The four main tabs
	 * take about 21 times it in width, which the left panel always holds (GVars_Vue_Edition.buildSizes).
	 */
	public static int fontSize()
	{return Math.max(15, Gdx.graphics.getWidth() / 80);}

	/** Points every font field of {@code style} that holds {@code from}, and of the styles it holds, to the new font. */
	private static void swapFont(Object style, BitmapFont from, Set<Object> seen)
	{
		if (style == null || !seen.add(style))
			return;
		for (Field field : style.getClass().getFields())
		{
			try
			{
				Object value = field.get(style);
				if (value == from && field.getType() == BitmapFont.class)
					field.set(style, areaTextFont);
				else if (value != null && value.getClass().getSimpleName().endsWith("Style"))
					swapFont(value, from, seen);
			}
			catch (IllegalAccessException e)
			{
				throw new IllegalStateException(e);
			}
		}
	}

	/** Starts a new, empty stage for the next view. */
	public static void reset()
	{
		mainUi.dispose();
		mainUi = new Stage(new ScreenViewport());
		Gdx.input.setInputProcessor(mainUi);
	}

	public static void dispose()
	{
		mainUi.dispose();
		if (areaTextFont != null)
			areaTextFont.dispose();
		generator.dispose();
		VisUI.dispose(); // also disposes baseSkin
	}
}

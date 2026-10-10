package jks.tools2d.parallax.editor.vue.edition;

import static jks.tools2d.parallax.editor.gvars.GVars_UI.baseSkin;
import static jks.tools2d.parallax.editor.gvars.GVars_Vue_Edition.getDefaults;
import static jks.tools2d.parallax.editor.gvars.GVars_Vue_Edition.size_Bloc_Selection_Parallax_Width;
import static jks.tools2d.parallax.editor.vue.Vue_Edition.parallax_Heart;
import static jks.tools2d.libgdxutils.Utils_Interface.onChange;

import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.kotcrab.vis.ui.util.dialog.Dialogs;
import com.kotcrab.vis.ui.util.dialog.Dialogs.OptionDialogType;
import com.kotcrab.vis.ui.util.dialog.OptionDialogAdapter;
import com.kotcrab.vis.ui.widget.Tooltip;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;

import jks.tools2d.libgdxutils.JksForm;
import jks.tools2d.libgdxutils.JksTextureList;
import jks.tools2d.libgdxutils.Utils_Interface;
import jks.tools2d.parallax.ParallaxLayer;
import jks.tools2d.parallax.editor.gvars.GVars_UI;
import jks.tools2d.parallax.editor.gvars.GVars_Vue_Edition;
import jks.tools2d.parallax.editor.vue.edition.data.Position_Infos;
import jks.tools2d.parallax.editor.vue.edition.utils.Utils_LoadingImages;
import jks.tools2d.parallax.editor.vue.edition.utils.Utils_Texture;
import jks.tools2d.parallax.heart.Gvars_Parallax;

/**
 * List of the project images. The selected image gets buttons to add it as a layer, to make the layers using another
 * image use this one instead, or to remove it.
 */
public class VE_Tab_TextureList_Adding extends Tab
{
	public static JksTextureList imageList;

	private final Table mainTable = new Table();
	/** The selected image's buttons, on a line above the list (r68): add, swap and remove, or switch for and cancel. */
	private final Table buttons = new Table();
	private final VisLabel buttonsName = new VisLabel();

	private final ImageButton button_addData, button_changeData, button_removeData;
	private final ImageButton button_switchFor, button_cancel;

	/** Image whose layers will switch to the next selected image. */
	private TextureRegion changingRegion;

	VE_Tab_TextureList_Adding()
	{
		super(false, false);

		// A line's height, which grows with the font.
		float buttonSize = GVars_UI.fontSize() * 2.2f;
		button_addData = squareButton("editor/interfaces/button_add.png", buttonSize);
		button_changeData = squareButton("editor/interfaces/button_transform.png", buttonSize);
		button_removeData = squareButton("editor/interfaces/delete.png", buttonSize);
		button_switchFor = squareButton("editor/interfaces/button_transform.png", buttonSize);
		button_cancel = squareButton("editor/interfaces/delete.png", buttonSize);

		button_addData.setName("adding.add");
		button_changeData.setName("adding.change");
		button_removeData.setName("adding.remove");
		button_switchFor.setName("adding.switchFor");
		button_cancel.setName("adding.cancel");

		new Tooltip.Builder("Add it as a layer").target(button_addData).build();
		new Tooltip.Builder("Make the layers using another image use this one").target(button_changeData).build();
		new Tooltip.Builder("Remove it").target(button_removeData).build();
		new Tooltip.Builder("Their layers use this image instead").target(button_switchFor).build();
		new Tooltip.Builder("Cancel").target(button_cancel).build();

		onChange(button_addData, this::addSelectedAsLayer);
		onChange(button_changeData, () ->
		{
			changingRegion = imageList.getSelected();
			showSwitching(true);
		});
		onChange(button_removeData, this::askRemoveSelected);
		onChange(button_switchFor, () ->
		{
			Utils_Texture.changeTextureInPage(changingRegion, imageList.getSelected());
			showSwitching(false);
		});
		onChange(button_cancel, () -> showSwitching(false));

		imageList = buildImageList();
		imageList.setName("adding.imageList");
		GVars_Vue_Edition.setItems();

		ScrollPane scrollPane = new ScrollPane(imageList, baseSkin);
		scrollPane.setFadeScrollBars(false);

		JksForm actions = new JksForm();
		actions.pad(4, 8, 4, 8);
		buttons.left();
		actions.line(buttonsName, buttons);
		showSwitching(false);

		mainTable.top();
		mainTable.add(actions).growX().row();
		mainTable.add(scrollPane).grow();
	}

	private static ImageButton squareButton(String image, float size)
	{
		ImageButton button = Utils_Interface.buildSquareButton(image, size);
		button.setSize(size, size);
		return button;
	}

	private JksTextureList buildImageList()
	{
		return new JksTextureList(baseSkin, size_Bloc_Selection_Parallax_Width, size_Bloc_Selection_Parallax_Width / 2f)
		{
			@Override
			public void choiceAction(TextureRegion item)
			{update();}
		};
	}

	private void addSelectedAsLayer()
	{
		TextureRegion region = imageList.getSelected();
		if (region == null)
			return;

		ParallaxLayer layer = new ParallaxLayer(region, true, Gvars_Parallax.getWorldWidth(), .01f, .01f, 1);
		layer.setUseOriginalSize(parallax_Heart.currentPage.useOriginalSize);
		layer.setUpEverything(getDefaults().defaultModel);
		addItem(layer);
	}

	private void askRemoveSelected()
	{
		TextureRegion region = imageList.getSelected();
		Position_Infos position = region == null ? null : GVars_Vue_Edition.imageRef.get(region);
		if (position == null)
			return;

		String message = (position.fromAtlas
				? "Do you really want to delete this part of the atlas? You won't be able to add it back"
				: "Do you really want to delete this image and all its uses?")
				+ "\n YES: delete from the parallax AND the list"
				+ "\n NO: delete only from the parallax";

		Dialogs.showOptionDialog(GVars_UI.mainUi, "Delete image", message, OptionDialogType.YES_NO_CANCEL, new OptionDialogAdapter()
		{
			@Override
			public void yes()
			{
				Utils_LoadingImages.removeFile(region, true);
				imageList.clearSelected();
				update();
				GVars_Vue_Edition.refreshActiveTab();
			}

			@Override
			public void no()
			{
				Utils_LoadingImages.removeFile(region, false);
				GVars_Vue_Edition.refreshActiveTab();
			}
		});
	}

	/** Adds a layer in front of or behind the others depending on the defaults, then moves the defaults on. */
	public static void addItem(ParallaxLayer layer)
	{addItem(layer, getDefaults().addInFront ? parallax_Heart.parallaxReader.layers.size() : 0);}

	public static void addItem(ParallaxLayer layer, int position)
	{
		parallax_Heart.parallaxReader.layers.add(Math.min(position, parallax_Heart.parallaxReader.layers.size()), layer);

		if (getDefaults().increment)
			getDefaults().doIncrement(true);

		GVars_Vue_Edition.selectLayer(layer);
		GVars_Vue_Edition.addToLinks(layer);
	}

	public void update()
	{
		if (imageList.getItems().size == 0)
		{
			showSwitching(false);
			return;
		}
		enable(button_addData, imageList.getSelected() != null);
		enable(button_changeData, imageList.getSelected() != null);
		enable(button_removeData, imageList.getSelected() != null);
		enable(button_switchFor, imageList.getSelected() != null && imageList.getSelected() != changingRegion);
	}

	/** Disabled and faded: the line keeps its buttons where they are, nothing selected or not. */
	private static void enable(ImageButton button, boolean enabled)
	{
		button.setDisabled(!enabled);
		button.setTouchable(enabled ? Touchable.enabled : Touchable.disabled);
		button.getColor().a = enabled ? 1 : 0.35f;
	}

	/** The line holds add, swap and remove, or, while choosing the image to swap to, switch for and cancel. */
	private void showSwitching(boolean show)
	{
		if (!show)
			changingRegion = null;
		buttonsName.setText(show ? "Swap for" : "Selected");
		buttons.clearChildren();
		for (ImageButton button : show ? new ImageButton[] { button_switchFor, button_cancel } : new ImageButton[] { button_addData, button_changeData, button_removeData })
			buttons.add(button).padRight(6);
		update();
	}

	@Override
	public String getTabTitle()
	{return "Adding new";}

	@Override
	public Table getContentTable()
	{
		update();
		return mainTable;
	}
}
